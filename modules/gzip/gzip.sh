#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh $@

# check, if used tools are installed
USED_TOOLS='gzip:echo:grep:wc:rm:whoami'
MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS" "check_shflag_tools")
CODE=$?

if [ $CODE -ne 0 ]; then
	echoError "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi

# define parameters
DEFINE_string 'input' '' 'path to input file' 'i'
DEFINE_boolean 'decompress' 'false' '[optional] decompress the input file' 'u'
DEFINE_string 'output' '${input}.gz' '[optional] path to output file' 'o'
DEFINE_string 'md5' '' '[optional] path to md5 file to store a md5 hashsum of the file to compress or to verify that the file was extracted correctly' 'm'
DEFINE_string 'oldPathMd5' '' '[optional] path where the files was stored when the md5 checksum was created' ''
DEFINE_boolean 'verify' 'true' '[optional] verify after compression the integrity of the file' 'v'
DEFINE_boolean 'delete' 'false' '[optional] delete the file after compression was performed; enforces integrity check' 'd'
DEFINE_integer 'quality' '9' '[optional] compression quality ranging from [1:9] with 1 being the fastest and 9 being the slowest but smallest.' 'q'
DEFINE_integer 'limitLines' '' '[optional] extract only the first N lines.' 'l'
DEFINE_string 'returnFilePath' '' 'path to the return variables file' ''
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

# set output name, if none is set
if [ "$FLAGS_output" == '${input}.gz' ]; then
	if [ "$FLAGS_decompress" -eq 0 ]; then
		echoError "Parameter -o must be set in case of decompress mode. (see --help for details)";
		exit $EXIT_MISSING_ARGUMENTS
	fi
	FLAGS_output="$FLAGS_input.gz"
fi

# enforce verify when delete is enabled
if [ "$FLAGS_delete" -eq 0 ]; then
	FLAGS_verify=0
fi

# disable verify when line limit is applied
if [ ! -z "$FLAGS_limitLines" ] && [ "$FLAGS_limitLines" -ge 1 ]; then
	FLAGS_verify=1
fi

# verify quality
if [ "$FLAGS_quality" -le 0 ] || [ "$FLAGS_quality" -ge 10 ] ; then
	FLAGS_quality=9
	echoWarn "Quality value must be between [1:9]; User input was overwritten with $FLAGS_quality!"
fi
printParamValues "parameters before actual script starts" # print param values, if in debug mode
##################################################### START with actual SCRIPT ##################################################### 

# test if file is there and readable
verifyFileExistence "$FLAGS_input"

# create output folder
createOutputFolder "$FLAGS_output"

# compression mode
if [ "$FLAGS_decompress" -eq 1 ]; then
	# compress the file
	MESSAGE=$(gzip -$FLAGS_quality --stdout "$FLAGS_input" > "$FLAGS_output")
	CODE=$?

	if [ $CODE -ne 0 ]; then
		echoError "Compression of '$FLAGS_input' failed. File was deleted. See error of gzip below"
		echoAError "$MESSAGE, error code: '$CODE'"
		rm -f $FLAGS_output 2>&1 > /dev/null
		exit $EXIT_FAILED
	else
		if [ -f "$FLAGS_output" ]; then
			if [ "$FLAGS_verify" -eq 0 ]; then
				# perform integrity check
				TEST=$(gzip --test -v "$FLAGS_output" 2>&1 | grep -E "^$FLAGS_output:\s+OK$" | wc -l)
				if [ "$TEST" -eq 1 ]; then
					if [ "$FLAGS_delete" -eq 0 ]; then
						rm -f "$FLAGS_input" 2>&1 > /dev/null
					fi
					writeParam2File "$FLAGS_returnFilePath" "processedGzipFile" "$FLAGS_input"
					writeParam2File "$FLAGS_returnFilePath" "createdGzipFile" "$FLAGS_output"
					blockUntilFileIsWritten "$FLAGS_returnFilePath"
					exit $EXIT_OK
				else
					rm -f $FLAGS_output 2>&1 > /dev/null
					echoError "Integrity check of '$FLAGS_output' failed. File was deleted."
					exit $EXIT_FAILED
				fi
			else
				writeParam2File "$FLAGS_returnFilePath" "processedGzipFile" "$FLAGS_input"
				writeParam2File "$FLAGS_returnFilePath" "createdGzipFile" "$FLAGS_output"
				blockUntilFileIsWritten "$FLAGS_returnFilePath"
				exit $EXIT_OK
			fi	
		else
			echoError "Compression of '$FLAGS_input' failed with unknown reason! File was deleted."
			rm -f "$FLAGS_output" 2>&1 > /dev/null
			exit $EXIT_FAILED
		fi
	fi
# decompress mode
else 
	# get only first n lines
	if [ ! -z "$FLAGS_limitLines" ] && [ "$FLAGS_limitLines" -ge 1 ]; then
		MESSAGE=$(gzip -cd "$FLAGS_input" | head -n $FLAGS_limitLines > "$FLAGS_output")
	else
		MESSAGE=$(gzip -cd "$FLAGS_input" > "$FLAGS_output")
	fi
	CODE=$?

	if [ $CODE -ne 0 ]; then
		echoError "Decompression of '$FLAGS_input' failed. File was deleted. See error of gzip below:"
		echoAError "$MESSAGE, error code: '$CODE'"
		rm -f $FLAGS_output 2>&1 > /dev/null
		exit $EXIT_FAILED
	else
		# verify the decompression with a given md5 checksum file
		if [ ! -z "$FLAGS_md5" ];  then
			if [ "$FLAGS_oldPathMd5" != "" ]; then
				OLD="--oldChecksumName \"$FLAGS_oldPathMd5\""
			fi
			MESSAGE=$($SCRIPT_FOLDER/../../modules/checksum/checksum.sh -i $FLAGS_output -c $FLAGS_md5 -v $OLD)
			CODE=$?

			if [ $CODE -ne 0 ]; then
				echoAError "$MESSAGE, error code: '$CODE'"
				echoAError "Output file '$FLAGS_output' was deleted."
				rm -f $FLAGS_output 2>&1 > /dev/null
				exit $EXIT_FAILED
			fi
		fi
		
		# check, if input file should be deleted
		if [ "$FLAGS_delete" -eq 0 ]; then
			rm -f "$FLAGS_input" 2>&1 > /dev/null
		fi
		writeParam2File "$FLAGS_returnFilePath" "processedGzipFile" "$FLAGS_input"
		writeParam2File "$FLAGS_returnFilePath" "createdGzipFile" "$FLAGS_output"
		blockUntilFileIsWritten "$FLAGS_returnFilePath"
		exit $EXIT_OK
	fi
fi

# exit with exit status 0 or exit code should be there earlier
exit $EXIT_REACHED_END_OF_SCRIPT
