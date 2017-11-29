#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh # include some basic functions
SCRIPT=$(readlink_own -m $SCRIPT_FOLDER/bamstats.sh)
GLOBAL_TEST_DATA=$SCRIPT_FOLDER/../../test_data
TEST_DATA_FOLDER=$SCRIPT_FOLDER/test_data
FAILED_TESTS=0

TMP_OUT="/tmp"

# call with invalid parameter
testExitCode "/bin/bash $SCRIPT" "$EXIT_MISSING_ARGUMENTS" "Missing parameter test"
testExitCode "/bin/bash $SCRIPT -b $TEST_DATA_FOLDER/notExistingFile.super -o $TMP_OUT/bamstats1.test -r 100" "$EXIT_MISSING_INPUT_FILES" "Missing input file test"
testExitCode "/bin/bash $SCRIPT -b $GLOBAL_TEST_DATA/test.file.notReadable -o $TMP_OUT/bamstats2.test -r 100" "$EXIT_MISSING_INPUT_FILES" "Not readable input file test"

# real calls
testExitCode "/bin/bash $SCRIPT -b $GLOBAL_TEST_DATA/fastq/joined_bam/test_paired_new.bam -o $TMP_OUT/bamstats3.test -r 101 -a $GLOBAL_TEST_DATA/annotation/test.bed --paired" "$EXIT_OK" "Simple stats test" "$TMP_OUT/bamstats3.test/test_paired_new/idxstats.txt"
testExitCode "/bin/bash $SCRIPT -b $GLOBAL_TEST_DATA/fastq/joined_bam/test_paired_new.bam --disableAllDefault -o $TMP_OUT/bamstats4.test -r 101 -a $GLOBAL_TEST_DATA/annotation/test.bed --paired" "$EXIT_OK" "Disable all test" " " "$TMP_OUT/bamstats4.test/test_paired_new/idxstats.txt"

# delete all the temporary file
rm -f -r $TMP_OUT/bamstats*.test 2>&1 > /dev/null

# return the number of failed tests
exit $FAILED_TESTS


