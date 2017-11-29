#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh # include some basic functions
SCRIPT=$(readlink_own -m $SCRIPT_FOLDER/gzip.sh)
GLOBAL_TEST_DATA=$SCRIPT_FOLDER/../../test_data
TEST_DATA_FOLDER=$SCRIPT_FOLDER/test_data
FAILED_TESTS=0

TMP_OUT="/tmp"
cp -n $TEST_DATA_FOLDER/test.file $TMP_OUT/compressionTest.delete

# call with invalid parameter
testExitCode "/bin/bash $SCRIPT" "$EXIT_MISSING_ARGUMENTS" "Missing parameter test"
testExitCode "/bin/bash $SCRIPT -i $TEST_DATA_FOLDER/notExistingFile.super" "$EXIT_MISSING_INPUT_FILES" "Missing input file test"
testExitCode "/bin/bash $SCRIPT -i $TEST_DATA_FOLDER/test.file.notReadable" "$EXIT_MISSING_INPUT_FILES" "Not readable input file test"

# real calls
testExitCode "/bin/bash $SCRIPT -i $TEST_DATA_FOLDER/test.file -o $TMP_OUT/compressionTest1.file.gz" "$EXIT_OK" "Simple compression test" "$TMP_OUT/compressionTest1.file.gz"
testExitCode "/bin/bash $SCRIPT -i $TEST_DATA_FOLDER/test.file -o $TMP_OUT/compressionTest2.file.gz -v" "$EXIT_OK" "Verify compression test" "$TMP_OUT/compressionTest2.file.gz"
testExitCode "/bin/bash $SCRIPT -i $TEST_DATA_FOLDER/test.file -o $TMP_OUT/compressionTest3.file.gz -q 5" "$EXIT_OK" "Different quality test" "$TMP_OUT/compressionTest3.file.gz"
testExitCode "/bin/bash $SCRIPT -i $TEST_DATA_FOLDER/test.file -o $TMP_OUT/compressionTest4.file.gz -q 100" "$EXIT_OK" "Invalid quality test" "$TMP_OUT/compressionTest4.file.gz"
testExitCode "/bin/bash $SCRIPT -i $TMP_OUT/compressionTest.delete -d -o $TMP_OUT/compressionTest5.file.gz" "$EXIT_OK" "Delete compression test" "$TMP_OUT/compressionTest5.file.gz" "$TMP_OUT/compressionTest.delete"
testExitCode "/bin/bash $SCRIPT -i $TMP_OUT/compressionTest1.file.gz --decompress -o $TMP_OUT/compressionTest.txt" "$EXIT_OK" "Decompression test" "$TMP_OUT/compressionTest.txt" " " "$TEST_DATA_FOLDER/md5.txt"
testExitCode "/bin/bash $SCRIPT -i $TMP_OUT/compressionTest2.file.gz --decompress -d -o $TMP_OUT/compressionTest2.txt" "$EXIT_OK" "Decompression delete test" "$TMP_OUT/compressionTest.txt" "$TMP_OUT/compressionTest2.file.gz"
testExitCode "/bin/bash $SCRIPT -i $TMP_OUT/compressionTest1.file.gz --decompress --md5 $TEST_DATA_FOLDER/checksums.md5 -o $TMP_OUT/compressionTest.txt" "$EXIT_OK" "Decompression, verification test" "$TMP_OUT/compressionTest.txt" " " "$TEST_DATA_FOLDER/md5.txt"
testExitCode "/bin/bash $SCRIPT -i $TMP_OUT/compressionTest1.file.gz --decompress --md5 $TEST_DATA_FOLDER/checksums.md5 --oldPathMd5 /tmp/oldName.txt -o $TMP_OUT/compressionTest.txt" "$EXIT_OK" "Decompression, verification rename test" "$TMP_OUT/compressionTest.txt" " " "$TEST_DATA_FOLDER/md5.txt"

# delete all the temporary file
rm -f $TMP_OUT/compressionTest*.gz
rm -f $TMP_OUT/compressionTest.delete
rm -f $TMP_OUT/compressionTest.txt

# return the number of failed tests
exit $FAILED_TESTS
