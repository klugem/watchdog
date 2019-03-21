#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh $@

# check, if used tools are installed
USED_TOOLS='sed:head:wc:echo:bc:cut:cutadapt'
MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS" "check_shflag_tools")
CODE=$?

if [ $CODE -ne 0 ]; then
	echoError "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi

# define parameters
DEFINE_string 'fastq' '' "path to one FASTQ file" 'f'
DEFINE_string 'outfile' '' 'path to an output file' 'o'
DEFINE_string 'infofile' '' '[optional] path to a file which will contain trimming statistics' 'i'
DEFINE_string 'prim3' '' "[optional] adapter that was ligated at the 3' end; '$' at the end will cause that the adapter is anchored at the end of the read" ''
DEFINE_string 'prim5' '' "[optional] adapter that was ligated at the 5' end; '^' at the start will cause that the adapter is anchored at the beginning of the read" ''
DEFINE_string 'adapter' '' "[optional] adapter that can be located at the 3' and 5' end" ''
DEFINE_float 'errorRate' '0.05' '[optional] maximum allowed error rate' 'e'
DEFINE_integer 'repeat' '1' '[optional] try to remove adapters at most COUNT times' 'r'
DEFINE_integer 'minOverlap' '6' '[optional] minimum overlap length' ''
DEFINE_integer 'minLength' '40' '[optional] minimum read length after trimming' ''
DEFINE_integer 'maxLength' '-1' '[optional] maximum read length after trimming' '' 
DEFINE_integer 'cutFixedLength' '0' '[optional] trimmes a fixed length from the beginning (positive numbers) or the end of the reads (negative numbers)' 'c'
DEFINE_integer 'shortenReads' '0' '[optional] shorten reads to a maximal length after trimming; positive values keep the beginning of reads; negative ones the ends (starting from cutadapt version 1.17)'
DEFINE_float 'qualityCutoff' '0' '[optional] trimmes reads at the ends using a sliding window approach' ''
DEFINE_integer 'qualityBase' '33' '[optional] base quality value' ''
DEFINE_boolean 'noIndels' '1' '[optional] does not allow indels between read and adapter' ''
DEFINE_boolean 'discardTrimmed' '1' '[optional] discard sequences which were trimmed' ''
DEFINE_boolean 'discardUntrimmed' '1' '[optional] discard sequences which were not trimmed' ''
DEFINE_boolean 'maskAdapters' '1' '[optional] does not cut the adapters but replace the corresponding regions with N' ''
DEFINE_string 'returnFilePath' '' 'path to the return variables file' ''
DEFINE_boolean 'debug' 'false' '[optional] prints out debug messages.' ''

# parse parameters
FLAGS "$@" || exit $EXIT_INVALID_ARGUMENTS
eval set -- "${FLAGS_ARGV}"
printParamValues "initial parameters" # print param values, if in debug mode
if [ -z "$FLAGS_shortenReads" ] && [ "$FLAGS_shortenReads" -eq 0 ]; then
	echoError "Parameter --shortenReads can not be zero. (see --help for details)";
	exit $EXIT_INVALID_ARGUMENTS
fi
# check if mandatory arguments are there
if [ -z "$FLAGS_fastq" ]; then
	echoError "Parameter -f must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_outfile" ]; then
	echoError "Parameter -o must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_prim3" ] && [ -z "$FLAGS_prim5" ] && [ -z "$FLAGS_adapter" ]; then
	echoError "At least one adapter must be set using --prim3, --prim5 or --adapter. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ $(echo "scale=0; $FLAGS_errorRate*100" | bc | cut -d "." -f 1) -gt 100 ] || [ $(echo "scale=0; $FLAGS_errorRate*100" | bc | cut -d "." -f 1) -lt 0 ]; then
	echoError "Parameter -e must be between [0, 1]. (see --help for details)";
	exit $EXIT_INVALID_ARGUMENTS
