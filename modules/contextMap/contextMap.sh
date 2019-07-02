#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh $@

# check, if used tools are installed
USED_TOOLS='java:tail:grep:rm:echo:tee:wc:head:basename:hostname:awk'
MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS" "check_shflag_tools")
CODE=$?

if [ $CODE -ne 0 ]; then
	echoError "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi

# define parameters
DEFINE_string 'jarPath' '' 'path to contextmap jar' 'j'
DEFINE_string 'reads' '' 'path to reads in fasta or fastq format' 'r'
DEFINE_string 'alignerName' '' 'name of a alignment tools; supported values: "bwa", "bowtie1" or "bowtie2"' '' #replace _ with capital letter
DEFINE_string 'alignerBin' '' 'absolute path to the executable of the chosen aligner tool' '' #replace _ with capital letter
DEFINE_string 'indexerBin' '' "absolute path to the executable of the aligner's indexing tool (not needed for BWA)" '' #replace _ with capital letter
DEFINE_string 'indices' '' 'comma separated list of paths to basenames of indices, which can be used by the chosen aligner!' 'i'
DEFINE_string 'genome' '' 'path to a directory with genome sequences in fasta format (for each chromosome a separate file)' 'g'
DEFINE_string 'output' '' 'path to the output directory' 'o' #renamed from o to output

DEFINE_string 'skipsplit' '' "comma separated list of booleans, each element refers to a given aligner index (same ordering). 'true' for no split detection, 'false' otherwise (REQ. in mining mode)." ''
DEFINE_string 'skipmultisplit' '' "comma separated list of booleans, each element refers to a given aligner index (same ordering). 'true' for no multisplit detection, 'false' otherwise (REQ. in mining mode)." ''
DEFINE_string 'speciesindex' '' "path to a directory containing index files created with the 'indexer' tool (REQ. in mining mode)>" ''
DEFINE_string 'alignerTmp' '' 'path to a directory for temporary alignment files' '' #replace _ with capital letter
DEFINE_integer 'seed' '' 'seed length for the alignment (default: Bwt1: 30, BWA/Bwt2: 20)' ''
DEFINE_integer 'seedmismatches' '' 'allowed mismatches in the seed region (default: Bwt1: 1, BWA/Bwt2: 0)' ''
DEFINE_integer 'mismatches' '4' 'allowed mismatches in the whole read' ''
DEFINE_integer 'splitseedsizes' '15' 'seed size for the split search seed (default: 15)' ''
DEFINE_integer 'splitseedmismatches' '0' 'allowed mismatches for the split seed search (default: 0)' ''
DEFINE_integer 'mmdiff' '0' 'maximum allowed mismatch difference between the best and second best alignment of the same read' ''
DEFINE_integer 'maxhits' '' 'maximum number of candidate alignments per read. Reads with more hits are skipped (bwa/bwt1) or the already found hits are reported (bwt2) (default for bwa/bwt1:10, bwt2: 3)' ''
DEFINE_integer 'minsize' '10' 'minimum number of reads a genomic region has to contain for being regarded as a local context' ''
DEFINE_string 'gtf' '' 'path to an annotation file in gtf format' ''
DEFINE_integer 'threads' '' '[optional] number of threads used for mapping' ''  #renamed from t to threads
DEFINE_integer 'maxindelsize' '10' '[optional] maximum allowed size of insertions or deletions (default: 10)' ''

