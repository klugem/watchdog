#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh $@

# check, if used tools are installed
USED_TOOLS='Rscript:cut:grep:bc'
MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS" "check_shflag_tools")
CODE=$?

INSTALL_DIR="$SCRIPT_FOLDER/R/"
SCRIPT_PATH="performTest.R"

if [ $CODE -ne 0 ]; then
	echoError "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi
USED_TOOLS='^R|DESeq|DESeq2|edgeR|limma|Biobase|getopt|RColorBrewer|gplots|genefilter|lattice'
MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS")
CODE=$?

if [ $CODE -ne 0 ]; then
	echoError "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi

# define parameters
DEFINE_string 'controlCondition' '' 'name of the control condition' ''
DEFINE_string 'testCondition' '' 'name of the test condition' ''
DEFINE_string 'countFile' '' 'count file with features in rows and samples in columns' 'c'
DEFINE_string 'sampleAnnotation' '' 'annotation file with sample names in the first colum and sample condition in the second condition (header: sample\tcondition)' 's'
DEFINE_string 'featureAnnotation' '' 'annotation file which is joined with the count file' 'f'
DEFINE_string 'featureAnnotationID' 'FeatureID' 'name of the column with is used for joining' ''
DEFINE_string 'featureAnnotationType' 'type' 'name of the column in the annotation file for which a distribution plot is created' ''
DEFINE_string 'excludeSamples' '' 'names of samples that should be excluded from the analysis' 'e'
DEFINE_float 'pValueCutoff' '0.01' 'p-Value cutoff for significant results' 'p'
DEFINE_integer 'minKeepReads' '25' 'number of reads a feature must own in average per sample to pass filtering step before DE test is performed' 'm'
DEFINE_string 'foldchangeCutoff' '0.0,0.415,1.0' "log2 foldchange cutoffs for which a own result file will be created; will be used for both directions (+/-); separated by ','" ''
DEFINE_string 'foldchangeCutoffNames' 'significant,0.33-fold,2-fold' "corresponding names to the foldchange cutoffs; separated by ','" ''
DEFINE_string 'output' '' 'output folder' 'o'
DEFINE_string 'method' 'all' 'method that should be applied; {limma, DESeq, DESeq2, edgeR, all}' ''
DEFINE_boolean 'debug' 'false' '[optional] prints out debug messages.' ''

# parse parameters
FLAGS "$@" || exit $EXIT_INVALID_ARGUMENTS
eval set -- "${FLAGS_ARGV}"
printParamValues "initial parameters" # print param values, if in debug mode

# check if mandatory arguments are there
if [ -z "$FLAGS_controlCondition" ]; then
	echoError "Parameter --controlCondition must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_testCondition" ]; then
	echoError "Parameter --testCondition must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_countFile" ]; then
	echoError "Parameter --countFile must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_sampleAnnotation" ]; then
	echoError "Parameter --sampleAnnotation must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_output" ]; then
	echoError "Parameter --output must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
printParamValues "parameters before actual script starts" # print param values, if in debug mode
##################################################### START with actual SCRIPT ##################################################### 

# test, if method is valid
if [ "$FLAGS_method" != "all" ] && [ "$FLAGS_method" != "limma" ] && [ "$FLAGS_method" != "DESeq" ] && [ "$FLAGS_method" != "DESeq2" ] && [ "$FLAGS_method" != "edgeR" ]; then
	echoError "'$FLAGS_method' is no valid method name!"
	exit $EXIT_INVALID_ARGUMENTS
fi

# test if files are there and readable
verifyFileExistence "$FLAGS_countFile"
verifyFileExistence "$FLAGS_sampleAnnotation"

# get an absolute path for the files
FLAGS_countFile=$(abspath $FLAGS_countFile)
FLAGS_sampleAnnotation=$(abspath $FLAGS_sampleAnnotation)
if [ ! -z "$FLAGS_featureAnnotation" ]; then
	verifyFileExistence "$FLAGS_featureAnnotation"
	FLAGS_featureAnnotation=$(abspath $FLAGS_featureAnnotation)
fi

# create output folder, if not already there
OUT_BASE=$(createOutputFolder "$FLAGS_output/.dummyFile")

