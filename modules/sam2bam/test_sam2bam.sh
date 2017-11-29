#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh # include some basic functions
SCRIPT=$(readlink_own -m $SCRIPT_FOLDER/sam2bam.sh)
GLOBAL_TEST_DATA=$SCRIPT_FOLDER/../../test_data
TEST_DATA_FOLDER=$SCRIPT_FOLDER/test_data
FAILED_TESTS=0

TMP_OUT="/tmp"

TEST_S_NEW="$GLOBAL_TEST_DATA/fastq/joined_samWithSequences/test_single_new.sam"
TEST_S_OLD="$GLOBAL_TEST_DATA/fastq/joined_samWithSequences/test_single_old.sam"
TEST_P_NEW="$GLOBAL_TEST_DATA/fastq/joined_samWithSequences/test_paired_new.sam"
TEST_P_OLD="$GLOBAL_TEST_DATA/fastq/joined_samWithSequences/test_paired_old.sam"

# call with invalid parameter
testExitCode "/bin/bash $SCRIPT" "$EXIT_MISSING_ARGUMENTS" "Missing parameter test"
testExitCode "/bin/bash $SCRIPT -s $TEST_DATA_FOLDER/notExistingFile.super -b $TMP_OUT/sam2bam1.test" "$EXIT_MISSING_INPUT_FILES" "Missing input file test"
testExitCode "/bin/bash $SCRIPT -s $GLOBAL_TEST_DATA/test.file.notReadable -b $TMP_OUT/sam2bam2.test" "$EXIT_MISSING_INPUT_FILES" "Not readable input file test"
testExitCode "/bin/bash $SCRIPT -s $TEST_S_OLD -b $TMP_OUT/sam2bam3.test -t 0" "$EXIT_INVALID_ARGUMENTS" "Invalid thread parameter test"
testExitCode "/bin/bash $SCRIPT -s $TEST_S_OLD -b $TMP_OUT/sam2bam4.test -m 123" "$EXIT_INVALID_ARGUMENTS" "Invalid memory parameter test"
testExitCode "/bin/bash $SCRIPT -s $TEST_S_OLD -b $TMP_OUT/sam2bam5.test -q 0" "$EXIT_INVALID_ARGUMENTS" "Invalid quality parameter test"

# real calls
testExitCode "/bin/bash $SCRIPT -s $TEST_S_OLD -b $TMP_OUT/sam2bam6.test -q 2 -t 2 -m 16M" "$EXIT_OK" "Single end old format conversion test" "$TMP_OUT/sam2bam6.test"
testExitCode "/bin/bash $SCRIPT -s $TEST_S_NEW -b $TMP_OUT/sam2bam7.test -q 2 -t 2 -m 16M" "$EXIT_OK" "Single end new format conversion test" "$TMP_OUT/sam2bam7.test"
testExitCode "/bin/bash $SCRIPT -s $TEST_P_OLD -b $TMP_OUT/sam2bam8.test -q 2 -t 2 -m 16M" "$EXIT_OK" "Paired end old format conversion test" "$TMP_OUT/sam2bam8.test"
testExitCode "/bin/bash $SCRIPT -s $TEST_P_NEW -b $TMP_OUT/sam2bam9.test -q 2 -t 2 -m 16M" "$EXIT_OK" "Paired end new format conversion test" "$TMP_OUT/sam2bam9.test"

# delete all the temporary file
rm -f $TMP_OUT/sam2bam*.test 2>&1 > /dev/null

# return the number of failed tests
exit $FAILED_TESTS
