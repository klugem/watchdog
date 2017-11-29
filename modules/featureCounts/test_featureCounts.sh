#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../../core_lib/includeBasics.sh # include some basic functions
SCRIPT=$(readlink_own -m $SCRIPT_FOLDER/featureCounts.sh)
GLOBAL_TEST_DATA=$SCRIPT_FOLDER/../../test_data
TEST_DATA_FOLDER=$SCRIPT_FOLDER/test_data
FAILED_TESTS=0

TMP="/tmp"

TEST_S_NEW="$GLOBAL_TEST_DATA/fastq/joined_bam/test_single_new"
TEST_S_OLD="$GLOBAL_TEST_DATA/fastq/joined_bam/test_single_old"
TEST_P_NEW="$GLOBAL_TEST_DATA/fastq/joined_bam/test_paired_new"
TEST_P_OLD="$GLOBAL_TEST_DATA/fastq/joined_bam/test_paired_old"

cp $TEST_S_NEW.bai $TMP/.
cp $TEST_S_OLD.bai $TMP/.
cp $TEST_P_NEW.bai $TMP/.
cp $TEST_P_OLD.bai $TMP/.
cp $TEST_S_NEW.bam $TMP/.
cp $TEST_S_OLD.bam $TMP/.
cp $TEST_P_NEW.bam $TMP/.
cp $TEST_P_OLD.bam $TMP/.

DEFAULT_TEST="$TMP/test_single_old.bam"

# call with invalid parameter
testExitCode "/bin/bash $SCRIPT" "$EXIT_MISSING_ARGUMENTS" "Missing parameter test"
testExitCode "/bin/bash $SCRIPT -i $TEST_DATA_FOLDER/notExistingFile.super -a $TEST_DATA_FOLDER/notExistingFile.super -o $TMP/featureCounts1.test" "$EXIT_MISSING_INPUT_FILES" "Missing input file test" " " "$TMP/featureCounts1.test"
testExitCode "/bin/bash $SCRIPT -i $GLOBAL_TEST_DATA/test.file.notReadable -a $TEST_DATA_FOLDER/notExistingFile.super -o $TMP/featureCounts1.test" "$EXIT_MISSING_INPUT_FILES" "Read-only input file test" " " "$TMP/featureCounts1.test"
testExitCode "/bin/bash $SCRIPT -i $DEFAULT_TEST -a $TEST_DATA_FOLDER/test.saf.withRandomEnding -o $TMP/featureCounts1.test" "$EXIT_MISSING_ARGUMENTS" "Missing annotation type test" " " "$TMP/featureCounts1.test"
testExitCode "/bin/bash $SCRIPT -i $DEFAULT_TEST -a $TEST_DATA_FOLDER/test.saf.withRandomEnding -o $TMP/featureCounts1.test --annotationType lol" "$EXIT_MISSING_ARGUMENTS" "Invalid annotation type test" " " "$TMP/featureCounts1.test"

# real calls
testExitCode "/bin/bash $SCRIPT -i $DEFAULT_TEST -a $TEST_DATA_FOLDER/test.saf -o $TMP/featureCounts2.test" "$EXIT_OK" "Automatic SAF count test" "$TMP/featureCounts2.test"
testExitCode "/bin/bash $SCRIPT -i $DEFAULT_TEST -a $TEST_DATA_FOLDER/test.gtf -o $TMP/featureCounts3.test" "$EXIT_OK" "Automatic GTF count test" "$TMP/featureCounts3.test"
testExitCode "/bin/bash $SCRIPT -i $DEFAULT_TEST -a $TEST_DATA_FOLDER/test.saf.withRandomEnding -o $TMP/featureCounts4.test --annotationType SAF" "$EXIT_OK" "SAF random ending count test" "$TMP/featureCounts4.test"

testExitCode "/bin/bash $SCRIPT -i $TMP/test_single_old.bam -a $TEST_DATA_FOLDER/test.gtf -o $TMP/featureCounts5.test" "$EXIT_OK" "Single end old format test" "$TMP/featureCounts5.test"
testExitCode "/bin/bash $SCRIPT -i $TMP/test_single_new.bam -a $TEST_DATA_FOLDER/test.gtf -o $TMP/featureCounts6.test" "$EXIT_OK" "Single end new format test" "$TMP/featureCounts6.test"
testExitCode "/bin/bash $SCRIPT --countFragments -i $TMP/test_paired_old.bam -a $TEST_DATA_FOLDER/test.gtf -o $TMP/featureCounts7.test" "$EXIT_OK" "Paired end old format test" "$TMP/featureCounts7.test"
testExitCode "/bin/bash $SCRIPT --countFragments -i $TMP/test_paired_new.bam -a $TEST_DATA_FOLDER/test.gtf -o $TMP/featureCounts8.test" "$EXIT_OK" "Paired end new format test" "$TMP/featureCounts8.test"

# delete all the temporary file
rm -f $TMP/featureCounts*.test 2>&1
rm -f $TMP/test_single* 2>&1
rm -f $TMP/test_paired* 2>&1

# return the number of failed tests
exit $FAILED_TESTS
