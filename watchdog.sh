#!/bin/bash
SCRIPT_FOLDER=$(cd $(dirname $(readlink -f "${BASH_SOURCE[0]}" 2>/dev/null || readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")) && pwd -P)

# autoconfigure examples if environment variable WATCHDOG_HOME is set and WATCHDOG_AUTO_CONFIG is set to 1
if [ "$WATCHDOG_AUTO_CONFIG" == "1" ] && [ ! -z "$WATCHDOG_HOME" ]; then
	if [ -d "$WATCHDOG_HOME/examples" ] && [ ! -f "$WATCHDOG_HOME/examples/example_basic_sleep.xml" ]; then
		(bash -c "$WATCHDOG_HOME/helper_scripts/configureExamples.sh -i '$WATCHDOG_HOME' 2>&1 > /dev/null") 2>&1 > /dev/null
	fi
fi

java -jar "$SCRIPT_FOLDER/jars/watchdog.jar" $@
exit $?
