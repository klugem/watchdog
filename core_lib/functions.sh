#!/bin/bash
# add because later it will be checked if used tools are installed when checkUsedTools is called
USED_TOOLS_FUNCTIONS='echo:head:tr:md5sum:touch:mkdir:mktemp:cut:printf:tee:sync'
MESSAGE_SIZE=100

echoInfo() {
	if [ ! -z "$1" ]; then
		printf "[INFO] $1 [$PRIMARY_CALLER]\n"
	fi
}

echoWarn() {
	if [ ! -z "$1" ]; then
		printf "[WARN] $1 [$PRIMARY_CALLER]\n"
	fi
}

echoError() {
	if [ ! -z "$1" ]; then
		printf "[ERROR] $1 [$PRIMARY_CALLER]\n" 1>&2
	fi
}

echoAInfo() {
	if [ ! -z "$1" ]; then
		printf "$1\n"
	fi
}

echoAWarn() {
	if [ ! -z "$1" ]; then
		printf "$1\n"
	fi
}

echoAError() {
	if [ ! -z "$1" ]; then
		printf "$1\n" 1>&2
	fi
}

checkExitCode() {
	EXIT_CODE_CHECK=$?
	if [ $EXIT_CODE_CHECK -ne 0 ]; then
		FAILED_COMMAND=$(eval echo "$command_prev")
		echoError "Last command failed: '$FAILED_COMMAND' in line $1"
		exit $EXIT_FAILED
	fi
}

testExitCode() {
	COMMAND=$1
	EXPECTED_CODE=$2
	TEST_MESSAGE=$3

	# reset vars
	TEST_FILE=""
	MISSING_TEST_FILE=""
	MD5_TEST=""

	if [ "$#" -ge "4" ]; then
		TEST_FILE=$4
		if [ "$TEST_FILE" == " " ]; then
			TEST_FILE="/bin/cp" # look only for missing files
		fi
	fi

	if [ "$#" -ge "5" ]; then
		MISSING_TEST_FILE=$5
	fi

	if [ "$#" -eq "6" ]; then
		MD5_TEST=$6
	fi

	# execute command
	RET=$($COMMAND 2>&1)
	CODE=$?

	# test if command resulted in expected output
	if [ "$CODE" == "$EXPECTED_CODE" ]; then
		if [ ! -z $TEST_FILE ]; then
			if [ -e $TEST_FILE ]; then
				# test md5 hash if it is set
				if [ ! -z $MD5_TEST ]; then
					
					RET=$(diff <(grep "$TEST_FILE" "$MD5_TEST" | cut -d " " -f 1) <(md5sum "$TEST_FILE" | cut -d " " -f 1) 2>&1)
					EXIT_CODE=$?
					if [ $EXIT_CODE -ne 0 ]; then
						echo "[FAILED] $TEST_MESSAGE: MD5 checksum of '$TEST_FILE' does not represent the same checksum."
						echoAError "error message: '$RET'; return code: '$CODE'";
						FAILED_TESTS=$(($FAILED_TESTS+1))
					fi
				fi				

				# test if file is missing
				if [ ! -z $MISSING_TEST_FILE ]; then
					if [ ! -e $MISSING_TEST_FILE ]; then
						echo "[OK] $TEST_MESSAGE";
					else
						echo "[FAILED] $TEST_MESSAGE: Test file '$MISSING_TEST_FILE' is NOT missing!"
						echoAError "error message: '$RET'; return code: '$CODE'";
						FAILED_TESTS=$(($FAILED_TESTS+1))
					fi
				else
					echo "[OK] $TEST_MESSAGE";
				fi
			else	
				echo "[FAILED] $TEST_MESSAGE: Test file '$TEST_FILE' is missing!"
				echoAError "error message: '$RET'; return code: '$CODE'";
				FAILED_TESTS=$(($FAILED_TESTS+1))
			fi
		else
			echo "[OK] $TEST_MESSAGE";
		fi
	else
		echo "[FAILED] $TEST_MESSAGE";
		echoAError "error message: '$RET'; return code: '$CODE'";
		FAILED_TESTS=$(($FAILED_TESTS+1))
	fi
}

