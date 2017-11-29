#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh # include some basic functions
SCRIPT=$(readlink_own -m $SCRIPT_FOLDER/indexBam.sh)
GLOBAL_TEST_DATA=$SCRIPT_FOLDER/../../test_data
TEST_DATA_FOLDER=$SCRIPT_FOLDER/test_data
FAILED_TESTS=0

TMP_OUT="/tmp"

TEST_S_NEW="$GLOBAL_TEST_DATA/fastq/joined_bam/test_single_new.bam"
TEST_S_OLD="$GLOBAL_TEST_DATA/fastq/joined_bam/test_single_old.bam"
TEST_P_NEW="$GLOBAL_TEST_DATA/fastq/joined_bam/test_paired_new.bam"
TEST_P_OLD="$GLOBAL_TEST_DATA/fastq/joined_bam/test_paired_old.bam"

cp $TEST_S_NEW $TMP_OUT/test_single_new.bam
cp $TEST_S_OLD $TMP_OUT/test_single_old.bam
cp $TEST_P_NEW $TMP_OUT/test_paired_new.bam
cp $TEST_P_OLD $TMP_OUT/test_paired_old.bam

# call with invalid parameter
testExitCode "/bin/bash $SCRIPT" "$EXIT_MISSING_ARGUMENTS" "Missing parameter test"
testExitCode "/bin/bash $SCRIPT -b $TEST_DATA_FOLDER/notExistingFile.super" "$EXIT_MISSING_INPUT_FILES" "Missing input file test"
testExitCode "/bin/bash $SCRIPT -b $GLOBAL_TEST_DATA/test.file.notReadable" "$EXIT_MISSING_INPUT_FILES" "Not readable input file test"

# real calls
testExitCode "/bin/bash $SCRIPT -b $TMP_OUT/test_single_old.bam" "$EXIT_OK" "Simple index test" "$TMP_OUT/test_single_old.bam.bai"
rm -f $TMP_OUT/test_single_old*.bai 2>&1 > /dev/null
testExitCode "/bin/bash $SCRIPT -b $TMP_OUT/test_single_old.bam --nolink" "$EXIT_OK" "No link test" "$TMP_OUT/test_single_old.bai" "$TMP_OUT/test_single_old.bam.bai"
testExitCode "/bin/bash $SCRIPT -b $TMP_OUT/test_single_old.bam" "$EXIT_OK" "Single end old format conversion test" "$TMP_OUT/test_single_old.bai"
testExitCode "/bin/bash $SCRIPT -b $TMP_OUT/test_single_new.bam" "$EXIT_OK" "Single end new format conversion test" "$TMP_OUT/test_single_new.bai"
testExitCode "/bin/bash $SCRIPT -b $TMP_OUT/test_paired_old.bam" "$EXIT_OK" "Paired end old format conversion test" "$TMP_OUT/test_paired_old.bai"
testExitCode "/bin/bash $SCRIPT -b $TMP_OUT/test_paired_new.bam" "$EXIT_OK" "Paired end new format conversion test" "$TMP_OUT/test_paired_new.bai"

# delete all the temporary file
rm -f $TMP_OUT/test_single.bam 2>&1 > /dev/null
rm -f $TMP_OUT/test_single*.bai 2>&1 > /dev/null
rm -f $TMP_OUT/test_paired.bam 2>&1 > /dev/null
rm -f $TMP_OUT/test_paired*.bai 2>&1 > /dev/null

# return the number of failed tests
exit $FAILED_TESTS
