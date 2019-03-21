#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh $@

# check, if used tools are installed
USED_TOOLS='cat:sed:touch:rm:ls'
MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS" "check_shflag_tools")
CODE=$?

if [ $CODE -ne 0 ]; then
	echoAError "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi

# define parameters

DEFINE_string 'input' '' 'multiple input files (or input folders) in the order in which they should be joined; separated by ','; in pattern mode (--pattern) folder path(s) are expected' 'i'
DEFINE_string 'output' '' 'path to output file' 'o'
DEFINE_boolean 'convertPairedEnd' '1' 'special flag for joining of FASTQ files; adds /1 and /2 at the end of read names if casava format 1.8 or greater is used; default: disabled' 'c'
DEFINE_string 'pattern' '' 'one ore more unix file pattern (e.g. *.txt) that are used to find files matching that pattern; multiple patterns can be used separated by ','; one pattern corresponds to one input folder path; order of files to join can not be influenced' 'p'
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
if [ -z "$FLAGS_output" ]; then
	echoError "Parameter -o must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi

printParamValues "parameters before actual script starts" # print param values, if in debug mode
##################################################### START with actual SCRIPT ##################################################### 

# split the stuff
IFS=',' read -ra IN <<< "$FLAGS_input"
COUNT=0
# if in pattern mode --> get the files which match the pattern
if [ "$FLAGS_pattern" != "" ]; then
	# check, if one or multiple pattern are given
	IFS=',' read -ra PATTERNS <<< "$FLAGS_pattern"

	if [ "${#PATTERNS[@]}" -eq 1 ]; then
		SINGLE_PATTERN=1
	else
		if [ "${#PATTERNS[@]}" -ne "${#IN[@]}" ]; then
			echoError "Only one or the same number of patterns than input folders must be given!"
			exit $EXIT_MISFORMATED_INPUT
		fi
		SINGLE_PATTERN=0
	fi
	# process all folders that are in the IN list
	for I in "${!IN[@]}"; do 
		P="${IN[$I]}"

		# verify that folder is there
		verifyFolderExistence "$P"
		cd "$P"
		if [ "$SINGLE_PATTERN" -eq 1 ]; then
			PAT="${PATTERNS[0]}"
		else
			PAT="${PATTERNS[$I]}"
		fi
		# get all files which match that pattern

		cd "$P"
		RET=$(ls -p $PAT 2> /dev/null)
		if [ $? -eq 0 ]; then
			for FILE in $(ls -p $PAT); do 
				FILE="$P/$FILE"
				verifyFileExistence "$FILE" # test if file is there and readable
				FILES[$COUNT]="$FILE"
				COUNT=$((COUNT+1))
			done
		else
			echoError "Pattern '$PAT' in '$P' did not match any files."
			exit $EXIT_MISFORMATED_INPUT
		fi
	done
else
	for I in "${!IN[@]}"; do 
		FILE="${IN[$I]}"
		# test if file is there and readable
		verifyFileExistence "$FILE" # test if file is there and readable
		FILES[$COUNT]=$FILE
		COUNT=$((COUNT+1))
	done
fi

# check if more than one file was entered
if [ "$COUNT" -le 1 ]; then
	echoError "Only $COUNT file was given as input but at least 2 files are required to join them.";
	exit $EXIT_INVALID_ARGUMENTS
fi

# create output folder
OUT_BASE_FOLDER=$(createOutputFolder "$FLAGS_output")

rm -f "$FLAGS_output" 2>&1 > /dev/null
touch "$FLAGS_output"
for I in "${!FILES[@]}"; do 
	FILE="${FILES[$I]}"
	echo "merging file '$FILE'..."
	MESSAGE=$(cat "$FILE" >> "$FLAGS_output")
	CODE=$?

	if [ $CODE -ne 0 ]; then
		echoError "Concatination of '${FILES[*]}' failed. Output file was deleted. See error of cat below:"
		echoAError "$MESSAGE, error code: '$CODE'"
		rm -f $FLAGS_output 2>&1 > /dev/null
		exit $EXIT_FAILED
	fi
done

# check, if fastq and paired and
if [ $FLAGS_convertPairedEnd -eq 0 ]; then
	sedinline -E 's/^(.+(\/[12])?)\s+(([12]):[NY]:[0-9]+:[ATCG]{6})$/\1\/\4 \3/' "$FLAGS_output"
fi

writeParam2File "$FLAGS_returnFilePath" "joinedFile" "$FLAGS_output"
blockUntilFileIsWritten "$FLAGS_returnFilePath"
exit $EXIT_OK

# exit with exit status 0 or exit code shoul be there earlier
echoError "Reached end of script! Exit should be performed earlier..."
exit $EXIT_REACHED_END_OF_SCRIPT
