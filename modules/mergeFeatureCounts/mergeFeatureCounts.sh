#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh $@
trap 'checkExitCode $LINENO' ERR # check, the exit code and paste the last command which failed

INSTALL_DIR="$SCRIPT_FOLDER/R/"
SCRIPT_PATH_CPM="counts2rpkm.R"
SCRIPT_PATH_FEATURES="featureStatistic.R"
SCRIPT_PATH_MAP="ContextMap.R"

# check, if used tools are installed
USED_TOOLS='basename:rm:sed:head:cat:tail:sort:md5sum:cut:awk:grep'
MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS" "check_shflag_tools")
CODE=$?

if [ $CODE -ne 0 ]; then
	echoError "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi
 
# define parameters
DEFINE_string 'searchFolder' '' 'path to the folder in which the script looks for *.counts files' 's'
DEFINE_string 'statsFolder' '' '[optional] path to merged statistic folder' ''
DEFINE_string 'output' '' 'path to the output file' 'o'
DEFINE_string 'featureAnnotation' '' 'annotation file which is joined with the count file' 'f'
DEFINE_string 'featureAnnotationID' 'Geneid' 'name of the column with is used for joining' ''
DEFINE_string 'featureAnnotationType' 'type' '[optional] name of the column in the annotation file for which a distribution plot is created' ''
DEFINE_string 'featureAnnotationExonLength' 'exon_length' '[optional] name of the column that contains the exon_length of the features' ''
DEFINE_string 'returnFilePath' '' 'path to the return variables file' ''
DEFINE_boolean 'noPlotting' 'false' '[optional] disables the call of R scripts' ''
DEFINE_boolean 'prefixNames' 'false' '[optional] prefixes the names of the features with continuous numbers' ''
DEFINE_boolean 'debug' 'false' '[optional] prints out debug messages.' ''

# parse parameters
FLAGS "$@" || exit $EXIT_INVALID_ARGUMENTS
eval set -- "${FLAGS_ARGV}"
printParamValues "initial parameters" # print param values, if in debug mode

# check if mandatory arguments are there
if [ -z "$FLAGS_searchFolder" ]; then
	echoError "Parameter -s must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_output" ]; then
	echoError "Parameter -o must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi

printParamValues "parameters before actual script starts" # print param values, if in debug mode
##################################################### START with actual SCRIPT ##################################################### 

# check, if the input files exist
verifyFolderExistence "$FLAGS_searchFolder"

# check, if some files were found
if [ $(ls -1 "$FLAGS_searchFolder/" | grep -c -E ".counts$") -eq 0 ]; then
	echoError "No files with .counts ending were found in '$FLAGS_searchFolder'."
	exit $EXIT_MISSING_INPUT_FILES
fi

# check, if the feature file is correct
if [ ! -z "$FLAGS_featureAnnotation" ]; then
	if [ $(head -n 1 "$FLAGS_featureAnnotation" | grep -c -E $'\t'"$FLAGS_featureAnnotationID|$FLAGS_featureAnnotationID"$'\t') -eq 0 ]; then
		echoError "Column '$FLAGS_featureAnnotationID' was not found in the feature annotation file '$FLAGS_featureAnnotation'!";
		exit $EXIT_INVALID_ARGUMENTS
	fi
	if [ $(head -n 1 "$FLAGS_featureAnnotation" | grep -c -E $'\t'"$FLAGS_featureAnnotationExonLength|$FLAGS_featureAnnotationExonLength"$'\t') -eq 0 ]; then
		echoError "Column '$FLAGS_featureAnnotationExonLength' was not found in the feature annotation file '$FLAGS_featureAnnotation'!";
		exit $EXIT_INVALID_ARGUMENTS
	fi
	if [ $(head -n 1 "$FLAGS_featureAnnotation" | grep -c -E $'\t'"$FLAGS_featureAnnotationType|$FLAGS_featureAnnotationType"$'\t') -eq 0 ]; then
		echoError "Column '$FLAGS_featureAnnotationType' was not found in the feature annotation file '$FLAGS_featureAnnotation'!";
		exit $EXIT_INVALID_ARGUMENTS
	fi
fi

# create output folder
OUT_BASE=$(createOutputFolder "$FLAGS_output")
TMP_FOLDER=$(getTmpFile mergeFeatureCounts.names)
TMP_FOLDER=$(createOutputFolder "$TMP_FOLDER/.dummyFile")
trap "rm -rf \"$TMP_FOLDER\" 2>&1 > /dev/null" EXIT

