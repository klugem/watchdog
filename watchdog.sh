#!/bin/bash
SCRIPT_FOLDER=$(cd $(dirname $(readlink -f "${BASH_SOURCE[0]}" 2>/dev/null || readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")) && pwd -P)
source "$SCRIPT_FOLDER/core_lib/includeBasics.sh"
SLEEP_TIME="60s"

# auto-configure examples if environment variable WATCHDOG_HOME is set and WATCHDOG_AUTO_CONFIG is set to 1
if [ "$WATCHDOG_AUTO_CONFIG" == "1" ] && [ ! -z "$WATCHDOG_HOME" ]; then
	if [ -d "$WATCHDOG_HOME/examples" ] && [ ! -f "$WATCHDOG_HOME/examples/example_basic_sleep.xml" ]; then
		(bash -c "$WATCHDOG_HOME/helper_scripts/configureExamples.sh -i '$WATCHDOG_HOME' 2>&1 > /dev/null") 2>&1 > /dev/null
	fi
fi

# check if watchdog should run in start&stop mode 
if [ $(echo "$@" | grep -c "stopWheneverPossible") -eq 1 ]; then
	# generate tmp location for status information 
	SESSION_INFO_FILE=$(getTmpFile watchdog.stopWheneverPossible.info)
	# ensure that file is deleted on exit
	deleteFileOnExit $SESSION_INFO_FILE

	java -jar $SCRIPT_FOLDER/jars/watchdog.jar $@ -restartInfo "$SESSION_INFO_FILE"
	RET=$?
	while [ $RET -eq 1 ]; do
		echo "------------------"
		echo "sleeping for $SLEEP_TIME"
		echo "------------------"
		sleep "$SLEEP_TIME"
		java -jar $SCRIPT_FOLDER/jars/watchdog.jar $@ -restartInfo "$SESSION_INFO_FILE"
		RET=$?
	done
else
	java -jar "$SCRIPT_FOLDER/jars/watchdog.jar" $@
fi
exit $?