DEFINE_boolean 'mining' '1' '[optional] enables the mining for infections or contaminations' ''
DEFINE_boolean 'noclipping' '1' '[optional] disables the calculation of clipped alignments' ''
DEFINE_boolean 'noncanonicaljunctions' '1' '[optional] enables the prediction of non-canonical splice sites' ''
DEFINE_boolean 'strandspecific' '1' '[optional] enables strand specific mapping' ''
DEFINE_boolean 'pairedend' '1' '[optional] enables mapping of paired-end reads. Nomenclature for mates from the same fragment: base_name/1 and base_name/2, respectively; only valid for versions smaller than 2.7.2' ''
DEFINE_boolean 'polyA' '1' '[optional] enables the search for polyA-tails (mutually exclusive with --noclipping)' ''
DEFINE_boolean 'verbose' '1' '[optional] verbose mode' ''
DEFINE_boolean 'keeptmp' '1' '[optional] does not delete some temporary files' ''
DEFINE_boolean 'sequenceDB' '1' '[optional] sequence mapping to disk; recommended for very large data sets.' ''
DEFINE_string 'returnFilePath' '' 'path to the return variables file' ''
DEFINE_string 'localTmpFolder' '/usr/local/storage' 'path to a local storage that is used for temporary data' ''
DEFINE_integer 'memoryScaleFactor' '75' 'scale factor [0,100] in percent that defines the proportion of the memory that is used for java; default memory: 3GB*threads*(scaleFactor/100)' ''
DEFINE_integer 'memoryPerThread' '3072' 'total memory per thread in MB if running on local host; otherwise memory limit of executor might be set; default: 3072' ''
DEFINE_boolean 'debug' 'false' '[optional] prints out debug messages.' ''

# parse parameters
FLAGS "$@" || exit $EXIT_INVALID_ARGUMENTS
eval set -- "${FLAGS_ARGV}"
printParamValues "initial parameters" # print param values, if in debug mode

# check if mandatory arguments are there
if [ -z "$FLAGS_jarPath" ]; then
	echoError "Parameter -j must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
else 
	# test if the file is a jar file
	ENDING=$(echo $FLAGS_jarPath | sed 's/^.*\.//' | tr '[:lower:]' '[:upper:]')
	if [ "$ENDING" != "JAR" ]; then
		echoError "No valid jar file was found in '$FLAGS_jarPath' for parameter --jarPath (see --help for details)";
		exit $EXIT_INVALID_ARGUMENTS
	fi
	# check, which version we are using
	VT=$(basename "$FLAGS_jarPath" ".jar" | cut -d "_" -f 2 | sed 's/v//')
	IFS='.' read -ra V <<< "$VT"
	FV=${V[0]}
	SV=$(echo $VT | sed 's/'$FV'.//' | sed 's/\.//')
	if [ $SV -ge 72 ]; then
		# check, if paired end flag is not given
		if [ "$FLAGS_pairedend" -eq "0" ]; then
			echoError "Pairedend flag is deprecated for version of ContextMap greater than 2.7.1. Provide two separate files in -reads instead. (see --help for details)";
			exit $EXIT_INVALID_ARGUMENTS
		fi 
	else
		if [ $(echo "$FLAGS_reads" | grep "," -c) -ne 0 ]; then
			echoError "Only ContextMap greater than 2.7.1 accepts two files as input. Please join both files before. (see --help for details)";
			exit $EXIT_INVALID_ARGUMENTS
		fi
	fi

fi
if [ -z "$FLAGS_reads" ]; then
	echoError "Parameter -r must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_alignerName" ]; then
	echoError "Parameter --alignerName must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
else 
	if [ "$FLAGS_alignerName" != "bwa" ] && [ "$FLAGS_alignerName" != "bowtie1" ] && [ "$FLAGS_alignerName" != "bowtie2" ]; then
		echoError "Value '$FLAGS_alignerName' is not allowed for the parameter --alignerName (see --help for details)";
		exit $EXIT_INVALID_ARGUMENTS
	fi
fi
if [ "$FLAGS_alignerName" != "bwa" ] && [ -z "$FLAGS_indexerBin" ]; then
	echoError "Parameter --indexerBin must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_alignerBin" ]; then
	echoError "Parameter --alignerBin must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_indices" ]; then
	echoError "Parameter -i must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_genome" ]; then
	echoError "Parameter -g must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_output" ]; then
	echoError "Parameter -o must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