L_CHECK=""
# ensure that the feature are in the same order
for i in "$FLAGS_searchFolder"/*.counts; 
do
	B=$(basename "$i")
	tail -n +3 "$i" | sort > "$TMP_FOLDER/$B"
	CHECK=$(cut -f 1 "$TMP_FOLDER/$B" | md5sum | cut -f 1)

	if [ "$L_CHECK" != "" ]; then
		if [ "$L_CHECK" != "$CHECK" ]; then
			echoError "Feature names of '$i' are not the same as for the other files."
			exit $EXIT_MISFORMATED_INPUT
		fi
	else
		L_CHECK=$CHECK
	fi
done

# build the command
COMMAND="paste \"$TMP_FOLDER/gene.names\" ";
FILES="FeatureID"
FILE_COUNTER=1 # start with 1 because of gene names
for i in "$TMP_FOLDER"/*.counts; 
do
        COMMAND="$COMMAND <(cut -f 7 \"$i\")"
        LAST=$i
        B=$(basename "$i")
        FILES="$FILES\t${B%.counts}"
	FILE_COUNTER=$((FILE_COUNTER+1))
done

# get gene names
cut -f 1 "$LAST" > "$TMP_FOLDER/gene.names"
FEATURES=$(wc -l "$LAST" | cut -f 1 -d " ")
FEATURES=$((FEATURES+1)) # add header line

# ensure that old file is deleted
rm -f "$FLAGS_output" 2>&1 > /dev/null
COMMAND="$COMMAND > '$TMP_FOLDER/out.tmp'"
$(eval ${COMMAND})

# output the header
echo -e "$FILES" | sed 's/.unmapped.bwa//g' > "$FLAGS_output"
cat "$TMP_FOLDER/out.tmp" >> "$FLAGS_output"
rm -f "$TMP_FOLDER.*" 2>&1 > /dev/null

# test if file is there
if [ -f "$FLAGS_output" ]; then
	# test, if line number is correct
	OUT_LINE=$(wc -l "$FLAGS_output" | cut -f 1 -d " ")
	if [ $FEATURES -ne $OUT_LINE ]; then
		echoErr "Something went wrong because output file has not the same number of lines as the input files."
		exit $EXIT_FAILED
	fi
	# test if the numer of colums is correct
	OUT_COL=$(awk '{print NF; exit 0}' "$FLAGS_output")
	if [ $FILE_COUNTER -ne $OUT_COL ]; then
		echoErr "Something went wrong because output file has not the correct number of columns."
		exit $EXIT_FAILED
	fi

	# prefix the names with continuous numbers
	if [ $FLAGS_prefixNames -eq 0 ]; then
		COMMAND="awk '{if(NR != 1) {printf \"%s\",(NR-1)\"_\"}; print $1;}' \"$FLAGS_output\" > \"$TMP_FOLDER/lineNumbers\" && mv \"$TMP_FOLDER/lineNumbers\" \"$FLAGS_output\""
		executeCommand "$COMMAND" "/dev/null" "prefix the names"
	fi

	if [ ! -z "$FLAGS_featureAnnotation" ] && [ $FLAGS_noPlotting -ne 0 ]; then
		# normalize it with R script
		# all parameters seem to be ok, run the script!
		RUN2END=$(getTmpFile "mergeFeatureCounts")
		COMMAND="Rscript '$INSTALL_DIR/$SCRIPT_PATH_CPM' --confirmRun2EndFile '$RUN2END' --countFile '$FLAGS_output' --featureAnnotation '$FLAGS_featureAnnotation' --featureAnnotationID '$FLAGS_featureAnnotationID' --featureAnnotationExonLength '$FLAGS_featureAnnotationExonLength' --outFolder '$OUT_BASE'"

		executeCommand "$COMMAND" "/dev/null" "Rscript normalize"
		# test, if the script run to its end
		verifyRun2End "$RUN2END"

		if [ ! -z "$FLAGS_statsFolder" ]; then
			# ensure that the file is there
			verifyFileExistence "$FLAGS_statsFolder/Idxstats.txt"
			verifyFileExistence "$FLAGS_statsFolder/Basic_Statistics.txt"
			verifyFileExistence "$FLAGS_statsFolder/Assignment_statistic.txt"

			# create statistic
			RUN2END=$(getTmpFile "mergeFeatureCounts")
			COMMAND="Rscript '$INSTALL_DIR/$SCRIPT_PATH_FEATURES' --confirmRun2EndFile '$RUN2END' --countFile '$FLAGS_output' --featureAnnotation '$FLAGS_featureAnnotation' --featureAnnotationID '$FLAGS_featureAnnotationID' --featureAnnotationType '$FLAGS_featureAnnotationType' --bamMergedStats '$FLAGS_statsFolder/Idxstats.txt' --outFolder '$OUT_BASE'"
			
			executeCommand "$COMMAND" "/dev/null" "Rscript statistic I"
			# test, if the script run to its end
			verifyRun2End "$RUN2END"
	
			# create mapping statistic
			RUN2END=$(getTmpFile "mergeFeatureCounts")
			COMMAND="Rscript '$INSTALL_DIR/$SCRIPT_PATH_MAP' --confirmRun2EndFile '$RUN2END' --statsFolder '$FLAGS_statsFolder' --outFile '${FLAGS_output}'"

			executeCommand "$COMMAND" "/dev/null" "Rscript statistic II"
			# test, if the script run to its end
			verifyRun2End "$RUN2END"
		fi
	fi

	# all is ok, exit!
	writeParam2File "$FLAGS_returnFilePath" "mergedCountFile" "$FLAGS_output"
	blockUntilFileIsWritten "$FLAGS_returnFilePath"
	exit $EXIT_OK
fi

# exit with exit status 0 or exit code should be there earlier
echoError "Reached end of script! Exit should be performed earlier..."
exit $EXIT_REACHED_END_OF_SCRIPT
