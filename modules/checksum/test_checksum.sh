#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh # include some basic functions
SCRIPT=$(readlink_own -m $SCRIPT_FOLDER/checksum.sh)
GLOBAL_TEST_DATA=$SCRIPT_FOLDER/../../test_data
TEST_DATA_FOLDER=$SCRIPT_FOLDER/test_data
FAILED_TESTS=0

TMP_OUT="/tmp"
cp $TEST_DATA_FOLDER/testFile.txt $TMP_OUT/testFile.txt

# call with invalid parameter
testExitCode "/bin/bash $SCRIPT" "$EXIT_MISSING_ARGUMENTS" "Missing parameter test"
testExitCode "/bin/bash $SCRIPT -i $TEST_DATA_FOLDER/notExistingFile.super" "$EXIT_MISSING_INPUT_FILES" "Missing input file test"
testExitCode "/bin/bash $SCRIPT -i $GLOBAL_TEST_DATA/test.file.notReadable" "$EXIT_MISSING_INPUT_FILES" "Read-only input file test"

# real calls
testExitCode "/bin/bash $SCRIPT -i $TEST_DATA_FOLDER/testFile.txt -c $TMP_OUT/checksumTest1.test" "$EXIT_OK" "Simple checksum test" "$TMP_OUT/checksumTest1.test"
testExitCode "/bin/bash $SCRIPT -i $TEST_DATA_FOLDER/testFile.txt -c $TMP_OUT/checksumTest1.test" "$EXIT_INVALID_ARGUMENTS" "Duplicate checksum test"
testExitCode "/bin/bash $SCRIPT -i $TEST_DATA_FOLDER/testFile.txt -c $TMP_OUT/checksumTest1.test -u" "$EXIT_OK" "Update checksum test" "$TMP_OUT/checksumTest1.test"
testExitCode "/bin/bash $SCRIPT -i $TEST_DATA_FOLDER/testFile.txt -c $TMP_OUT/checksumTest2.test -a" "$EXIT_OK" "Absolute name checksum test" "$TMP_OUT/checksumTest2.test"
testExitCode "/bin/bash $SCRIPT -i $TEST_DATA_FOLDER/testFile.txt -c $TMP_OUT/checksumTest1.test -v" "$EXIT_OK" "Verify checksum test"
testExitCode "/bin/bash $SCRIPT -i $TEST_DATA_FOLDER/testFile.txt -c $TMP_OUT/checksumTest1.test -v" "$EXIT_OK" "Verify checksum rename test"
testExitCode "/bin/bash $SCRIPT -i $TMP_OUT/testFile.txt -c $TMP_OUT/checksumTest1.test -v --ignorePath" "$EXIT_OK" "Verify checksum ignore path test"

# delete all the temporary file
rm -f $TMP_OUT/checksumTest*.test

# return the number of failed tests
exit $FAILED_TESTS