# test required parameters when in mining mode
if [ $FLAGS_mining -eq 0 ]; then
	if [ -z "$FLAGS_skipsplit" ]; then
		echoError "Parameter --skipsplit must be set in mining mode. (see --help for details)";
		exit $EXIT_MISSING_ARGUMENTS
	fi
	if [ -z "$FLAGS_skipmultisplit" ]; then
		echoError "Parameter --skipmultisplit must be set in mining mode. (see --help for details)";
		exit $EXIT_MISSING_ARGUMENTS
	fi
	if [ -z "$FLAGS_speciesindex" ]; then
		echoError "Parameter --speciesindex must be set in mining mode. (1see --help for details)";
		exit $EXIT_MISSING_ARGUMENTS
	fi
fi

printParamValues "parameters before actual script starts" # print param values, if in debug mode
##################################################### START with actual SCRIPT ##################################################### 

# test if all the required input files exist
verifyFileExistence "$FLAGS_jarPath"
# get name of input file

IFS=',' read -ra READS <<< "$FLAGS_reads"
for I in "${!READS[@]}"; do 
	FILE="${READS[$I]}"
	verifyFileExistence "$FILE"
done

if [ ${#READS[@]} -eq 2 ]; then
	F=$(basename "${READS[0]}" | tr -d '\0')
	S=$(basename "${READS[1]}" | tr -d '\0')
	BASENAME=$(echo -e "$F\n$S" | grep -zoP '^(.*)(?=.*?\n\1)')
	BASENAME=${BASENAME%_R}
else
	BASENAME=$(basename "${FLAGS_reads}" | tr -d '\0')
	BASENAME=${BASENAME%.*}
fi

# split the input array and check, if all files are there
IFS=',' read -ra IN <<< "$FLAGS_indices"
for I in "${!IN[@]}"; do 
	FILE="${IN[$I]}"

	if [ "$FLAGS_alignerName" == "bwa" ]; then
		FILE="$FILE.bwt"
	else
		if [ "$FLAGS_alignerName" == "bowtie1" ]; then
			FILE="$FILE.1.ebwt"
		else
			if [ "$FLAGS_alignerName" == "bowtie2" ]; then
				FILE="$FILE.1.bt2"
			fi
		fi
	fi

	# test if file is there and readable
	verifyFileExistence "$FILE"
done

# if skip split is given ensure that is has the same length as the indices
if [ ! -z "$FLAGS_skipsplit" ]; then
	IFS=',' read -ra SPLIT <<< "$FLAGS_skipsplit"
	if [ ${#IN[@]} -ne ${#SPLIT[@]} ]; then
		echoError "The number of --skipsplit parameters (${#SPLIT[@]}) must be the same as the number of given indices (${#IN[@]})."
		exit $EXIT_INVALID_ARGUMENTS	
	fi
	# check, if only true or false is given
	for I in "${!SPLIT[@]}"; do 
		VALUE="${SPLIT[$I]}"
		if [ "$VALUE" != "true" ] && [ "$VALUE" != "false" ]; then
			echoError "Only 'true' or 'false' is allowed for the --skipsplit parameter but '$VALUE' is given."
			exit $EXIT_INVALID_ARGUMENTS
		fi
	done
fi
if [ ! -z "$FLAGS_skipmultisplit" ]; then
	IFS=',' read -ra SPLIT <<< "$FLAGS_skipmultisplit"
	if [ ${#IN[@]} -ne ${#SPLIT[@]} ]; then
		echoError "The number of --skipmultisplit parameters (${#SPLIT[@]}) must be the same as the number of given indices (${#IN[@]})."
		exit $EXIT_INVALID_ARGUMENTS	
	fi
	# check, if only true or false is given
	for I in "${!SPLIT[@]}"; do 
		VALUE="${SPLIT[$I]}"
		if [ "$VALUE" != "true" ] && [ "$VALUE" != "false" ]; then
			echoError "Only 'true' or 'false' is allowed for the --skipmultisplit parameter but '$VALUE' is given."
			exit $EXIT_INVALID_ARGUMENTS
		fi
	done
fi

# check, if genome folder is there
if [ ! -d $FLAGS_genome ]; then
	echoError "Folder '$FLAGS_genome' was not found.";
	exit $EXIT_MISSING_INPUT_FILES
fi

# check gtf file, if one was set
if [ ! -z "$FLAGS_gtf" ]; then
	verifyFileExistence "$FLAGS_gtf"
fi

# check, if the binaries are there
verifyFileExistence "$FLAGS_alignerBin" 1
if [ "$FLAGS_alignerName" != "bwa" ]; then
	verifyFileExistence "$FLAGS_indexerBin" 1
fi

# verify that not valid integers are not set
FLAGS_seed=$(ensureLowerBound "$FLAGS_seed" 1)
FLAGS_seedmismatches=$(ensureLowerBound "$FLAGS_seedmismatches" 0)
FLAGS_mismatches=$(ensureLowerBound "$FLAGS_mismatches" 0)
FLAGS_mmdiff=$(ensureLowerBound "$FLAGS_mmdiff" 0)
FLAGS_maxhits=$(ensureLowerBound "$FLAGS_maxhits" 0)
FLAGS_minsize=$(ensureLowerBound "$FLAGS_minsize" 1)
FLAGS_threads=$(ensureLowerBound "$FLAGS_threads" 1)
FLAGS_maxindelsize=$(ensureLowerBound "$FLAGS_maxindelsize" 0)
FLAGS_memoryScaleFactor=$(ensureLowerBound "$FLAGS_memoryScaleFactor" 1)
FLAGS_memoryScaleFactor=$(ensureUpperBound "$FLAGS_memoryScaleFactor" 100)
FLAGS_memoryPerThread=$(ensureLowerBound "$FLAGS_memoryPerThread" 512)

# create output folder
OUT_BASE=$(createOutputFolder "$FLAGS_output/.dummyFile")

# set output path to temporary path
TMP_FOLDER=$(getTmpFile contextMap "$FLAGS_localTmpFolder")
createOutputFolder "$TMP_FOLDER/.dummyFile"
MEMORY=$(getMemoryForJava $FLAGS_threads $FLAGS_memoryPerThread $FLAGS_memoryScaleFactor) #use 3GB as default if not running on grid

COMMAND="java $MEMORY -XX:+UseConcMarkSweepGC -XX:NewSize=300M -XX:MaxNewSize=300M -jar '$FLAGS_jarPath' mapper"
# build the command
for PARAM in $__flags_longNames; do
	# ignore that parameter since it is only for the module
	if [ "$PARAM" == "jarPath" ] || [ "$PARAM" == "localTmpFolder" ] || [ "$PARAM" == "returnFilePath" ] || [ "$PARAM" == "memoryScaleFactor" ] || [ "$PARAM" == "memoryPerThread" ]; then
		continue
	fi

	V_NAME='FLAGS_'"$PARAM"
	VALUE=${!V_NAME}
	V_NAME='__flags_'"$PARAM"'_type'
	TYPE=${!V_NAME}

	# change the name back
	if [ "$PARAM" != "polyA" ]; then
		PARAM=$(echo $PARAM | sed 's/\([A-Z]\)/_\L\1/')
	fi

	# special cases
	if [ "$PARAM" == "output" ]; then
		PARAM="o"
		VALUE="$TMP_FOLDER"
	fi
	if [ "$PARAM" == "threads" ]; then
		PARAM="t"
	fi
	
	# test if value for that parameter was set
	if [ ! -z "$VALUE" ]; then
		# test if flag
		if [ $TYPE -eq 1 ]; then
			# test if flag is enabled
			if  [ $VALUE -eq 0 ]; then
				COMMAND="$COMMAND --$PARAM"
			fi
		else 
			COMMAND="$COMMAND -$PARAM '$VALUE'"
		fi
	fi
done

# run it
FAIL=0
MESSAGE=$(eval "$COMMAND" 2>&1 | tee "$FLAGS_output/$BASENAME.log")
CODE=$?

# check, if tool run to end.
COUNT=$(echo $MESSAGE | tail -n 1 | grep -c "ContextMap run finished.")

if [ $COUNT -ne 1 ]; then
	echoError "ContextMap was terminated before it finished."
	FAIL=1
else
	# check exit code
	if [ $CODE -eq 0 ]; then
		# check for exception
		MESSAGE_ERROR=$($SCRIPT_FOLDER/../../core_lib/errorChecker.sh "$FLAGS_output/$BASENAME.log" 2>&1)
		CODE_ERROR=$?

		if [ $CODE_ERROR -ne 0 ]; then
			echoError "Error checker found some errors, see found errors below"
			FAIL=2
			echoError "$MESSAGE_ERROR"
		else
			# check if mapping file is there and contains some mappings
			if [ -e "$TMP_FOLDER/mapping.sam" ]; then
				COUNT=$(grep -P -v "^@" "$TMP_FOLDER/mapping.sam" | head -n 1 | wc -l)
				if [ $COUNT -ne 1 ]; then
					FAIL=1
					echoError "Mapping file '$TMP_FOLDER/mapping.sam' contains less than one mapped reads."
				fi
			else
				echoError "Mapping file '$TMP_FOLDER/mapping.sam' was not found."
				FAIL=1
			fi

		fi
	else
		FAIL=1
	fi
fi

# check how the status is
if [ $FAIL -eq 0 ]; then
	echoInfo "Copy results...";
	# rename the output files based on the input filename
	mv "$TMP_FOLDER/mapping.sam" "$FLAGS_output/$BASENAME.sam"

	if [ -e "$TMP_FOLDER/polyA_tails.bed" ]; then
		mv "$TMP_FOLDER/polyA_tails.bed" "$FLAGS_output/$BASENAME.polyA.bed"
	fi
	# output the original message
	echoAInfo "$MESSAGE"
	if [ $FLAGS_keeptmp -eq 1 ]; then
		rm -rf "$TMP_FOLDER" 2>&1 > /dev/null
	else 
		echoInfo "Temporary files can be found in '$TMP_FOLDER' on host '$(hostname)'."
	fi
	writeParam2File "$FLAGS_returnFilePath" "contextMapSAMFile" "$FLAGS_output/$BASENAME.sam"
	writeParam2File "$FLAGS_returnFilePath" "contextMapPolyAFile" "$FLAGS_output/$BASENAME.polyA.bed"
	blockUntilFileIsWritten "$FLAGS_returnFilePath"
	exit $EXIT_OK
else
	if [ $FAIL -eq 1 ]; then
		echoError "ContextMap run failed. Output folder was deleted. See errors above and log of ContextMap below:"
		echoAError "error code: '$CODE'"
		# output the original message
		echoAInfo "$MESSAGE"
	else
		echoError "ContextMap run failed. Output files were deleted. See errors above:"
		echoAError "error code: '$CODE'"
	fi
	rm -f "$TMP_FOLDER/mapping.sam" 2>&1 > /dev/null
	rm -f "$TMP_FOLDER/polyA_tails.bed" 2>&1 > /dev/null
	if [ $FLAGS_keeptmp -eq 1 ]; then
		rm -rf "$TMP_FOLDER" 2>&1 > /dev/null
	else 
		echoInfo "Temporary files can be found in '$TMP_FOLDER' on host '$(hostname)'."
	fi
	exit $EXIT_FAILED
fi

# exit with exit status 0 or exit code shoul be there earlier
echoError "Reached end of script! Exit should be performed earlier..."
exit $EXIT_REACHED_END_OF_SCRIPT
