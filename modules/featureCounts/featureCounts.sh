#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
MODULE_VERSION_PARAMETER_NAME="--moduleVersion"
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh $@
TMP_FOLDER=$(getDefaultFolderForTmpFiles)
cd "$TMP_FOLDER"

# check, if used tools are installed
USED_TOOLS='featureCounts:printf:sed:tr:echo'
MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS" "check_shflag_tools")
CODE=$?

if [ $CODE -ne 0 ]; then
	echoError "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi

# define parameters
# params used in any version of the module
DEFINE_string 'annotation' '' 'feature annotation in GTF or SAF format' 'a'
DEFINE_string 'input' '' 'indexed BAM file which should be used for counting' 'i'
DEFINE_string 'output' '' 'path to output file' 'o'
DEFINE_string 'annotationType' '' '[optional] disables automatic type detection based on the file ending of the input file; valid values: GTF or SAF;' ''
DEFINE_string 'featureType' 'exon' '[optional] feature type (e.g. exon or intron) which is used for counting in GTF mode' 'f'
DEFINE_string 'groupType' 'gene_id' '[optional] attribute which is used for summarization in GTF mode' 'g'
DEFINE_boolean 'disableGroupSummarization' '1' '[optional] flag that can be used to turn summarization on groupType off' ''
DEFINE_integer 'stranded' '0' '[optional] indicates strand-specific read counting; possible values:  0 (unstranded), 1 (stranded) and 2 (reversely stranded)' 's'
DEFINE_integer 'threads' '1' '[optional] number of threads used for counting' 't'
DEFINE_boolean 'multiMapping' '1' 'when enabled all alignments of multi mapping reads are counted' ''
DEFINE_boolean 'primary' '0' '[optional] when enabled only alignments which are flagged as primary alignments are counted' 'p'
DEFINE_boolean 'countFragments' '1' '[optional] counts fragments instead of reads; only for paired end data' 'P'
DEFINE_string 'returnFilePath' '' 'path to the return variables file' ''
DEFINE_boolean 'multiCountMetaFeatures' '1' '[optional] allows a read to be counted for more than one meta-feature' 'O'
DEFINE_boolean 'detailedReadAssignments' '1' '[optional] saves for each read if it was assigned or not; filename: {input_file_name}.featureCounts; format: read name<TAB>status<TAB>feature name<TAB>number of counts for that read' 'd'
# params only available in module version 1 - #VER_TAG:1-1
if [ ${MODULE_VERSION} -eq 1 ]; then
	DEFINE_string 'minOverlap' '1' '[optional] minimum number of overlapping bases required to assign a read to a feature; also negative values are allowed' 'm'
# params only available in module version 2 - #VER_TAG:2-2
elif [ ${MODULE_VERSION} -eq 2 ]; then
	DEFINE_string 'minReadOverlap' '1' '[optional] minimum number of overlapping bases required to assign a read to a feature; also negative values are allowed' 'm'
	DEFINE_string 'minFracOverlap' '0' '[optional] assign reads to the meta-feature/feature which has the largest number of overlapping bases' ''
	DEFINE_integer 'readExtension5' '0' "[optional] extend reads at the 5' end" ''
	DEFINE_integer 'readExtension3' '0' "[optional] extend reads at the 3' end" ''
	DEFINE_boolean 'fraction' '1' '[optional] count fractional; only in combination with the assignToAllOverlappingFeatures or/and multiMapping flag(s)' ''
	DEFINE_boolean 'largestOverlap' '1' '[optional] assign reads to a meta-feature/feature that has the largest number of overlapping bases.' ''
	DEFINE_boolean 'longReads' '1' '[optional] mode for long read counting (e.g. Nanopore or PacBio)' ''
fi
DEFINE_integer 'moduleVersion' '1' '[optional] version of the module that should be used' ''
DEFINE_boolean 'debug' 'false' '[optional] prints out debug messages.' ''

# parse parameters
FLAGS "$@" || exit $EXIT_INVALID_ARGUMENTS
eval set -- "${FLAGS_ARGV}"
printParamValues "initial parameters" # print param values, if in debug mode

# check if mandatory arguments are there
if [ -z "$FLAGS_annotation" ]; then
	echoError "Parameter -a must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_input" ]; then
	echoError "Parameter -i must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_output" ]; then
	echoError "Parameter -o must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ "$FLAGS_annotationType" != "" ] && [ "$FLAGS_annotationType" != "GTF" ] && [ "$FLAGS_annotationType" != "SAF" ]; then
	echoError "Parameter --annotationType must be either GTF or SAF. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ "$FLAGS_stranded" -gt 2 ] || [ "$FLAGS_stranded" -lt 0 ]; then
	echoError "Parameter -s must be between [0, 2]. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ "$FLAGS_threads" -lt 0 ] ; then
	echoError "Parameter -t must be greater than 0. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi

printParamValues "parameters before actual script starts" # print param values, if in debug mode
##################################################### START with actual SCRIPT ##################################################### 

# check, if the input files exist
verifyFileExistence "$FLAGS_input"
verifyFileExistence "$FLAGS_annotation"