fi
if [ "$FLAGS_repeat" -gt 100 ] || [ "$FLAGS_repeat" -lt 1 ]; then
	echoError "Parameter -r must be between [1, 100]. (see --help for details)";
	exit $EXIT_INVALID_ARGUMENTS
fi
if [ -z "$FLAGS_shortenReads" ] && [ "$FLAGS_shortenReads" -eq 0 ]; then
	echoError "Parameter --shortenReads can not be zero. (see --help for details)";
	exit $EXIT_INVALID_ARGUMENTS
fi
if [ "$FLAGS_minOverlap" -lt 1 ] ; then
	echoError "Parameter --minOverlap must be greater than 1. (see --help for details)";
	exit $EXIT_INVALID_ARGUMENTS
fi
if [ "$FLAGS_minLength" -gt 100000 ] || [ "$FLAGS_minLength" -lt 1 ]; then
	echoError "Parameter --minLength must be between [1, 100000]. (see --help for details)";
	exit $EXIT_INVALID_ARGUMENTS
fi
if [ "$FLAGS_maxLength" -gt 100000 ] || [ "$FLAGS_maxLength" -lt 1 ]; then
	if [ "$FLAGS_maxLength" -ne -1 ]; then
		echoError "Parameter --maxLength must be between [1, 100000]. (see --help for details)";
		exit $EXIT_INVALID_ARGUMENTS
	fi
fi
if [ "$FLAGS_cutFixedLength" -gt 1000000 ] || [ "$FLAGS_cutFixedLength" -lt -1000000 ]; then
	echoError "Parameter -c must be between [-1000000, 1000000]. (see --help for details)";
	exit $EXIT_INVALID_ARGUMENTS
fi
if [ $FLAGS_noIndels -eq 0 ]; then
	if [ ! -z "$FLAGS_adapter" ] || [[ ! -z "$FLAGS_prim3" && ! "$FLAGS_prim3" =~ ^[ATCGN]+\$$ ]] || [[ ! -z "$FLAGS_prim5" && ! "$FLAGS_prim5" =~ ^\^[ATCGN]+$ ]]; then
		echoError "Not allowing indels is currently supported only for anchored 5' and 3' adapters."
		exit $EXIT_INVALID_ARGUMENTS
	fi
fi
printParamValues "parameters before actual script starts" # print param values, if in debug mode
##################################################### START with actual SCRIPT ##################################################### 

# check, if the input files exist
verifyFileExistence "$FLAGS_fastq"

# create the output folder
OUT_BASE=$(createOutputFolder "${FLAGS_outfile}")
if [ ! -z "$FLAGS_infofile" ]; then OUT_BASE=$(createOutputFolder "${FLAGS_infofile}"); fi

# build the command
COMMAND="cutadapt "

