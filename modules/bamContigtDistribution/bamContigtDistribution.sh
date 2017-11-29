#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh $@

# check, if used tools are installed
USED_TOOLS='Rscript:cut:grep:bc'
MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS" "check_shflag_tools")
CODE=$?

INSTALL_DIR="$SCRIPT_FOLDER/R/"
SCRIPT_PATH="bamstats.R"

if [ $CODE -ne 0 ]; then
	echoError "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi
USED_TOOLS='^R|getopt'
MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS")
CODE=$?

if [ $CODE -ne 0 ]; then
	echoError "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi

# define parameters
DEFINE_string 'bamMergedStats' '' 'path to the merged bam stats file' ''
DEFINE_string 'outputFile' '' 'path to a output pdf file' ''
DEFINE_boolean 'debug' 'false' '[optional] prints out debug messages.' ''

# parse parameters
FLAGS "$@" || exit $EXIT_INVALID_ARGUMENTS
eval set -- "${FLAGS_ARGV}"
printParamValues "initial parameters" # print param values, if in debug mode

# check if mandatory bamMergedStats are there
if [ -z "$FLAGS_bamMergedStats" ]; then
	echoError "Parameter --bamMergedStats must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_outputFile" ]; then
	echoError "Parameter --outputFile must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
printParamValues "parameters before actual script starts" # print param values, if in debug mode
##################################################### START with actual SCRIPT ##################################################### 

# test if files are there and readable
verifyFileExistence "$FLAGS_bamMergedStats"

# get an absolute path for the files
FLAGS_bamMergedStats=$(abspath $FLAGS_bamMergedStats)

# create output folder, if not already there
OUT_BASE=$(createOutputFolder "$FLAGS_outputFile")
touch "$FLAGS_outputFile"
FLAGS_outputFile=$(abspath $FLAGS_outputFile)

# all parameters seem to be ok, run the script!
RUN2END=$(getTmpFile "bamContigtDistribution")
COMMAND="Rscript '$INSTALL_DIR/$SCRIPT_PATH' --confirmRun2EndFile '$RUN2END'"
# build the command
for PARAM in $__flags_longNames; do
	# ignore that parameter since it is only for the module
	if [ "$PARAM" == "debug" ]; then
		continue
	fi

	V_NAME='FLAGS_'"$PARAM"
	VALUE=${!V_NAME}
	V_NAME='__flags_'"$PARAM"'_type'
	TYPE=${!V_NAME}
	
	# test if value for that parameter was set
	if [ ! -z "$VALUE" ]; then
		# test if flag
		if [ $TYPE -eq 1 ]; then
			# test if flag is enabled
			if  [ $VALUE -eq 0 ]; then
				COMMAND="$COMMAND --$PARAM"
			fi
		else 
			COMMAND="$COMMAND --$PARAM '$VALUE'"
		fi
	fi
done

# execute the command
LOG="${FLAGS_output}.rscript.log"
if [ -e "$LOG" ]; then
	rm "$LOG"
fi
echo $COMMAND
executeCommand "$COMMAND" "$LOG" "Rscript bamContictDistribution"
# test, if the script run to its end
verifyRun2End "$RUN2END"

# if, we come till here, all should be ok.
exit $EXIT_OK
