#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh # include some basic functions
SCRIPT=$(readlink_own -m $SCRIPT_FOLDER/mergeFeatureCounts.sh)
GLOBAL_TEST_DATA=$SCRIPT_FOLDER/../../test_data
TEST_DATA_FOLDER=$SCRIPT_FOLDER/test_data
FAILED_TESTS=0

TMP_OUT="/tmp"

# call with invalid parameter
testExitCode "/bin/bash $SCRIPT" "$EXIT_MISSING_ARGUMENTS" "Missing parameter test"
testExitCode "/bin/bash $SCRIPT -s $TEST_DATA_FOLDER/notExistingFolder -o $TMP_OUT/mergeFeatureCounts1.test" "$EXIT_MISSING_INPUT_FOLDER" "Missing input folder test" " " "$TMP_OUT/mergeFeatureCounts1.test"
testExitCode "/bin/bash $SCRIPT -s $TEST_DATA_FOLDER/emptyFolder -o $TMP_OUT/mergeFeatureCounts2.test" "$EXIT_MISSING_INPUT_FILES" "Empty input folder test" " " "$TMP_OUT/mergeFeatureCounts2.test"

# real calls
testExitCode "/bin/bash $SCRIPT -s $TEST_DATA_FOLDER/counts -o $TMP_OUT/mergeFeatureCounts3.test" "$EXIT_OK" "Simple merge test" "$TMP_OUT/mergeFeatureCounts3.test"
testExitCode "/bin/bash $SCRIPT -s $TEST_DATA_FOLDER/unequalFeatures -o $TMP_OUT/mergeFeatureCounts4.test" "$EXIT_MISFORMATED_INPUT" "No equal feature sets test" " " "$TMP_OUT/mergeFeatureCounts4.test"

# delete all the temporary file
rm -f $TMP_OUT/mergeFeatureCounts*.test 2>&1 > /dev/null

# return the number of failed tests
exit $FAILED_TESTS