# create output folder
createOutputFolder "$FLAGS_output"

# check, type of SAF or GTF
ANNO_TYPE=$FLAGS_annotationType
ENDING_raw=$(echo $FLAGS_annotation | sed 's/^.*\.//')
ENDING=$(echo $ENDING_raw | tr '[:lower:]' '[:upper:]')
if [ "$ANNO_TYPE" == "" ]; then
	if [ "$ENDING" != "SAF" ] && [ "$ENDING" != "GTF" ]; then
		echoError "The type of the annotation file could not be detected automatically as it is '$ENDING_raw'. Please specify the type with the parameter --annotationType (see --help for details)."
		exit $EXIT_MISSING_ARGUMENTS
	else 
		ANNO_TYPE=$ENDING
	fi
fi

# build the command
COMMAND="featureCounts -a '$FLAGS_annotation' -o '$FLAGS_output' -F $ANNO_TYPE"

if [ $FLAGS_disableGroupSummarization -eq 0 ]; then 
	 COMMAND="$COMMAND -f"
fi
if [ "$ANNO_TYPE" == "GTF" ]; then 
	COMMAND="$COMMAND -t $FLAGS_featureType -g $FLAGS_groupType"
fi
if [ $FLAGS_stranded -ne 0 ]; then 
	COMMAND="$COMMAND -s '$FLAGS_stranded'"
fi
if [ $FLAGS_threads -ne 1 ]; then 
	COMMAND="$COMMAND -T '$FLAGS_threads'"
fi
if [ $FLAGS_multiMapping -eq 0 ]; then 
	COMMAND="$COMMAND -M"
fi
if [ $FLAGS_primary -eq 0 ]; then 
	COMMAND="$COMMAND --primary"
fi
if [ $FLAGS_countFragments -eq 0 ]; then 
	COMMAND="$COMMAND -p"
fi
if [ $FLAGS_multiCountMetaFeatures -eq 0 ]; then
	COMMAND="$COMMAND -O"
fi
if [ $FLAGS_detailedReadAssignments -eq 0 ]; then
	COMMAND="$COMMAND -R"
fi

# params only available in module version 1
if [ ${MODULE_VERSION} -eq 1 ]; then
	if [ $FLAGS_minOverlap -ne 1 ]; then 
		COMMAND="$COMMAND --minReadOverlap '$FLAGS_minOverlap'"
	fi
# params only available in module version 2
elif [ ${MODULE_VERSION} -eq 2 ]; then
	if [ $FLAGS_minReadOverlap -ne 1 ]; then 
		COMMAND="$COMMAND --minOverlap '$FLAGS_minReadOverlap'"
	fi
	if [ $FLAGS_minFracOverlap -ne 0 ]; then 
		COMMAND="$COMMAND --fracOverlap '$FLAGS_minFracOverlap'"
	fi
	if [ $FLAGS_largestOverlap -eq 0 ]; then 
		COMMAND="$COMMAND --largestOverlap"
	fi
	if [ $FLAGS_fraction -eq 0 ]; then
		COMMAND="$COMMAND --fraction"
	fi
	if [ $FLAGS_readExtension5 -gt 0 ]; then
		COMMAND="$COMMAND --readExtension5 $FLAGS_readExtension5"
	fi
	if [ $FLAGS_readExtension3 -gt 0 ]; then
		COMMAND="$COMMAND --readExtension3 $FLAGS_readExtension3"
	fi
	if [ $FLAGS_longReads -eq 0 ]; then
		COMMAND="$COMMAND -L"
	fi
fi
COMMAND="$COMMAND '$FLAGS_input'"

# run it
MESSAGE=$(eval "$COMMAND" 2>&1 | sed 's|%|%%|g')
RET=$?

# check, if tool run to end.
COUNT=$(echo $MESSAGE | grep -c "Read assignment finished.")
if [ $COUNT -ne 1 ]; then
	echoError "FeatureCounts was terminated before it finished."
	CODE=1
fi

# get base output name
BASENAME=$(basename "${FLAGS_reads}")
BASENAME=${BASENAME%.*}

# check exit code
if [ $CODE -eq 0 ]; then
	# output the original message
	printf "$MESSAGE\n"
	writeParam2File "$FLAGS_returnFilePath" "FeatureCountCountFile" "$FLAGS_output/$BASENAME.counts"
	writeParam2File "$FLAGS_returnFilePath" "FeatureCountSummaryFile" "$FLAGS_output/$BASENAME.summary"
	blockUntilFileIsWritten "$FLAGS_returnFilePath"
	exit $EXIT_OK
else
	echoError "FeatureCounts run failed. Count file was deleted. See error of featureCounts below"
	echoAError "error code: '$CODE'"
	 # output the original message
	printf "$MESSAGE\n"
	rm -f "$FLAGS_output" 2>&1 > /dev/null
	rm -f "$FLAGS_output.summary" 2>&1 > /dev/null
	exit $EXIT_FAILED
fi

# exit with exit status 0 or exit code shoul be there earlier
echoError "Reached end of script! Exit should be performed earlier..."
exit $EXIT_REACHED_END_OF_SCRIPT
