#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh # include some basic functions
SCRIPT=$(readlink_own -m $SCRIPT_FOLDER/leon.sh)
GLOBAL_TEST_DATA=$SCRIPT_FOLDER/../../test_data
TEST_DATA_FOLDER=$SCRIPT_FOLDER/test_data
FAILED_TESTS=0

TMP_OUT="/tmp"

# call with invalid parameter
testExitCode "/bin/bash $SCRIPT" "$EXIT_MISSING_ARGUMENTS" "Missing parameter test"
testExitCode "/bin/bash $SCRIPT -i $TEST_DATA_FOLDER/notExistingFile.fastq -o $TMP_OUT/leon1/" "$EXIT_MISSING_INPUT_FILES" "Missing input file test"

# real calls
testExitCode "/bin/bash $SCRIPT -i $GLOBAL_TEST_DATA/fastq/pairedEnd_newFormat_101bp_1.fastq -o $TMP_OUT/leon2/" "$EXIT_OK" "Simple compression test" "$TMP_OUT/leon2/pairedEnd_newFormat_101bp_1.leon.tar"
testExitCode "/bin/bash $SCRIPT -i $TMP_OUT/leon2/pairedEnd_newFormat_101bp_1.leon.tar -o $TMP_OUT/leon3/" "$EXIT_OK" "Simple decompression test" "$TMP_OUT/leon3/pairedEnd_newFormat_101bp_1.fastq"

# test if both checksums are equal
S=$(md5sum -b "$GLOBAL_TEST_DATA/fastq/pairedEnd_newFormat_101bp_1.fastq" | cut -f 1 -d " ")
D=$(md5sum -b "$TMP_OUT/leon3/pairedEnd_newFormat_101bp_1.fastq" | cut -f 1 -d " ")
if [ "$S" != "$D" ]; then
	echo "[FAILED] Checksums of original and decompressed files are not equal"
else
	echo "[OK] Checksums of original and decompressed files are equal"
fi

# delete all the temporary file
rm -rf $TMP_OUT/leon*.test 2>&1 > /dev/null

# return the number of failed tests
exit $FAILED_TESTS
