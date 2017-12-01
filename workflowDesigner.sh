#!/bin/bash
SCRIPT_FOLDER=$(cd $(dirname $(readlink -f "${BASH_SOURCE[0]}")) && pwd)
java -jar "$SCRIPT_FOLDER/jars/WatchdogDesigner.jar"
exit $?
