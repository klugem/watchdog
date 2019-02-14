#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh

# check, if used tools are installed
USED_TOOLS='grep'
MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS" "check_shflag_tools")
CODE=$?

if [ $CODE -ne 0 ]; then
	echoError "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi

# define parameters
DEFINE_string 'options' '' '[optional] additional flags or parameters that are directly delivered to grep' ''
DEFINE_string 'pattern' '' 'pattern to search for' 'p'
DEFINE_string 'file' '' 'file to search in' 'f'
DEFINE_string 'outputFile' '' 'return of grep is written to that output file' 'o'
DEFINE_string 'returnFilePath' '' 'path to the return variables file' ''
DEFINE_boolean 'debug' 'false' '[optional] prints out debug messages.' ''

# parse parameters
FLAGS "$@" || exit $EXIT_INVALID_ARGUMENTS
eval set -- "${FLAGS_ARGV}"
printParamValues "initial parameters" # print param values, if in debug mode

# check if mandatory arguments are there
if [ -z "$FLAGS_pattern" ]; then
	echoError "Parameter -p must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_file" ]; then
	echoError "Parameter -f must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_outputFile" ]; then
	echoError "Parameter -o must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi

printParamValues "parameters before actual script starts" # print param values, if in debug mode
##################################################### START with actual SCRIPT ##################################################### 

# check, if the input files exist
verifyFileExistence "$FLAGS_file"
OUT=$(createOutputFolder "$FLAGS_outputFile")

# run it
MESSAGE=$(grep $FLAGS_options "$FLAGS_pattern" "$FLAGS_file" > "$FLAGS_outputFile")
RET=$?

# check exit code
if [ $RET -eq 0 ] || [ $RET -eq 1 ]; then
	# write return value, if variable is set
	if [ ! -z "$FLAGS_returnFilePath" ]; then
		writeParam2File "$FLAGS_returnFilePath" "grepResultFile" "$FLAGS_outputFile"
		blockUntilFileIsWritten "$FLAGS_returnFilePath"
	fi
	exit $EXIT_OK
else
	echoError "grep run failed. Output file was deleted. See error of grep below"
	echoAError "error code: '$RET'"
	 # output the original message
	printf "$MESSAGE\n"
	rm -f "$FLAGS_output" 2>&1 > /dev/null
	exit $EXIT_FAILED
fi

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< #

# exit with exit status 0 or exit code should be there earlier
echoError "Reached end of script! Exit should be performed earlier..."
exit $EXIT_REACHED_END_OF_SCRIPT