ADAPTER=""
ACOUNT=0
MIN_SEQ="$FLAGS_minOverlap"
# add the adapters
IFS=',' read -a A3 <<< "$FLAGS_prim3"
IFS=',' read -a A5 <<< "$FLAGS_prim5"
IFS=',' read -a A53 <<< "$FLAGS_adapter"
for I in "${!A3[@]}"; do A="${A3[$I]}"; ADAPTER="$ADAPTER -a $A"; ACOUNT=$((ACOUNT+1)); if [ ${#A} -lt $MIN_SEQ ]; then MIN_SEQ=${#A}; fi; done
for I in "${!A5[@]}"; do A="${A5[$I]}"; ADAPTER="$ADAPTER -g $A"; ACOUNT=$((ACOUNT+1)); if [ ${#A} -lt $MIN_SEQ ]; then MIN_SEQ=${#A}; fi; done
for I in "${!A53[@]}"; do A="${A53[$I]}"; ADAPTER="$ADAPTER -b $A"; ACOUNT=$((ACOUNT+1)); if [ ${#A} -lt $MIN_SEQ ]; then MIN_SEQ=${#A}; fi; done

# add the settings
SETTINGS="-e $FLAGS_errorRate -n $FLAGS_repeat -O $MIN_SEQ --quality-base $FLAGS_qualityBase"
if [ "$FLAGS_minLength" -ne -1 ]; then SETTINGS="$SETTINGS -m $FLAGS_minLength"; fi
if [ "$FLAGS_maxLength" -ne -1 ]; then SETTINGS="$SETTINGS -M $FLAGS_maxLength"; fi
if [ "$FLAGS_cutFixedLength" -ne 0 ]; then SETTINGS="$SETTINGS -u $FLAGS_cutFixedLength"; fi
if [ "$FLAGS_qualityCutoff" -ne 0 ]; then SETTINGS="$SETTINGS -q $FLAGS_qualityCutoff"; fi
if [ "$FLAGS_shortenReads" -ne 0 ]; then SETTINGS="$SETTINGS -l $FLAGS_shortenReads"; fi

# add the flags
FLAGS=""
if [ "$FLAGS_noIndels" -eq 0 ]; then FLAGS="$FLAGS --no-indels"; fi
if [ "$FLAGS_discardTrimmed" -eq 0 ]; then FLAGS="$FLAGS --discard-trimmed"; fi
if [ "$FLAGS_discardUntrimmed" -eq 0 ]; then FLAGS="$FLAGS --discard-untrimmed"; fi
if [ "$FLAGS_maskAdapters" -eq 0 ]; then FLAGS="$FLAGS --mask-adapter"; fi

# build the final command
if [ ! -z "$FLAGS_infofile" ]; then FLAGS="$FLAGS --info-file '$FLAGS_infofile'"; fi
COMMAND="$COMMAND $ADAPTER $SETTINGS $FLAGS -o '$FLAGS_outfile' '$FLAGS_fastq'"

# run it
MESSAGE=$(eval "$COMMAND" 2>&1 | sed 's|%|%%|g')
RET=$?
CODE=0

# check, if return code is ok
if [ $RET -eq 0 ]; then
	# check, if tool run to end.
	PROCESSED_ADAPTERS=$(echo -e "$MESSAGE" | grep -E "=== Adapter $ACOUNT ===" | wc -l)
	if [ $PROCESSED_ADAPTERS -ne 1 ]; then
		echoError "Cutadapt was either terminated before it finished or something else went wrong."
		echoAError "It seems that not all adapters were processed by cutadapt."
		CODE=1
	else
		# check, if the files are there
		if [ ! -f "$FLAGS_outfile" ]; then
			echoError "Output file '$FLAGS_outfile' is missing."
			CODE=1
		else 
			# test, if output contains some input
			COUNT=$(head -n 4 "$FLAGS_outfile" | wc -l)
			if [ $COUNT -ne 4 ]; then
				CODE=1
				echoError "Fastq file '$FLAGS_outfile' contains less than one read."
			fi
		fi
	fi
fi

# check exit code
if [ $RET -eq 0 ] && [ $CODE -eq 0 ] ; then
	# output the original message
	printf "$MESSAGE\n"
	writeParam2File "$FLAGS_returnFilePath" "cutadaptTrimFile" "$FLAGS_outfile"
	writeParam2File "$FLAGS_returnFilePath" "cutadaptInfoFile" "$FLAGS_infofile"
	blockUntilFileIsWritten "$FLAGS_returnFilePath"
	exit $EXIT_OK
else
	echoError "Cutadapt run failed. Output file was deleted. See log of Cutadapt below"
	echoAError "error code: '$RET'"
	 # output the original message
	printf "$MESSAGE\n"
	rm -f "$FLAGS_outfile" 2>&1 > /dev/null
	if [ ! -z "$FLAGS_infofile" ]; then rm -f "$FLAGS_infofile" 2>&1 > /dev/null; fi
	exit $EXIT_FAILED
fi

# exit with exit status 0 or exit code should be there earlier
echoError "Reached end of script! Exit should be performed earlier..."
exit $EXIT_REACHED_END_OF_SCRIPT
