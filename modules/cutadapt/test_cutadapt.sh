#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh # include some basic functions
SCRIPT=$(readlink_own -m $SCRIPT_FOLDER/cutadapt.sh)
GLOBAL_TEST_DATA=$SCRIPT_FOLDER/../../test_data
TEST_DATA_FOLDER=$SCRIPT_FOLDER/test_data
FAILED_TESTS=0

TMP_OUT="/tmp"

# call with invalid parameter
testExitCode "/bin/bash $SCRIPT" "$EXIT_MISSING_ARGUMENTS" "Missing parameter test"
testExitCode "/bin/bash $SCRIPT -f $TEST_DATA_FOLDER/notExistingFile.super -o $TMP_OUT/cutadapt1.test --adapter TTTT" "$EXIT_MISSING_INPUT_FILES" "Missing input file test"
testExitCode "/bin/bash $SCRIPT -f $GLOBAL_TEST_DATA/test.file.notReadable -o $TMP_OUT/cutadapt2.test --adapter TTTT" "$EXIT_MISSING_INPUT_FILES" "Not readable input file test"
testExitCode "/bin/bash $SCRIPT -f $TEST_DATA_FOLDER/notExistingFile.super -o $TMP_OUT/cutadapt3.test" "$EXIT_MISSING_ARGUMENTS" "Missing adapter test"
testExitCode "/bin/bash $SCRIPT -f $TEST_DATA_FOLDER/notExistingFile.super -o $TMP_OUT/cutadapt4.test --adapter TTTT --noIndels" "$EXIT_INVALID_ARGUMENTS" "NoIndel test I"
testExitCode "/bin/bash $SCRIPT -f $TEST_DATA_FOLDER/notExistingFile.super -o $TMP_OUT/cutadapt5.test --prim3 TTTT --noIndels" "$EXIT_INVALID_ARGUMENTS" "NoIndel test II"

# real calls
testExitCode "/bin/bash $SCRIPT -f $GLOBAL_TEST_DATA/fastq/pairedEnd_newFormat_101bp_1.fastq -o $TMP_OUT/cutadapt6.test --prim3 TTTT" "$EXIT_OK" "Paired end new format test" "$TMP_OUT/cutadapt6.test"
testExitCode "/bin/bash $SCRIPT -f $GLOBAL_TEST_DATA/fastq/pairedEnd_oldFormat_101bp_1.fastq -o $TMP_OUT/cutadapt7.test --prim3 TTTT" "$EXIT_OK" "Paired end old format test" "$TMP_OUT/cutadapt7.test"
testExitCode "/bin/bash $SCRIPT -f $GLOBAL_TEST_DATA/fastq/singleEnd_newFormat_101bp.fastq -o $TMP_OUT/cutadapt8.test --prim3 TTTT" "$EXIT_OK" "Single end new format test" "$TMP_OUT/cutadapt8.test"
testExitCode "/bin/bash $SCRIPT -f $GLOBAL_TEST_DATA/fastq/singleEnd_oldFormat_101bp.fastq -o $TMP_OUT/cutadapt9.test --prim3 TTTT" "$EXIT_OK" "Single end old format test" "$TMP_OUT/cutadapt9.test"

# delete all the temporary file
rm -f $TMP_OUT/cutadapt*.test 2>&1 > /dev/null

# return the number of failed tests
exit $FAILED_TESTS
