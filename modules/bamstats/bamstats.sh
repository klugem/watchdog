#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh $@
trap 'checkExitCode $LINENO' ERR # check, the exit code and paste the last command which failed

# define parameters
DEFINE_string 'bam' '' "path to one or more BAM file(s); multiple values can be separated by ','" 'b'
DEFINE_string 'outdir' '' 'path to the output folder; individual files will be stored in a sub-folder (using the basename of the BAM file as folder name)' 'o'
DEFINE_integer 'readLength' '' 'maximal length of the reads' 'r'
DEFINE_integer 'sampleDepth' '100000' '[optional] number of reads which are used for sampling' 's'
DEFINE_string 'annotation' '' '[optional] gene annotation in BED format' 'a'
DEFINE_string 'geneBodyAnnotation' '' '[optional] genes that are used to calculate the gene body coverage; should contain house keeping genes' 'g'
DEFINE_boolean 'idxstats' '0' '[optional] number of reads mapped on each chromosome' ''
DEFINE_boolean 'flagstat' '0' '[optional] flags of mapped reads' ''
DEFINE_boolean 'count' '1' '[optional] raw and rpkm count table for exons, introns and mRNAs' ''
DEFINE_boolean 'saturation' '0' '[optional] down-samples the mapped reads to infer the sequencing depth' ''
DEFINE_boolean 'clipping' '0' '[optional] clipping statistic of the mapped reads' ''
DEFINE_boolean 'insertion' '0' '[optional] insertion statistic of the mapped reads' ''
DEFINE_boolean 'deletion' '0' '[optional] deletion statistic of the mapped reads' ''
DEFINE_boolean 'inferExperiment' '0' 'tries to infer if the sequencing was strand specific or not' ''
DEFINE_boolean 'junctionAnnotation' '0' '[optional] checks how many of the splice junctions are novel or annotated' ''
DEFINE_boolean 'junctionSaturation' '0' '[optional] down-samples the spliced reads to infer if sequencing depth is enough for splicing analyses' ''
DEFINE_boolean 'distribution' '0' '[optional] calculates how mapped reads are distributed over genomic features' ''
DEFINE_boolean 'duplication' '0' '[optional] calculates duplication levels' ''
DEFINE_boolean 'gc' '0' '[optional] GC-content of the mapped reads' ''
DEFINE_boolean 'nvc' '0' '[optional] checks if a nucleotide composition bias exists' ''
DEFINE_boolean 'insertSize' '0' '[optional] calculates the insert size between two paired RNA reads' ''
DEFINE_boolean 'fragmentSize' '0' '[optional] calculates the fragment size for each transcript' ''
DEFINE_boolean 'tin' '0' '[optional] calculates the transcript integrity number which is similar to the RNA integrity number' ''
DEFINE_boolean 'statistics' '0' '[optional] calculate reads mapping statistics' ''
DEFINE_boolean 'geneBodyCoverage' '1' '[optional] check if reads coverage is uniform and if there is any 5’ or 3’ bias' ''
DEFINE_boolean 'paired' '1' '[optional] paired end library sequencing' ''
DEFINE_boolean 'stranded' '1' '[optional] stranded library sequencing' ''
DEFINE_boolean 'disableAllDefault' '1' '[optional] disable all which are not explicitly activated' ''
DEFINE_boolean 'debug' 'false' '[optional] prints out debug messages.' ''

# this parameters are disabled when not explicitely activated by the 'disableAllDefault' flag
ALL_FLAGS="idxstats,flagstat,count,saturation,clipping,insertion,deletion,inferExperiment,junctionAnnotation,junctionSaturation,distribution,duplication,gc,nvc,insertSize,fragmentSize,tin,statistics,geneBodyCoverage"

# parse parameters
FLAGS "$@" || exit $EXIT_INVALID_ARGUMENTS
eval set -- "${FLAGS_ARGV}"
printParamValues "initial parameters" # print param values, if in debug mode

# check if mandatory arguments are there
if [ -z "$FLAGS_bam" ]; then
	echoError "Parameter -b must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_outdir" ]; then
	echoError "Parameter -o must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_readLength" ]; then
	echoError "Parameter -r must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ "$FLAGS_readLength" -gt 2048 ] || [ "$FLAGS_readLength" -lt 1 ]; then
	echoError "Parameter -r must be between [1, 2048]. (see --help for details)";
	exit $EXIT_INVALID_ARGUMENTS
fi
if [ "$FLAGS_sampleDepth" -gt 1000000000000 ] || [ "$FLAGS_sampleDepth" -lt 100000 ]; then
	echoError "Parameter -s must be between [100000, 1000000000000]. (see --help for details)";
	exit $EXIT_INVALID_ARGUMENTS
