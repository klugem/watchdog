#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh # include some basic functions
SCRIPT=$(readlink_own -m $SCRIPT_FOLDER/{%MODULE%}.sh)
GLOBAL_TEST_DATA=$SCRIPT_FOLDER/../../test_data
TEST_DATA_FOLDER=$SCRIPT_FOLDER/test_data
FAILED_TESTS=0

TMP_OUT="/tmp"

# call with invalid parameter
testExitCode "/bin/bash $SCRIPT" "$EXIT_MISSING_ARGUMENTS" "Missing parameter test"
testExitCode "/bin/bash $SCRIPT --no-valid-parameter X" "$EXIT_INVALID_ARGUMENTS" "Invalid parameter test"
testExitCode "/bin/bash $SCRIPT -i $TEST_DATA_FOLDER/notExistingFile.super" "$EXIT_MISSING_INPUT_FILES" "Missing input file test"
testExitCode "/bin/bash $SCRIPT -i $GLOBAL_TEST_DATA/test.file.notReadable" "$EXIT_MISSING_INPUT_FILES" "Not readable input file test"
# TODO: add/modify invalid calls

# real calls
#testExitCode "/bin/bash $SCRIPT -i $TEST_DATA_FOLDER/test.file" "$EXIT_OK" "Simple compression test" "$TMP_OUT/{%MODULE%}1.test"
# TODO: add real calls

# delete all the temporary file
# TODO: needed ?
#rm -f $TMP_OUT/{%MODULE%}*.test 2>&1 > /dev/null

# return the number of failed tests
exit $FAILED_TESTS
