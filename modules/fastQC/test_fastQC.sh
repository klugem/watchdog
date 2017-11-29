#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh # include some basic functions
SCRIPT=$(readlink_own -m $SCRIPT_FOLDER/fastQC.sh)
GLOBAL_TEST_DATA=$SCRIPT_FOLDER/../../test_data
TEST_DATA_FOLDER=$SCRIPT_FOLDER/test_data
FAILED_TESTS=0

TMP_OUT="/tmp"

TEST_FILE_NEW="$GLOBAL_TEST_DATA/fastq/singleEnd_newFormat_101bp.fastq"
TEST_FILE_OLD="$GLOBAL_TEST_DATA/fastq/singleEnd_oldFormat_101bp.fastq"

# call with invalid parameter
testExitCode "/bin/bash $SCRIPT" "$EXIT_MISSING_ARGUMENTS" "Missing parameter test"
testExitCode "/bin/bash $SCRIPT -f $TEST_DATA_FOLDER/notExistingFile.super -o /tmp/fastQC1.test" "$EXIT_MISSING_INPUT_FILES" "Missing input file test" " " "/tmp/fastQC1.test/notExistingFile_fastqc.html"
testExitCode "/bin/bash $SCRIPT -f $GLOBAL_TEST_DATA/test.file.notReadable -o /tmp/fastQC2.test" "$EXIT_MISSING_INPUT_FILES" "Not readable input file test" " " "/tmp/fastQC2.test/test.file_fastqc.html"
testExitCode "/bin/bash $SCRIPT -f $TEST_FILE_OLD -o /" "$EXIT_WRITING_FAILED" "Not writeable output test" " " "/singleEnd_oldFormat_101bp_fastqc.html"
testExitCode "/bin/bash $SCRIPT -f $TEST_FILE_OLD -o $TMP_OUTcd /fastQC3.test -t -5" "$EXIT_INVALID_ARGUMENTS" "Invalid thread number test" " " "$TMP_OUT/fastQC3.test/singleEnd_oldFormat_101bp_fastqc.html"

# real calls
testExitCode "/bin/bash $SCRIPT -f $TEST_FILE_OLD -o $TMP_OUT/fastQC4.test" "$EXIT_OK" "Simple old format fastQC test" "$TMP_OUT/fastQC4.test/singleEnd_oldFormat_101bp_fastqc.html"
testExitCode "/bin/bash $SCRIPT -f $TEST_FILE_NEW -o $TMP_OUT/fastQC5.test" "$EXIT_OK" "Simple new format fastQC test" "$TMP_OUT/fastQC5.test/singleEnd_newFormat_101bp_fastqc.html"
testExitCode "/bin/bash $SCRIPT -f $TEST_FILE_OLD -o $TMP_OUT/fastQC6.test" "$EXIT_OK" "Thread fastQC test" "$TMP_OUT/fastQC6.test/singleEnd_oldFormat_101bp_fastqc.html"
testExitCode "/bin/bash $SCRIPT -f $GLOBAL_TEST_DATA/fastq/invalid.fastq -o $TMP_OUT/fastQC7.test" "$EXIT_FAILED" "Misformatted fastq test" " " "/tmp/fastQC7.test/singleEnd_oldFormat_101bp_fastqc.html"

# delete all the temporary file
rm -rf $TMP_OUT/fastQC*.test 2>&1 > /dev/null

# return the number of failed tests
exit $FAILED_TESTS
