#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh $@

# check, if used tools are installed
USED_TOOLS='fastqc:rm:tee:echo'
MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS" "check_shflag_tools")
CODE=$?

if [ $CODE -ne 0 ]; then
	echoError "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi

# define parameters
DEFINE_string 'fastq' '' 'path to fastq file which should be analyzed' 'f'
DEFINE_string 'outdir' '' 'path to output folder' 'o'
DEFINE_integer 'threads' '1' 'number of threads to use; each will consume 256 megabyte of memory' 't'
DEFINE_string 'contaminants' '' '[optional] path to a file with non-default contaminants for screen against overrepresented sequences; format: name[tab]sequence' 'c'
DEFINE_string 'adapters' '' '[optional] path to a file with non-default adapters for screen against the library; format: name[tab]sequence' 'a'
DEFINE_string 'limits' '' '[optional] path to a file containing non-default limits for warnings/errors; must be in the same format as limits.txt is' 'l'
DEFINE_boolean 'debug' 'false' '[optional] prints out debug messages.' ''

# parse parameters
FLAGS "$@" || exit $EXIT_INVALID_ARGUMENTS
eval set -- "${FLAGS_ARGV}"
printParamValues "initial parameters" # print param values, if in debug mode

# check if mandatory arguments are there
if [ -z "$FLAGS_fastq" ]; then
	echoError "Parameter -f must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_outdir" ]; then
	echoError "Parameter -o must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ "$FLAGS_threads" -gt 128 ] || [ "$FLAGS_threads" -lt 1 ]; then
	echoError "Parameter -t must be between [1, 128]. (see --help for details)";
	exit $EXIT_INVALID_ARGUMENTS
fi

printParamValues "parameters before actual script starts" # print param values, if in debug mode
##################################################### START with actual SCRIPT ##################################################### 

BASENAME=$(basename "${FLAGS_fastq}")
# adapted from fastqc 
BASENAME=${BASENAME%.gz}
BASENAME=${BASENAME%.bz2}
BASENAME=${BASENAME%.txt}
BASENAME=${BASENAME%.fastq}
BASENAME=${BASENAME%.fq}
BASENAME=${BASENAME%.csfastq}
BASENAME=${BASENAME%.sam}
BASENAME=${BASENAME%.bam}

# check, if the input files exist
verifyFileExistence "$FLAGS_fastq"
if [ "$FLAGS_contaminants" != "" ]; then verifyFileExistence "$FLAGS_contaminants"; fi
if [ "$FLAGS_adapters" != "" ]; then verifyFileExistence "$FLAGS_adapters"; fi
if [ "$FLAGS_limits" != "" ]; then verifyFileExistence "$FLAGS_limits"; fi

# create output folder
OUT_BASE=$(createOutputFolder "$FLAGS_outdir/removeThisAndCreateParentFolder")

# check, if folder can be written
if [ ! -w "$OUT_BASE" ]; then
	echoError "No write permissions in folder '$OUT_BASE'.";
	exit $EXIT_WRITING_FAILED
fi

# build the command
COMMAND="fastqc --format 'fastq' --extract --outdir '$FLAGS_outdir' --threads $FLAGS_threads"
if [ "$FLAGS_contaminants" != "" ]; then COMMAND="$COMMAND --contaminants '$FLAGS_contaminants'"; fi
if [ "$FLAGS_adapters" != "" ]; then COMMAND="$COMMAND --adapters '$FLAGS_adapters'"; fi
if [ "$FLAGS_limits" != "" ]; then COMMAND="$COMMAND --limits '$FLAGS_limits'"; fi
COMMAND="$COMMAND '$FLAGS_fastq'"

# execute the command
MESSAGE=$(eval "$COMMAND" 2>&1 | tee "$FLAGS_outdir/$BASENAME.log")
RET=$?

if [ $CODE -ne 0 ]; then
	echoError "FastQC run with fastq file '$FLAGS_fastq' failed. Output was deleted. See error of fastQC below"
	echoAError "$MESSAGE, error code: '$CODE'"
	rm -f "$FLAGS_outdir/${BASENAME}_fastqc.html" 2>&1 > /dev/null
	rm -f "$FLAGS_outdir/${BASENAME}_fastqc.zip" 2>&1 > /dev/null
	exit $EXIT_FAILED
else
	# check for exception
	MESSAGE_ERROR=$($SCRIPT_FOLDER/../../core_lib/errorChecker.sh "$FLAGS_outdir/$BASENAME.log" 2>&1)
	CODE_ERROR=$?

	if [ $CODE_ERROR -ne 0 ]; then
		echoError "Error checker found some errors, see found errors below"
		echo -e "$MESSAGE_ERROR"
		rm -f "$FLAGS_outdir/${BASENAME}_fastqc.html" 2>&1 > /dev/null
		rm -f "$FLAGS_outdir/${BASENAME}_fastqc.zip" 2>&1 > /dev/null
		exit $EXIT_FAILED
	else
		# check, if the html file is there
		if [ -f "$FLAGS_outdir/${BASENAME}_fastqc.html" ] && [ -f "$FLAGS_outdir/${BASENAME}_fastqc.zip" ]; then
			# all looks ok
			exit $EXIT_OK
		else
			echoError "Expected output files '${BASENAME}_fastqc.html' or '${BASENAME}_fastqc.zip' in '$FLAGS_outdir/' are missing."
			rm -f "$FLAGS_outdir/${BASENAME}_fastqc.html" 2>&1 > /dev/null
			rm -f "$FLAGS_outdir/${BASENAME}_fastqc.zip" 2>&1 > /dev/null
			exit $EXIT_FAILED	
		fi
	fi
fi

# exit with exit status 0 or exit code should be there earlier
echoError "Reached end of script! Exit should be performed earlier..."
exit $EXIT_REACHED_END_OF_SCRIPT