# check, if the foldchange cutoffs have the same length
IFS=',' read -ra FC <<< "$FLAGS_foldchangeCutoff"
IFS=',' read -ra FCN <<< "$FLAGS_foldchangeCutoffNames"
if [ ${#FC[@]} != ${#FC[@]} ]; then
	echoError "Number of foldchanges and names for foldchanges must be equal!"
	exit $EXIT_INVALID_ARGUMENTS
fi
# validate if header of sample file is correct
if [ ! "$(head -n 1 "$FLAGS_sampleAnnotation")" == "sample	condition" ]; then
	echoError "Sample file does not have the required header. (sample\tcondition)"
	exit $EXIT_INVALID_ARGUMENTS
fi

# check, if name of control and test condition are in sample file
NUM_CONTROL=$(cut -f 2 "$FLAGS_sampleAnnotation" | grep -c "$FLAGS_controlCondition")
NUM_TEST=$(cut -f 2 "$FLAGS_sampleAnnotation" | grep -c "$FLAGS_testCondition")
if [ "$NUM_CONTROL" -eq 0 ] || [ "$NUM_TEST" -eq 0 ]; then
	echoError "Both test conditions must contain at least one sample ($FLAGS_controlCondition: $NUM_CONTROL / $FLAGS_testCondition: $NUM_TEST).";
	exit $EXIT_INVALID_ARGUMENTS
fi

# check, if the samples, which should be excluded are part of the count file
IFS=',' read -ra EXC <<< "$FLAGS_excludeSamples"
for I in "${!EXC[@]}"; do 
	E="${EXC[$I]}"
	COUNT=$(cut -f 1 "$FLAGS_sampleAnnotation" | grep -E "^$E\$" -c)
	if [ $COUNT -eq 1 ]; then
		# get the type of the sample
		T=$(grep -E "^$E" "$FLAGS_sampleAnnotation" | cut -f 2)
		if [ "$T" == "$FLAGS_controlCondition" ]; then
			NUM_CONTROL=$((NUM_CONTROL-1))
		else
			if [ "$T" == "$FLAGS_testCondition" ]; then
				NUM_TEST=$((NUM_TEST-1))
			fi
		fi
	else
		echoError "Sample '$E' can not be excluded because it is not part of the sample list.";
		exit $EXIT_INVALID_ARGUMENTS
	fi
done

# check, if the feature file is correct
if [ ! -z "$FLAGS_featureAnnotation" ]; then
	if [ $(head -n 1 "$FLAGS_featureAnnotation" | grep -c -E $'\t'"$FLAGS_featureAnnotationID|$FLAGS_featureAnnotationID"$'\t') -eq 0 ]; then
		echoError "Column '$FLAGS_featureAnnotationID' was not found in the feature annotation file '$FLAGS_featureAnnotation'!";
		exit $EXIT_INVALID_ARGUMENTS
	fi
	if [ $(head -n 1 "$FLAGS_featureAnnotation" | grep -c -E $'\t'"$FLAGS_featureAnnotationType|$FLAGS_featureAnnotationType"$'\t') -eq 0 ]; then
		echoError "Column '$FLAGS_featureAnnotationType' was not found in the feature annotation file '$FLAGS_featureAnnotation'!";
		exit $EXIT_INVALID_ARGUMENTS
	fi
fi

# check, if after removal enough samples are there
if [ "$NUM_CONTROL" -le 0 ] || [ "$NUM_TEST" -le 0 ]; then
	echoError "After removal of excluded samples at least one test condition contains no samples anymore ($FLAGS_controlCondition: $NUM_CONTROL / $FLAGS_testCondition: $NUM_TEST).";
	exit $EXIT_INVALID_ARGUMENTS
fi

# check, if cutoffs have the correct value
if [ "$FLAGS_minKeepReads" -le 0 ]; then
	echoError "Parameter --minKeepReads must be greater than zero!"
	exit $EXIT_INVALID_ARGUMENTS
fi
if [ $(echo "$FLAGS_pValueCutoff < 0" | bc -l) -eq 1 ] || [ $(echo "$FLAGS_pValueCutoff > 1" | bc -l) -eq 1 ]; then
	echoError "Parameter --pValueCutoff must be between zero and one!"
	exit $EXIT_INVALID_ARGUMENTS
fi

# all parameters seem to be ok, run the script!
RUN2END=$(getTmpFile "DETest")
COMMAND="Rscript '$INSTALL_DIR/$SCRIPT_PATH' --installDir '$INSTALL_DIR' --confirmRun2EndFile '$RUN2END'"
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
echo $COMMAND
# execute the command
LOG="$FLAGS_output/Rscript.log"
if [ -e "$LOG" ]; then
	rm "$LOG"
fi
executeCommand "$COMMAND" "$LOG" "Rscript DETest"
# test, if the script run to its end
verifyRun2End "$RUN2END"

# if, we come till here, all should be ok.
exit $EXIT_OK