fi

# disable all which are not explicitely activated
if [ $FLAGS_disableAllDefault -eq 0 ]; then
	IFS=',' read -ra CHECK_FLAGS <<< "$ALL_FLAGS"
	for I in "${!CHECK_FLAGS[@]}"; do 
		F_NAME="${CHECK_FLAGS[$I]}"
		case "${__flags_opts[@]}" in  *"--${F_NAME}"*) continue ;; esac # break, if found otherwise, reset
		eval "FLAGS_${F_NAME}=1" # disable the flag
	done
fi

# if no annotation is, given, disable all the modules, which need it
if [ -z "$FLAGS_annotation" ]; then
	FLAGS_count=1
	FLAGS_saturation=1
	FLAGS_inferExperiment=1
	FLAGS_junctionAnnotation=1
	FLAGS_junctionSaturation=1
	FLAGS_distribution=1
	FLAGS_insertSize=1
	FLAGS_tin=1
	FLAGS_fragmentSize=1
fi

# if no gene body annotation is, given, disable all the modules, which need it
if [ -z "$FLAGS_geneBodyAnnotation" ]; then
	FLAGS_geneBodyCoverage=1
fi

# check, if used tools are installed
USED_TOOLS='samtools:basename'
USED_TOOLS_ADD='FPKM_count.py:RPKM_saturation.py:inner_distance.py:bam_stat.py:clipping_profile.py:insertion_profile.py:deletion_profile.py:infer_experiment.py:junction_annotation.py:junction_saturation.py:read_distribution.py:
read_duplication.py:read_GC.py:read_NVC.py:geneBody_coverage.py:RNA_fragment_size.py:tie.py'

# check, if the additional python scripts are needed
IFS=',' read -ra CHECK_FLAGS <<< "$ALL_FLAGS"
for I in "${!CHECK_FLAGS[@]}"; do 
	F_NAME="${CHECK_FLAGS[$I]}"
	if [ "$F_NAME" == "idxstats" ] || [ "$F_NAME" == "flagstat" ]; then
		continue;
	fi
	N="FLAGS_${F_NAME}"
	if [ ${!N} -eq 0 ]; then
		USED_TOOLS="$USED_TOOLS:$USED_TOOLS_ADD"
		break;
	fi
done

MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS" "check_shflag_tools")
CODE=$?
if [ $CODE -ne 0 ]; then
	echoAError "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi

printParamValues "parameters before actual script starts" # print param values, if in debug mode
##################################################### START with actual SCRIPT ##################################################### 

# check, if the input files exist
if [ ! -z "$FLAGS_annotation" ]; then verifyFileExistence "$FLAGS_annotation"; fi
if [ ! -z "$FLAGS_geneBodyAnnotation" ]; then verifyFileExistence "$FLAGS_geneBodyAnnotation"; fi

# split the input array and check, if all files are there
IFS=',' read -ra FILES <<< "$FLAGS_bam"
for I in "${!FILES[@]}"; do 
	FILE="${FILES[$I]}"
	verifyFileExistence "$FILE"
done

if [ $FLAGS_paired -eq 0 ]; then
	PAIRED="PE"
else
	PAIRED="SE"
fi

# set strand information
if [ $FLAGS_stranded -eq 0 ]; then
	if [ $FLAGS_paired -eq 0 ]; then
		STRAND='--strand=1++,1--,2+-,2-+'
	else
		STRAND='--strand=++,--' 
	fi
else
	STRAND=""
fi

