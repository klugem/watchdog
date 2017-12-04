#!/bin/bash
SCRIPT_FOLDER=$(cd $(dirname $(realpath "${BASH_SOURCE[0]}")) && pwd)
java -jar "$SCRIPT_FOLDER/jars/watchdog.jar" $@
exit $?
