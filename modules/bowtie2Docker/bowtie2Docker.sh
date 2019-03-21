#!/bin/bash
### this is an example module that shows how a docker container can be used within Watchdog and does not contain all options and parameters of bowtie2 ###
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh $@

# check, if used tools are installed
USED_TOOLS='docker:basename:dirname:sed:tee:pgrep'
MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS" "check_shflag_tools")
CODE=$?

if [ $CODE -ne 0 ]; then
	echoError "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi

# info about container to use
CONTAINER_NAME="quay.io/biocontainers/bowtie2"
CONTAINER_TAG="2.3.4.1--py35pl5.22.0_0"
CONTAINER="${CONTAINER_NAME}:${CONTAINER_TAG}"
DOCKER_BASE_DIR="/host_data"

# define parameters
DEFINE_string 'genome' '' 'path to indexed reference genome (withouth trailing .X.bt2 ending)' 'g'
DEFINE_string 'reads' '' 'path to reads in fastq format; for paired end separated by ,' 'r'
DEFINE_string 'outfile' '' 'path to output file, which is written in SAM format; a log file with .log suffix will also be written' 's'
DEFINE_boolean 'debug' 'false' '[optional] prints out debug messages.' ''

# parse parameters
FLAGS "$@" || exit $EXIT_INVALID_ARGUMENTS
eval set -- "${FLAGS_ARGV}"
printParamValues "initial parameters" # print param values, if in debug mode

# check if mandatory arguments are there
if [ -z "$FLAGS_genome" ]; then
	echoError "Parameter -g must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_reads" ]; then
	echoError "Parameter -r must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_outfile" ]; then
	echoError "Parameter -s must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi

printParamValues "parameters before actual script starts" # print param values, if in debug mode
##################################################### START with actual SCRIPT ##################################################### 

# ensure that docker daemon is running
WCD=$(pgrep -f 'dockerd' | wc -l)
if [ $WCD -eq 0 ]; then
	echoError "It seems that the docker daemon 'dockerd' is not running!"
	exit 3
fi

# test if file is there and readable
verifyFileExistence "$FLAGS_genome.1.bt2"
FLAGS_genome=$(abspath "$FLAGS_genome.1.bt2" | sed 's/.1.bt2$//')

BASEDIR_INDEX=$(dirname "${FLAGS_genome}")
GENOME_INDEX_NAME=$(basename "${FLAGS_genome}")
DOCKER_INDEX="$DOCKER_BASE_DIR/index"
MOUNT_DOCKER_INDEX="-v '$BASEDIR_INDEX:$DOCKER_INDEX:ro'"

# test if read files are there
IFS=',' read -ra READS <<< "$FLAGS_reads"
for I in "${!READS[@]}"; do 
	FILE="${READS[$I]}"
	verifyFileExistence "$FILE"
	READS[$I]=$(abspath "$FILE")
done

if [ ${#READS[@]} -eq 2 ]; then
	DOCKER_READ_R1="$DOCKER_BASE_DIR/reads/r1_"$(basename ${READS[0]})
	DOCKER_READ_R2="$DOCKER_BASE_DIR/reads/r2_"$(basename ${READS[1]})
	READ_PART="-1 '$DOCKER_READ_R1' -2 '$DOCKER_READ_R2'"
	MOUNT_DOCKER_READS="-v '${READS[0]}:$DOCKER_READ_R1:ro' -v '${READS[1]}:$DOCKER_READ_R2:ro'"
else
	DOCKER_READ_U="$DOCKER_BASE_DIR/reads/u_"$(basename ${READS[0]})
	READ_PART="-U '$DOCKER_READ_U'"
	MOUNT_DOCKER_READS="-v '${READS[0]}:$DOCKER_READ_U:ro'"
fi

# create output folder
OUT_BASE=$(createOutputFolder "$FLAGS_outfile")
DOCKER_OUTPUT_FOLDER="$DOCKER_BASE_DIR/writeable"
DOCKER_OUTPUT_FILENAME=$(basename "$FLAGS_outfile")
MOUNT_DOCKER_OUTPUT="-v '$OUT_BASE:$DOCKER_OUTPUT_FOLDER'"

# build the command and run it
COMMAND="docker run $MOUNT_DOCKER_INDEX $MOUNT_DOCKER_READS $MOUNT_DOCKER_OUTPUT  ${CONTAINER} bowtie2 -x '$DOCKER_INDEX/$GENOME_INDEX_NAME' $READ_PART -S '$DOCKER_OUTPUT_FOLDER/$DOCKER_OUTPUT_FILENAME'"

# run it
FAIL=0
eval "$COMMAND" 2>&1 | tee "$FLAGS_outfile.log" &
PID_DOCKER=$!
wait $PID_DOCKER
CODE=$?

# check exit code
if [ $CODE -eq 0 ]; then
	# check if mapping file is there and contains some mappings
	if [ -e "$FLAGS_outfile" ]; then
		COUNT=$(grep -P -v "^@" "$FLAGS_outfile" | head -n 1 | wc -l)
		if [ $COUNT -ne 1 ]; then
			echoError "Mapping file '$FLAGS_outfile' contains less than one mapped reads."
			echoInfo "check the log file for more information: '${FLAGS_outfile}.log'"
			exit 1
		else
			exit 0
		fi
	else
		echoError "Mapping file '$FLAGS_outfile' was not found."
		echoInfo "check the log file for more information: '${FLAGS_outfile}.log'"	
		exit 2
	fi

else
	echoError "docker returned an exit code not equal to zero: '$CODE'"
	echoInfo "check the log file for more information: '${FLAGS_outfile}.log'"
	exit $CODE
fi

# exit with exit status 0 or exit code shoul be there earlier
echoError "Reached end of script! Exit should be performed earlier..."
exit $EXIT_REACHED_END_OF_SCRIPT
