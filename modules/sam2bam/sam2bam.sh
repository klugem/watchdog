#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh $@

# check, if used tools are installed
USED_TOOLS='samtools:touch:echo:printf:rm:grep:head:wc'
MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS" "check_shflag_tools")
CODE=$?

if [ $CODE -ne 0 ]; then
	echoError "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi

# define parameters
DEFINE_string 'sam' '' 'path to SAM file' 's'
DEFINE_string 'bam' '' 'path to ouput BAM file' 'b'
DEFINE_integer 'threads' '1' 'number of threads to use for compression' 't'
DEFINE_integer 'quality' '9' 'compression level; [1, 9] whereby 1 is the worst/fastest and 9 is the best/slowest compression' 'q'
DEFINE_string 'memory' '768M' 'maximal memory that can be used per thread; only an estimation and might be exceeded!' 'm'
DEFINE_string 'tmpFolder' '' 'write temporary files to that folder' 'T'
DEFINE_string 'returnFilePath' '' 'path to the return variables file' ''
DEFINE_boolean 'debug' 'false' '[optional] prints out debug messages.' ''

# parse parameters
FLAGS "$@" || exit $EXIT_INVALID_ARGUMENTS
eval set -- "${FLAGS_ARGV}"
printParamValues "initial parameters" # print param values, if in debug mode

# check if mandatory arguments are there
if [ -z "$FLAGS_sam" ]; then
	echoError "Parameter -s must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_bam" ]; then
	echoError "Parameter -b must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ "$FLAGS_threads" -gt 128 ] || [ "$FLAGS_threads" -lt 1 ]; then
	echoError "Parameter -t must be between [1, 128]. (see --help for details)";
	exit $EXIT_INVALID_ARGUMENTS
fi
if [ "$FLAGS_quality" -gt 9 ] || [ "$FLAGS_quality" -lt 1 ]; then
	echoError "Parameter -q must be between [1, 9]. (see --help for details)";
	exit $EXIT_INVALID_ARGUMENTS
fi
COUNT=$(echo "$FLAGS_memory" | grep -E -c "[0-9]+[KMG]")
if [ $COUNT -ne 1 ]; then
	echoError "Memory must match pattern '[0-9]+[KMG]'. (K: kilobyte, m: megabyte, g: gigabyte)"
	exit $EXIT_INVALID_ARGUMENTS
fi

printParamValues "parameters before actual script starts" # print param values, if in debug mode
##################################################### START with actual SCRIPT ##################################################### 

# check, if the input files exist
verifyFileExistence "$FLAGS_sam"

# create output folder
OUT_BASE=$(createOutputFolder "$FLAGS_bam")
if [ ! -z "$FLAGS_tmpFolder" ]; then
	TMP_PREFIX=$(getTmpFile sam2bam "$FLAGS_tmpFolder")
	TMP_FILE=$(getTmpFile sam2bam "$FLAGS_tmpFolder")
else
	TMP_PREFIX=$(getTmpFile sam2bam)
	TMP_FILE=$(getTmpFile sam2bam /tmp) 
fi

# run it
MESSAGE=$(samtools sort -@ $FLAGS_threads -m $FLAGS_memory -l $FLAGS_quality -O bam -T "${TMP_PREFIX}.part" "$FLAGS_sam" > "$FLAGS_bam")
RET=$?

# check for error
FAIL=0
touch "$TMP_FILE"
trap "rm -f \"$TMP_FILE\" 2>&1 > /dev/null" EXIT
printf "$MESSAGE" > "$TMP_FILE"
MESSAGE_ERROR=$($SCRIPT_FOLDER/../../core_lib/errorChecker.sh "$TMP_FILE" "truncated file:fail to open file" 2>&1)
CODE_ERROR=$?
rm -f "$TMP_FILE" 2>&1 > /dev/null

if [ $CODE_ERROR -ne 0 ]; then
	echoError "Error checker found some errors, see found errors below"
	echo -e "$MESSAGE_ERROR"
	exit $EXIT_FAILED
else
	# check if bam file is there and contains some mappings
	if [ -f "$FLAGS_bam" ]; then
		COUNT=$(head -c 10240 "$FLAGS_bam" | wc -c)
		if [ $COUNT -eq 0 ]; then
			FAIL=1
			echoError "BAM file '$FLAGS_bam' is empty!"
		fi
	else
		echoError "BAM file '$FLAGS_bam' was not found."
		FAIL=1
	fi

	# check exit code
	if [ $FAIL -eq 0 ] && [ $RET -eq 0 ]; then
		# output the original message
		printf "$MESSAGE\n"
		writeParam2File "$FLAGS_returnFilePath" "BAMFile" "$FLAGS_bam"
		blockUntilFileIsWritten "$FLAGS_returnFilePath"
		exit $EXIT_OK
	else
		FAIL=1
	fi

	# output the error
	if [ $FAIL -eq 1 ]; then
		echoError "samtools run failed. BAM file was deleted. See error of samtools below"
		echoAError "error code: '$RET'"
		 # output the original message
		printf "$MESSAGE\n"
		rm -f "$FLAGS_bam" 2>&1 > /dev/null
		exit $EXIT_FAILED
	fi
fi

# exit with exit status 0 or exit code should be there earlier
echoError "Reached end of script! Exit should be performed earlier..."
exit $EXIT_REACHED_END_OF_SCRIPT
