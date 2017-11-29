#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh $@

# check, if used tools are installed
USED_TOOLS='md5sum:dirname:basename:cut:echo:mv:rm:head:sleep'
MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS" "check_shflag_tools")
CODE=$?

if [ $CODE -ne 0 ]; then
	echoError "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi

LOCK_TIMEOUT=180 # wait 3 minutes if lockfile is removed.

# define parameters
DEFINE_string 'input' '' 'file for which a checksum should be calculated or which should be verified' 'i'
DEFINE_string 'checksum' '.checksum.md5' '[optional] path to the checksum file; by default in the same dir as the input file is' 'c'
DEFINE_boolean 'verify' '1' '[optional] set flag to verify a file based on the checksum stored in the checksum file' 'v'
DEFINE_boolean 'update' '1' '[optional] override existing checksum for that file in the checksum file' 'u'
DEFINE_boolean 'absolutePath' '1' '[optional] this option can be set to store absolute pathes in the checksum file' 'a'
DEFINE_boolean 'ignorePath' '1' '[optional] this option can be set to use only the name of the file for identification of the checksum line; (can only be used in verify mode)' ''
DEFINE_string 'oldChecksumName' '' '[optional] uses that name to identify the correct checksum because the file was renamed after the checksum was created; (can only be used in verify mode)' ''
DEFINE_boolean 'debug' 'false' '[optional] prints out debug messages.' ''

# parse parameters
FLAGS "$@" || exit $EXIT_INVALID_ARGUMENTS
eval set -- "${FLAGS_ARGV}"
printParamValues "initial parameters" # print param values, if in debug mode

# check if mandatory arguments are there
if [ -z "$FLAGS_input" ]; then
	echoError "Parameter -i must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi

printParamValues "parameters before actual script starts" # print param values, if in debug mode
##################################################### START with actual SCRIPT ##################################################### 

# get an absolute path, if the flag is set
if [ $FLAGS_absolutePath -eq 0 ]; then
	FLAGS_input=$(abspath $FLAGS_input)
fi

# test if file is there and readable
verifyFileExistence "$FLAGS_input"
BASE=$(dirname $FLAGS_input)

# check,if path to file is set and if not, get a absolute one
if [ "$FLAGS_checksum" == '.checksum.md5' ]; then
	FLAGS_checksum="$BASE/.checksum.md5"
fi

# check, if checksum is there if in verify mode
if [ $FLAGS_verify -eq 0 ]; then
	verifyFileExistence "$FLAGS_checksum"
	CHECK_SUM_NAME=${FLAGS_input}

	# check, if a old name for that file is set
	if [ "$FLAGS_oldChecksumName" != "" ]; then
		CHECK_SUM_NAME=$FLAGS_oldChecksumName
	fi

	# check, if the path of the file should be ignored 
	if [ $FLAGS_ignorePath -eq 0 ]; then
		CHECK_SUM_NAME=$(basename $CHECK_SUM_NAME)
		ADD=".*"
	fi

	GREP_PATTERN="\w+[[:space:]]+${ADD}${CHECK_SUM_NAME}\$"
else 
	# disable not valid parameters
	FLAGS_ignorePath=1
	FLAGS_oldChecksumName=""
	GREP_PATTERN="\w+[[:space:]]+${FLAGS_input}\$"

	wait4Lock "$FLAGS_checksum" $LOCK_TIMEOUT
	lockFile "$FLAGS_checksum"
	# check, if checksum file is there
	if [ -f $FLAGS_checksum ]; then
		# ensure that there is no checksum if not in update mode
		if [ $FLAGS_update -eq 1 ]; then
			COUNT=$(grep -c -E "$GREP_PATTERN" "$FLAGS_checksum")

			if [ "$COUNT" -ne "0" ]; then
				echoError "There is already a checksum stored for '$FLAGS_input' in '$FLAGS_checksum'. If you want to update that checksum use the update flag (see --help for details)";
				unlockFile "$FLAGS_checksum"
				exit $EXIT_INVALID_ARGUMENTS
			fi
		# delete the old checksum
		else 
			TMP_FILE=$(getTmpFile checksum)
			
			MESSAGE=$(grep -v -E "$GREP_PATTERN" "$FLAGS_checksum" > "$TMP_FILE")
			CODE=$?
			if [ $CODE -ne 0 ] && [ $CODE -ne 1 ]; then
				echoError "Removal of checksum for '$FLAGS_input' failed. See error of grep below"
				echoAError "$MESSAGE, error code: '$CODE'"
				unlockFile "$FLAGS_checksum"
				exit $EXIT_FAILED
			else
				mv $TMP_FILE ${FLAGS_checksum}
			fi
		fi
	# try to create checksum file
	else
		createOutputFile $FLAGS_checksum
	fi
	unlockFile "$FLAGS_checksum"
fi

# create a new checksum
if [ $FLAGS_verify -eq 1 ]; then
	# create a checksum
	MESSAGE=$(md5sum "$FLAGS_input")
	CODE=$?

	if [ $CODE -ne 0 ]; then
		echoError "Calculation of md5sum for '$FLAGS_input' failed. See error of md5sum below"
		echoAError "$MESSAGE, error code: '$CODE'"
		exit $EXIT_FAILED
	else
		wait4Lock "$FLAGS_checksum" $LOCK_TIMEOUT
		lockFile "$FLAGS_checksum"
		
		MESSAGE=$( { echo -e "$MESSAGE" >> "$FLAGS_checksum"; } 2>&1)
		CODE=$?

		unlockFile "$FLAGS_checksum"

		# test if the echo command was successful
		if [ $CODE -ne 0 ]; then
			echoError "Writing the md5sum to disk failed. See error below:"
			echoAError "$MESSAGE, error code: '$CODE'"
			exit $EXIT_FAILED
		else
			exit $EXIT_OK
		fi
	fi
# verify the file using the checksum
else
	# test if checksum for that file is there
	TMP_FILE=$(getTmpFile checksum)
	wait4Lock "$FLAGS_checksum" $LOCK_TIMEOUT
	lockFile "$FLAGS_checksum"
	MESSAGE=$( { grep -E "$GREP_PATTERN" "$FLAGS_checksum" > "$TMP_FILE"; } 2>&1)
	CODE=$?
	unlockFile "$FLAGS_checksum"

	if [ $CODE -ne 0 ]; then
		echoError "Checksum for '$CHECK_SUM_NAME' is not stored in '$FLAGS_checksum'."
		echoAError "$MESSAGE, error code: '$CODE'"
		exit $EXIT_INVALID_ARGUMENTS
	fi

	X1=$(md5sum "$FLAGS_input" | cut -d " " -f 1)
	X2=$(cut -d " " -f 1 "$TMP_FILE")

	# verify that the checksum is valid 
	MESSAGE=$(diff <(echo $X1) <(echo $X2) 2> /dev/null) 
	CODE=$?
	rm -f "$TMP_FILE"
	if [ $CODE -ne 0 ]; then
		echoError "Verification of md5sum for '$FLAGS_input' failed. See error of md5sum below"
		echoAError "$MESSAGE"
		exit $EXIT_FAILED
	else
		echoInfo "Checksum was valid for '$FLAGS_input'."
		exit $EXIT_OK
	fi
fi

# exit with exit status 0 or exit code shoul be there earlier
echoError "Reached end of script! Exit should be performed earlier..."
exit $EXIT_REACHED_END_OF_SCRIPT
