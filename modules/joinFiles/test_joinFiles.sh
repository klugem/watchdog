#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh # include some basic functions
SCRIPT=$(readlink_own -m $SCRIPT_FOLDER/joinFiles.sh)
GLOBAL_TEST_DATA=$SCRIPT_FOLDER/../../test_data
TEST_DATA_FOLDER=$SCRIPT_FOLDER/test_data
FAILED_TESTS=0

TMP_OUT="/tmp/joinFiles.test.result.txt"
rm -f $TMP_OUT

# call with invalid parameter
testExitCode "/bin/bash $SCRIPT" "$EXIT_MISSING_ARGUMENTS" "Missing parameter test"
testExitCode "/bin/bash $SCRIPT -i $TEST_DATA_FOLDER/notExistingFile.super -o $TMP_OUT" "$EXIT_MISSING_INPUT_FILES" "Missing input file test"
testExitCode "/bin/bash $SCRIPT -i $GLOBAL_TEST_DATA/test.file.notReadable -o $TMP_OUT" "$EXIT_MISSING_INPUT_FILES" "Not readable input file test"
testExitCode "/bin/bash $SCRIPT -i $TEST_DATA_FOLDER/fileA.txt -o $TMP_OUT" "$EXIT_INVALID_ARGUMENTS" "Only one file test"

# real calls
testExitCode "/bin/bash $SCRIPT -i $TEST_DATA_FOLDER/fileA.txt,$TEST_DATA_FOLDER/fileB.txt -o ${TMP_OUT}.1" "$EXIT_OK" "Simple join test" "${TMP_OUT}.1" " " "$TEST_DATA_FOLDER/md5.txt"
testExitCode "/bin/bash $SCRIPT -i $GLOBAL_TEST_DATA/fastq/pairedEnd_oldFormat_101bp_1.fastq,$GLOBAL_TEST_DATA/fastq/pairedEnd_oldFormat_101bp_2.fastq -o ${TMP_OUT}.2 -c" "$EXIT_OK" "Fastq old format test" "${TMP_OUT}.2" " " "$TEST_DATA_FOLDER/md5.txt"
#testExitCode "/bin/bash $SCRIPT -i $GLOBAL_TEST_DATA/fastq/pairedEnd_newFormat_101bp_1.fastq,$GLOBAL_TEST_DATA/fastq/pairedEnd_newFormat_101bp_2.fastq -o ${TMP_OUT}.3 -c" "$EXIT_OK" "Fastq new format test" "${TMP_OUT}.3" " " "$TEST_DATA_FOLDER/md5.txt"

# delete all the temporary file
rm -f $TMP_OUT.* 2>&1 > /dev/null

# return the number of failed tests
exit $FAILED_TESTS

