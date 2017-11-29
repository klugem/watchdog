#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh # include some basic functions
SCRIPT=$(readlink_own -m $SCRIPT_FOLDER/grep.sh)
GLOBAL_TEST_DATA=$SCRIPT_FOLDER/../../test_data
TEST_DATA_FOLDER=$SCRIPT_FOLDER/test_data
FAILED_TESTS=0

# include some basic functions
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh
TMP_OUT="/tmp"

# call with invalid parameter
testExitCode "/bin/bash $SCRIPT" "$EXIT_MISSING_ARGUMENTS" "Missing parameter test"
testExitCode "/bin/bash $SCRIPT -f $TEST_DATA_FOLDER/notExistingFile.super -p x -o /dev/null" "$EXIT_MISSING_INPUT_FILES" "Missing input file test"
testExitCode "/bin/bash $SCRIPT -f $GLOBAL_TEST_DATA/test.file.notReadable -p x -o /dev/null" "$EXIT_MISSING_INPUT_FILES" "Not readable input file test"

# real calls
testExitCode "/bin/bash $SCRIPT -f $GLOBAL_TEST_DATA/annotation/test.bed -p 'ENST00000423796' -o $TMP_OUT/grep1.test" "$EXIT_OK" "Simple search test" "$TMP_OUT/grep1.test" " " "$TEST_DATA_FOLDER/grep1.checksum"
testExitCode "/bin/bash $SCRIPT -f $GLOBAL_TEST_DATA/annotation/test.bed -p 'ENST00000[0-9]+' -o $TMP_OUT/grep2.test --options '-E'" "$EXIT_OK" "Perl pattern search test" "$TMP_OUT/grep2.test" " " "$TEST_DATA_FOLDER/grep2.checksum"

# delete all the temporary file
rm -f $TMP_OUT/grep*.test 2>&1 > /dev/null

# return the number of failed tests
exit $FAILED_TESTS
