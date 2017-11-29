#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source "$SCRIPT_FOLDER/core_lib/includeBasics.sh"
PID=0
TMP_FILE_W1=""
TMP_FILE_W2=""

# check, if used tools are installed
USED_TOOLS='echo:grep:java:rm:kill:tail:touch'
MESSAGE=$("$LIB_SCRIPT_FOLDER/checkUsedTools.sh" "$USED_TOOLS")
CODE=$?

if [ $CODE -ne 0 ]; then
	echoError "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi

unset PRIMARY_CALLER # or otherwiese getting of hostnames will not work!
trap "onExit" EXIT

onExit() {
	if [ $PID -ne 0 ]; then
		$(kill -13 $PID > /dev/null 2>&1)
		$(kill -13 $(<"$TMP_FILE_W2") > /dev/null 2>&1)
	fi
	if [ "$TMP_FILE_W1" != "" ]; then
		$(rm -f "$TMP_FILE_W1" > /dev/null 2>&1)
	fi
	if [ "$TMP_FILE_W2" != "" ]; then
		$(rm -f "$TMP_FILE_W2" > /dev/null 2>&1)
	fi
}

TMP_FILE_W1=$(getTmpFile "watchdog")
TMP_FILE_W2=$(getTmpFile "watchdog")
touch "$TMP_FILE_W1"
( tail -F "$TMP_FILE_W1" & echo $! > "$TMP_FILE_W2" ) | grep -v "error: Invalid sge_strtok_r call, last is not NULL" &
PID=$!
java -jar "$SCRIPT_FOLDER/jars/watchdog.jar" $@ > "$TMP_FILE_W1" 2>&1
exit $?