repeatSingleChar() {
	REPEAT_STRING=$(head -c $1 < /dev/zero | tr '\0' "$2")
}

printParamValues() {

	if [ "$FLAGS_debug" == "0" ]; then
		
		# print header
		if [ "$#" -eq 1 ]; then
			START_HEADER=" [$1] "
		else
			START_HEADER=""
		fi
		SIZE_DIFF=$MESSAGE_SIZE-${#START_HEADER}
		SIZE=$((($SIZE_DIFF) / 2))
		if [ $((SIZE_DIFF % 2)) -eq 1 ]; then
			SIZE=$(($SIZE+1))
		fi
		repeatSingleChar $SIZE -
		START_MESSAGE=${REPEAT_STRING}${START_HEADER}${REPEAT_STRING}
		echo $START_MESSAGE

		# print parameters
		IFS=' ' read -a FF <<< "$__flags_longNames"
		unset IFS
		for F in ${FF[@]}; do
			PARAM_NAME='FLAGS_'$F
			echo "parameter: --$F ('"${!PARAM_NAME}"')"
		done

		# print footer
		repeatSingleChar $MESSAGE_SIZE -
		echo $REPEAT_STRING
	fi
}

confirm() {
	REPLY=""
	while [[ ! $REPLY =~ ^[YyNn]$ ]]; do
		read -p "$1 " -n 1 -r
		echo " "
	done
	
	if [[ ! $REPLY =~ ^[Yy]$ ]]; then
		CONFIRM_RETURN=0
	else
		CONFIRM_RETURN=1
	fi
}

getInput() {
	read -p "$1" -e
	echo $REPLY
}


getInputNumber() {
	REPLY=""
	while [[ ! $REPLY =~ ^-?[0-9]+(\.[0-9]+)?$ ]]; do
		REPLY=$(getInput "$1")
	done
	echo $REPLY
}

getInputName() {
	REPLY=" "
	while [[ $REPLY =~ ^.*[\ ]+.*$ ]] || [ "$REPLY" == "" ] ; do
		REPLY=$(getInput "$1")
	done
	echo $REPLY
}


replaceInFile() {
	if [ $# -eq 3 ]; then
		TMP_FILE_NAME_AWK="/tmp/awk_replace_${RANDOM}"
		# escape it for awk
		ESC=${2//\/\\/}
		ESC=$(printf "%b" "$ESC" | sed 's/\\/\\\\/g' | sed 's/\\"/"/g' | sed '/^$/d' | sed 's/&/\\\\&/g')

		# in case of macOS we need this line extra as no multilines are supported in BSD version of awk for -v
		if [[ $OSTYPE == darwin* ]]; then
			COUNT_TEST=$(awk --version 2>/dev/null | grep "(GNU Awk)" -c)
			if [ "$?" -ne "0" ] || [ "$COUNT_TEST" -eq "0" ]; then
				ESC=${ESC//$'\n'/\\n}
			fi
		fi

		# remove double new lines
		awk -v var1="$1" -v var2="$ESC" '{
		  gsub(var1,var2)
		  print
		}' $3 > $TMP_FILE_NAME_AWK
		mv $TMP_FILE_NAME_AWK $3
	else
		echo "Too less arguments for replace..."
	fi
}

createOutputFolder() {
	# create output folder
	OUT_BASE=$(dirname $1)
	if [ ! -d "$OUT_BASE" ]; then
		mkdir -p "$OUT_BASE"

		CODE=$?
		if [ $CODE -ne 0 ]; then
			echoError "Could not create output folder '$OUT_BASE'."
			exit $EXIT_WRITING_FAILED;
		fi
	fi
	echo "$OUT_BASE"
}

createOutputFile() {
	FILE=$1
	# create the output folder, if it is not there
	createOutputFolder "$FILE"
	touch "$FILE"
	CODE=$?
	if [ $CODE -ne 0 ]; then
		echoError "Could not create output file '$FILE'."
		exit $EXIT_WRITING_FAILED;
	fi
}

verifyFileExistence() {
	FILE=$1
	if [ ! -e "$FILE" ]; then
		echoError "Input file '$FILE' does not exist."
		exit $EXIT_MISSING_INPUT_FILES
	fi
	if [ ! -r "$FILE" ]; then
		echoError "Input file '$FILE' is not readable for user '$(whoami)'."
		exit $EXIT_MISSING_INPUT_FILES
	fi
	# test, if the file can be executed
	if [ $# -eq 2 ]; then
		if [ ! -x "$1" ]; then
			echoError "File '$FILE' can not be executed by user '$(whoami)'."
			exit $EXIT_MISSING_INPUT_FILES
		fi
	fi
}

verifyFolderExistence() {
	FOLDER=$1
	if [ ! -d "$FOLDER" ]; then
		echoError "Input folder '$FOLDER' does not exist."
		exit $EXIT_MISSING_INPUT_FOLDER
	fi
	if [ ! -r "$FOLDER" ]; then
		echoError "Input folder '$FOLDER' is not readable for user '$(whoami)'."
		exit $EXIT_MISSING_INPUT_FOLDER
	fi
	# test, if the folder can be accessed
	if [ $# -eq 2 ]; then
		if [ ! -x "$1" ]; then
			echoError "Folder '$FOLDER' can not be entered by user '$(whoami)'."
			exit $EXIT_MISSING_INPUT_FOLDER
		fi
	fi
}

getDefaultFolderForTmpFiles() {
	BASE="/usr/local/storage"
	# check if folder exists
	if [ ! -d "$BASE" ] || [ ! -w "$BASE" ]; then
		BASE="/tmp"
	fi
	echo "$BASE"
}

getTmpFile() {
	TOOL=$1
	FIRST=1
	TMP_OUT_CREATE=""

	if [ $# -eq 2 ]; then
		BASE=$2
	else
		BASE=$(getDefaultFolderForTmpFiles)
	fi

	while [ -e $TMP_OUT_CREATE ] || [ $FIRST -eq 1 ]; do
		TMP_OUT_CREATE=$(mktemp -u $BASE/${TOOL}.tmp.XXXXXX)
		FIRST=0
	done
	echo $TMP_OUT_CREATE
}

ensureLowerBound() {
	VALUE=$1
	BOUND=$2

	if [ ! -z "$VALUE" ]; then
		if [ $VALUE -lt $BOUND ]; then
			echo $BOUND
		else 
			echo $VALUE
		fi
	fi
}

function abspath() {
    if [ -d "$1" ]; then
        echo "$(cd "$1"; pwd)"
    elif [ -f "$1" ]; then
        if [[ $1 == */* ]]; then
            echo "$(cd "${1%/*}"; pwd)/${1##*/}"
        else
            echo "$(pwd)/$1"
        fi
    fi
}

function min() {
	if [ $# -eq 2 ]; then
		if [ $1 -le $2 ]; then
			echo $1
		else
			echo $2
		fi	
	fi
}

function max() {
	if [ $# -eq 2 ]; then
		if [ $2 -le $1 ]; then
			echo $1
		else
			echo $2
		fi	
	fi
}

# try to find an argument that h
function getSetModuleVersion() {
	RET=1 # default version
	NAME_TO_FIND=${@: -1}
	# shift until we find it
	while [ "$1" != "$NAME_TO_FIND" ]; do
		shift
	done
	# test, if last element was it
	if [ "$1" == "$NAME_TO_FIND" ]; then
		shift
		# check, if we have some more or if
		if [ "$1" != "$NAME_TO_FIND" ] && [ ! -z "$1" ]; then
			RET=$1
		fi
	fi
	# output the version
	echo $RET
}

# removes the module version parameter
function removeModuleVersionParamsAndPrint() {
	NAME_TO_REMOVE=${@: -1}
	PARAMS=()
	i=0
	SKIP=0
	for param in "${@}"; do
		if [ "$param" != "$NAME_TO_REMOVE" ] && [ $SKIP -ne 1 ]; then
			PARAMS[$i]="$param"
			i=$(($i+1))
		else
			if [ $SKIP -eq 1 ]; then
				SKIP=0
			else
				SKIP=1
			fi
		fi
	done
	# output it
	printf "\"%s\" " "${PARAMS[@]}"
}

# first parameter: number of cores
# second parameter: memory in MB per core
# does override these settings, if executed on the GRID
function getMemoryForJava() {
	if [ $# -eq 2 ]; then
		# check, if core and memory information is set
		if [ ! -z $IS_WATCHDOG_JOB ] && [ $IS_WATCHDOG_JOB -eq 1 ]; then
			# override user settings for threads with the number of set cores
			if [ ! -z $WATCHDOG_CORES ] && [ $WATCHDOG_CORES -ge 1 ]; then
				FLAGS_thread=$WATCHDOG_CORES
			fi
			# get the maximal amount of memory to use --> memory is already cores * memory done by java!
			if [ ! -z $WATCHDOG_MEMORY ] && [ $WATCHDOG_MEMORY -ge 1 ]; then
				MAX_MEMORY=$WATCHDOG_MEMORY
			fi

		fi

		# try to get some own values --> maximal half of the machines memory and 3GB per core
		if [ -z $MAX_MEMORY ]; then
			# try to find how much memory the machine has
			if [ -f /proc/meminfo ]; then
				MEM=$(grep MemTotal /proc/meminfo | awk '{print $2}')
				MEM=$((MEM/1024))
				MAX_MEMORY=$((MEM*100/47)) # take maximal less than the half of the machines memory
				# take 3 GB per core as default
				NEEDED_MEM=$(($1*$2))
				# get the minimum used stuff
				MAX_MEMORY=$(min $MAX_MEMORY $NEEDED_MEM)
				MAX_MEMORY=$(max 4096 $MAX_MEMORY)
			fi
		fi	

		# reserve one third of the max memory fixed but at least 2GB
		MIN_MEMORY=$((MAX_MEMORY/3))
		MIN_MEMORY=$(max 2048 $MIN_MEMORY)
		MAX_MEMORY=$(max $MAX_MEMORY $MIN_MEMORY)

		echo "-Xms${MIN_MEMORY}m -Xmx${MAX_MEMORY}m"
	fi
}

function escape4Sed() {
	IN=$1
	IN=${IN//$'\n'/\\$'\n'} # newlines
	IN=${IN//\//\\\/} # slashes
	IN=${IN//\"/\\\"} # "
	IN=${IN//\&/\\\&} # &
	echo -e "$IN"
}


function escape4HTML() {
	IN=$1
	IN=$(echo -e "$IN" | sed -e 's|<|\\\&lt;|g' -e 's|>|\\\&gt;|g')
	echo -e "$IN"
}

function getLockFilename() {
	FILE=$1
	LOCKNAME=$(echo "$FILE" | md5sum | cut -f 1 -d " ")
	echo $LOCKNAME
}

function lockFile() {
	LOCK_FILE=$(getLockFilename $1)
	# ensure that file is delete when script fails befor the file could be deleted
	trap "rm -f '$LOCK_FILE' 2>&1 > /dev/null" EXIT
	touch "$LOCK_FILE"
	sync
}

function deleteFileOnExit() {
	FILE=$1
	# ensure that file is delete when script fails befor the file could be deleted
	trap "rm -f '$1' 2>&1 > /dev/null" EXIT
}

function deleteFolderOnExit() {
	FOLDER=$1
	# ensure that folder is delete when script fails befor the file could be deleted
	trap "rm -rf '$1' 2>&1 > /dev/null" EXIT
}

function unlockFile() {
	LOCK_FILE=$(getLockFilename $1)
	rm -f "$LOCK_FILE" 2>&1 > /dev/null
}

function wait4Lock() {
	LOCK_FILE=$(getLockFilename $1)
	LOCK_TIMEOUT=$2
	# block while lock file is there or timeout
	I=0
	while [ -e "$LOCK_FILE" ] && [ $I -le $LOCK_TIMEOUT ]; do
		sleep 1
		I=$(($I+1))
	done
}

# ensure that the output folder is already there
function transfer() {
	SOURCE=$1
	DEST=$2
	ADD=$3
	MESSAGE=$(rsync --compress $ADD "$SOURCE" "$DEST" 2>&1)
	CODE=$?
	if [ $CODE -ne 0 ]; then
		rm -rf "$DEST" 2>&1 > /dev/null
		echoError "Failed to transfer '$SOURCE' to '$DEST'.  See error of rsync below";
		echoAError "error code: '$CODE'"
		# output the original message
		printf "$MESSAGE\n"
		exit $EXIT_FAILED
	fi
}

# writes a return parameter to a file
function writeParam2File() {
	if [ "$FLAGS_returnFilePath" != "" ]; then
		FILE=$1
		PARAM=$2
		VALUE=$3

		# ensure that the folder is there
		O=$(createOutputFolder "$FILE")

		# test if file is there 
		if [ ! -e "$FILE" ]; then
			touch "$FILE"
			chmod ugo-r "$FILE"
		fi
		MES=$(echo -e "$PARAM\t$VALUE" | tee -a "$FILE" 2>&1)
		if [ $? -ne 0 ]; then
			echoError "Could not write output parameter '"$PARAM\t$VALUE"' to '$FILE'."
			echoAError "Error message: $MES"
		fi
	fi
}

function blockUntilFileIsWritten() {
	FILE=$1
	if [ "$FLAGS_returnFilePath" != "" ]; then
		# block while lock file is there or timeout
		I=0
		while [ ! -e "$FILE" ] && [ $I -le 60 ]; do
			sleep 1s
			I=$(($I+1))
		done
		echo "?EOF!" | tee -a "$FILE" > /dev/null
		# give another 1s time to ensure that the writing process was completed
		sync
		chmod ugo+r "$FILE"
		sleep 1s
	fi
}

function executeCommand() {
	COMMAND=$1
	LOGFILE=$2
	NAME=$3
	MESSAGE=$(eval "$COMMAND" 2>&1)
	CODE=$?
	echo -e "$MESSAGE" > "$LOGFILE"

	# check exit code
	if [ $CODE -ne 0 ]; then
		echoError "$NAME run failed with exit code '$CODE'.!"
		echoAError "Output: $MESSAGE"
		echoAError "Log file ($LOGFILE):" 
		cat $LOGFILE 
		exit $EXIT_FAILED
	else
		#check, if we can find an default error message in the return code
		MESSAGE_ERROR=$($SCRIPT_FOLDER/../../core_lib/errorChecker.sh "$LOGFILE" 2>&1)
		CODE_ERROR=$?

		if [ $CODE_ERROR -ne 0 ]; then
			echoError "Error checker found some errors, see found errors below"
			echoError "$MESSAGE_ERROR"
			echoAError "Original output: $MESSAGE"
			exit $EXIT_FAILED
		fi
	fi
}

function verifyRun2End() {
	FILE=$1

	if [ ! -e "$FILE" ]; then
		echoError "Run to end test failed. Script might be aborded before."
		exit $EXIT_FAILED
	else
		rm "$FILE"
	fi
}

