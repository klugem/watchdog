#!/bin/bash
SCRIPT_FOLDER=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd -P)
java -jar "$SCRIPT_FOLDER/jars/WatchdogDesigner.jar"
exit $?
