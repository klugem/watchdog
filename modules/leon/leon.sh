#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh

# check, if used tools are installed
USED_TOOLS='leon:tar:mv:cp:echo:tail:grep'
MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS" "check_shflag_tools")
CODE=$?

if [ $CODE -ne 0 ]; then
	echoError "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi

# define parameters
DEFINE_string 'input' '' 'path to input file: either .fastq, .fq or .leon.tar file' 'i'
DEFINE_string 'outputFolder' '' 'folder in which the result file is stored with new file ending' 'o'
DEFINE_integer 'threads' '1' '[optional] number of cores to use' 't'
DEFINE_string 'returnFilePath' '' 'path to the return variables file' ''
DEFINE_string 'workingDir' '/usr/local/storage/' 'path to working directory' ''
DEFINE_integer 'kmerSize' '31' '[optional] size of a kmer' 'k'
DEFINE_boolean 'debug' 'false' '[optional] prints out debug messages.' ''

# parse parameters
FLAGS "$@" || exit $EXIT_INVALID_ARGUMENTS
eval set -- "${FLAGS_ARGV}"
printParamValues "initial parameters" # print param values, if in debug mode
MODE=-1

# check if mandatory arguments are there
if [ -z "$FLAGS_input" ]; then
	echoError "Parameter -i must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
# check for correct file handling
if [[ "$FLAGS_input" == *.fastq ]] || [[ "$$FLAGS_input" == *.fq ]]; then
	MODE="-c"
fi
if [[ "$FLAGS_input" == *.leon.tar ]]; then
	MODE="-d"
fi
if [ "$MODE" == "-1" ]; then
	echoError "File ending of input file ($FLAGS_input) must be either .fq, .fastq or .leon.tar."
	exit
fi
if [ -z "$FLAGS_outputFolder" ]; then
	echoError "Parameter -o must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ "$FLAGS_threads" -gt 32 ] || [ "$FLAGS_threads" -lt 1 ]; then
	echoError "Parameter -t must be between [1, 32]. (see --help for details)";
	exit $EXIT_INVALID_ARGUMENTS
fi
if [ "$FLAGS_kmerSize" -gt 64 ] || [ "$FLAGS_kmerSize" -lt 16 ]; then
	echoError "Parameter -k must be between [16, 64]. (see --help for details)";
	exit $EXIT_INVALID_ARGUMENTS
fi

printParamValues "parameters before actual script starts" # print param values, if in debug mode
##################################################### START with actual SCRIPT ##################################################### 

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< #
# get base output name
FLAGS_workingDir=${FLAGS_workingDir%/}
B=$(basename "${FLAGS_input}")
BASENAME=${B%.*}

# get temporary folder and create link in order to ensure that two leon tasks do not infer with each other!
TMP=$(getTmpFile leon "$FLAGS_workingDir")
mkdir -p "$TMP"
deleteFolderOnExit "$TMP"
cd "$TMP"
ln -s "$FLAGS_input" "$TMP/$B"
FLAGS_input="$TMP/$B"

# check, if the input files exist
verifyFileExistence "$FLAGS_input"

# get output folder
INPUTBASE_FOLDER=$(dirname "${FLAGS_input}")
INPUTBASE="${INPUTBASE_FOLDER}/${BASENAME/.*/}"
OUTFILE="${FLAGS_outputFolder}/${BASENAME}"

# create output folder
OUT_FOLDER=$(createOutputFolder "$OUTFILE")

# untar it if decompress mode
if [ "$MODE" == "-d" ]; then
	executeCommand "tar xf $FLAGS_input" "/dev/null" "untar leon archive"
	# check if both files are there
	verifyFileExistence "${INPUTBASE_FOLDER}/compressedFastq.leon"
	verifyFileExistence "${INPUTBASE_FOLDER}/compressedFastq.fastq.qual"
	FLAGS_input="${INPUTBASE_FOLDER}/compressedFastq.leon"
fi

# run it
MESSAGE_L=$(leon $MODE -nb-cores $FLAGS_threads -kmer-size $FLAGS_kmerSize -lossless -file "${FLAGS_input}" 2>&1) #-verbose 0 
RET=$?

# check, if tool run to end.
COUNT=$(echo $MESSAGE_L | grep -c -e "Speed: .* mo/s leon$")
if [ $COUNT -ne 1 ]; then
	echoError "Leon was terminated before it finished."
	CODE=1
fi

# check exit code
if [ $RET -eq 0 ]; then
	# move the file to the correct location
	if [ "$MODE" == "-c" ]; then
		mv "${B}.leon" "compressedFastq.leon"
		mv "${B}.qual" "compressedFastq.fastq.qual"
		OUTFILE="$OUTFILE.leon.tar"
		executeCommand "tar --remove-files -cf $OUTFILE \"compressedFastq.leon\" \"compressedFastq.fastq.qual\"" "/dev/null" "tar leon archive"
	else
		OUTFILE="${OUTFILE%.leon}.fastq"
		mv "${FLAGS_input%.leon}.fastq.d" "${OUTFILE}"
	fi

	# output the original message
	echo "${MESSAGE_L}"
	if [ ! -z "$FLAGS_returnFilePath" ]; then
		writeParam2File "$FLAGS_returnFilePath" "createdFile" "$OUTFILE"
		blockUntilFileIsWritten "$FLAGS_returnFilePath"
	fi
	exit $EXIT_OK
else
	echoError "Leon run failed. See error of leon below"
	echoAError "error code: '$RET'"
	 # output the original message
	echo "${MESSAGE_L}"
	if [ "$MODE" == "-d" ]; then
		rm -f "compressedFastq.leon"
		rm -f "compressedFastq.fastq.qual"
	else
		rm -f "${FLAGS_input%.leon}.fastq.d"
	fi
	exit $EXIT_FAILED
fi

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< #

# exit with exit status 0 or exit code should be there earlier
echoError "Reached end of script! Exit should be performed earlier..."
exit $EXIT_REACHED_END_OF_SCRIPT
