#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh $@

# check, if used tools are installed
USED_TOOLS='samtools:ln:rm:printf:echo:wc:mv'
MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS" "check_shflag_tools")
CODE=$?

if [ $CODE -ne 0 ]; then
	echoError "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi

# define parameters
DEFINE_string 'bam' '' 'path to the bam file' 'b'
DEFINE_boolean 'link' '0' '[optional] creates a link called NAME.bam.bai because some tool expect the index under that name; use --nolink to disable it' ''
DEFINE_string 'returnFilePath' '' 'path to the return variables file' ''
DEFINE_boolean 'debug' 'false' '[optional] prints out debug messages.' ''

# parse parameters
FLAGS "$@" || exit $EXIT_INVALID_ARGUMENTS
eval set -- "${FLAGS_ARGV}"
printParamValues "initial parameters" # print param values, if in debug mode

# check if mandatory arguments are there
if [ -z "$FLAGS_bam" ]; then
	echoError "Parameter -b must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi

printParamValues "parameters before actual script starts" # print param values, if in debug mode
##################################################### START with actual SCRIPT ##################################################### 

# check, if the input files exist
verifyFileExistence "$FLAGS_bam"
FILE_BAI="${FLAGS_bam%.*}.bai"

# verify that tool can write there
OUT_BASE=$(createOutputFolder "$FLAGS_bam")
if [ ! -w "$OUT_BASE" ]; then
	echoError "No write permissions in folder '$OUT_BASE'.";
	exit $EXIT_WRITING_FAILED
fi

# run it
MESSAGE=$(samtools index -b "$FLAGS_bam" 2>&1)
RET=$?

# check exit code
FAIL=0
if [ $RET -eq 0 ]; then
	# rename the stuff 
	mv "$FLAGS_bam.bai" "$FILE_BAI" 2>&1 > /dev/null 
	# check if bam file is there and contains some mappings
	if [ -f "$FILE_BAI" ]; then
		COUNT=$(head -c 512 "$FILE_BAI" | wc -c)
		if [ $COUNT -eq 0 ]; then
			FAIL=1
			echoError "Index file '$FILE_BAI' is empty!"
		fi
	else
		echoError "Index file '$FILE_BAI' was not found."
		FAIL=1
	fi
else
	FAIL=1
fi

# output the error
if [ $FAIL -eq 1 ]; then
	echoError "samtools run failed. Index file was deleted. See error of samtools below"
	echoAError "error code: '$RET'"
	 # output the original message
	printf "$MESSAGE\n"
	rm -f "$FILE_BAI" 2>&1 > /dev/null
	exit $EXIT_FAILED
else
	# create the link
	if [ $FLAGS_link -eq 0 ]; then
		FILE_BAI_NAME=$(basename "$FILE_BAI")
		BAM_NAME=$(basename "$FLAGS_bam")
		$(cd "$OUT_BASE" && ln -s "${FILE_BAI_NAME}" "${BAM_NAME}.bai")
	fi
	writeParam2File "$FLAGS_returnFilePath" "BAMFile" "$FLAGS_bam"
	blockUntilFileIsWritten "$FLAGS_returnFilePath"

	# output the original message
	printf "$MESSAGE\n"
	exit $EXIT_OK
fi


# exit with exit status 0 or exit code should be there earlier
echoError "Reached end of script! Exit should be performed earlier..."
exit $EXIT_REACHED_END_OF_SCRIPT
