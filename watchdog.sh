#!/bin/bash
SCRIPT_FOLDER=$(cd $(dirname $(readlink -f "${BASH_SOURCE[0]}" 2>/dev/null || readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")) && pwd -P)
source "$SCRIPT_FOLDER/core_lib/includeBasics.sh"
SLEEP_TIME="60s"

# capture STRG+C signals
trap 'handle_int' SIGINT

# auto-configure examples if environment variable WATCHDOG_HOME is set and WATCHDOG_AUTO_CONFIG is set to 1
if [ "$WATCHDOG_AUTO_CONFIG" == "1" ] && [ ! -z "$WATCHDOG_HOME" ]; then
	if [ -d "$WATCHDOG_HOME/examples" ] && [ ! -f "$WATCHDOG_HOME/examples/example_basic_sleep.xml" ]; then
		(bash -c "$WATCHDOG_HOME/helper_scripts/configureExamples.sh -i '$WATCHDOG_HOME' 2>&1 > /dev/null") 2>&1 > /dev/null
	fi
fi

# check if watchdog should run in start&stop mode 
AUTO_DETACH_COUNT=$(echo "$@" | grep -c "\-autoDetach")
ATTACH_INFO_COUNT=$(echo "$@" | grep -c "\-attachInfo")
if [ $AUTO_DETACH_COUNT -eq 1 ]; then
	# generate tmp location for status information 
	if [ $ATTACH_INFO_COUNT -eq 0 ]; then
		ATTACH_FILE=$(getTmpFile watchdog.attach)
		APPEND_PARAM="-attachInfo \"$ATTACH_FILE\""

		# ensure that file is deleted on exit
		deleteFileOnExit $ATTACH_FILE
	fi

	RET=123
	while [ $RET -eq 123 ]; do
		java -jar $SCRIPT_FOLDER/jars/watchdog.jar $@ $APPEND_PARAM &
		BACKGROUND_PID=$!
		waitForCommandToFinish ${BACKGROUND_PID}
		RET=${BACKGROUND_EXIT_CODE}

		if [ $RET -eq 123 ]; then
			# wait some time until watchdog is re-attached
			echo "------------------"
			echo "sleeping for $SLEEP_TIME"
			echo "------------------"
			sleep "$SLEEP_TIME"
		fi
	done
	exit $RET
else
	java -jar "$SCRIPT_FOLDER/jars/watchdog.jar" &
	BACKGROUND_PID=$!
	waitForCommandToFinish ${BACKGROUND_PID}
	exit $BACKGROUND_EXIT_CODE
fi
exit $?