# process all the bam files
for I in "${!FILES[@]}"; do 
	BAM_FILE="${FILES[$I]}"
	# get name of input file
	
	BASE=$(basename "${BAM_FILE}")
	BASE=${BASE%.bam}

	# create output folder
	OUT_BASE=$(createOutputFolder "${FLAGS_outdir}/${BASE}/.dummyFile")
	cd "$OUT_BASE" # switch to that folder
	LOG_BASE=$(createOutputFolder "${FLAGS_outdir}/${BASE}/log/.dummyFile")

	# call each of the scripts, when the flag is set
	if [ $FLAGS_idxstats -eq 0 ]; then samtools idxstats "${BAM_FILE}" > "$OUT_BASE/idxstats.txt" 2> "$LOG_BASE/idxstats.log"; fi
	if [ $FLAGS_flagstat -eq 0 ]; then samtools flagstat "${BAM_FILE}" > "$OUT_BASE/flagstat.txt" 2> "$LOG_BASE/flagstat.log"; fi
	if [ $FLAGS_count -eq 0 ]; then FPKM_count.py -i "$BAM_FILE" -o "$OUT_BASE/$BASE" -r "$FLAGS_annotation" --skip-multi-hits $STRAND > "$LOG_BASE/FPKM_count.log" 2>&1; fi
	if [ $FLAGS_saturation -eq 0 ]; then RPKM_saturation.py -i "$BAM_FILE" -o "$OUT_BASE/$BASE" -r "$FLAGS_annotation" $STRAND > "$LOG_BASE/RPKM_saturation.log" 2>&1; fi
	if [ $FLAGS_statistics -eq 0 ]; then bam_stat.py -i "$BAM_FILE" 2> "$OUT_BASE/bam_stat.txt" > "$LOG_BASE/bam_stat.log"; fi
	if [ $FLAGS_clipping -eq 0 ]; then clipping_profile.py -i "$BAM_FILE" -o "$OUT_BASE/$BASE" -s "$PAIRED" > "$LOG_BASE/clipping_profile.log" 2>&1; fi
	if [ $FLAGS_insertion -eq 0 ]; then insertion_profile.py -i "$BAM_FILE" -o "$OUT_BASE/$BASE" -s "$PAIRED" > "$LOG_BASE/insertion_profile.log" 2>&1; fi
	if [ $FLAGS_deletion -eq 0 ]; then deletion_profile.py -i "$BAM_FILE" -o "$OUT_BASE/$BASE" -l $FLAGS_readLength -n $FLAGS_sampleDepth > "$LOG_BASE/deletion_profile.log" 2>&1; fi
	if [ $FLAGS_inferExperiment -eq 0 ]; then infer_experiment.py -i "$BAM_FILE" -r "$FLAGS_annotation" -s $FLAGS_sampleDepth > "$OUT_BASE/infer_experiment.txt" 2> "$LOG_BASE/infer_experiment.log"; fi
	if [ $FLAGS_junctionAnnotation -eq 0 ]; then junction_annotation.py -i "$BAM_FILE" -o "$OUT_BASE/$BASE" -r "$FLAGS_annotation" > "$LOG_BASE/junction_annotation.log" 2>&1; fi
	if [ $FLAGS_junctionSaturation -eq 0 ]; then junction_saturation.py -i "$BAM_FILE" -o "$OUT_BASE/$BASE" -r "$FLAGS_annotation" > "$LOG_BASE/junction_saturation.log" 2>&1; fi
	if [ $FLAGS_distribution -eq 0 ]; then read_distribution.py -i "$BAM_FILE" -r "$FLAGS_annotation" > "$OUT_BASE/read_distribution.txt" 2> "$LOG_BASE/read_distribution.log"; fi
	if [ $FLAGS_duplication -eq 0 ]; then read_duplication.py -i "$BAM_FILE" -o "$OUT_BASE/$BASE" > "$LOG_BASE/read_duplication.log" 2>&1; fi
	if [ $FLAGS_gc -eq 0 ]; then read_GC.py -i "$BAM_FILE" -o "$OUT_BASE/$BASE" > "$LOG_BASE/read_GC.log" 2>&1; fi
	if [ $FLAGS_nvc -eq 0 ]; then read_NVC.py -i "$BAM_FILE" -o "$OUT_BASE/$BASE"> "$LOG_BASE/read_NVC.log" 2>&1; fi
	if [ $FLAGS_tin -eq 0 ]; then tin.py -i "$BAM_FILE" -r "$FLAGS_annotation" -c 50 > /dev/null 2> "$LOG_BASE/tin.log"; fi
	if [ $FLAGS_fragmentSize -eq 0 ]; then RNA_fragment_size.py -i "$BAM_FILE" -r "$FLAGS_annotation" -n 25 > "$OUT_BASE/fragmentSize.txt" 2> "$LOG_BASE/fragmentSize.log"; fi

	# paired end mode
	if [ $FLAGS_paired -eq 0 ]; then
		if [ $FLAGS_insertSize -eq 0 ]; then inner_distance.py -i "$BAM_FILE" -o "$OUT_BASE/$BASE" -k $FLAGS_sampleDepth -r "$FLAGS_annotation" > "$LOG_BASE/inner_distance.log" 2>&1; fi
	fi
done

# perform gene body coverage
if [ $FLAGS_geneBodyCoverage -eq 0 ]; then geneBody_coverage.py -r "$FLAGS_geneBodyAnnotation" -i "$FLAGS_bam" -o "${FLAGS_outdir}" > "${FLAGS_outdir}/${BASE}/geneBody_coverage.log" 2>&1; fi

# if no error occoured till here, all is ok.
exit $OK_EXIT
