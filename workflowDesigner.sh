#!/bin/bash
SCRIPT_FOLDER=$(cd $(dirname $(readlink -f "${BASH_SOURCE[0]}" 2>/dev/null || readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")) && pwd -P)

REQUIRED_JFX_MODULES=javafx.base,javafx.controls,javafx.graphics,javafx.fxml,javafx.controls
JFX_SDK_LIB_PATH="/usr/lib64/javafx-sdk-11.0.1/lib/"

java -Djdk.gtk.version=2 --add-opens javafx.fxml/javafx.fxml=ALL-UNNAMED --add-opens javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED --module-path "$JFX_SDK_LIB_PATH" --add-modules $REQUIRED_JFX_MODULES -jar "$SCRIPT_FOLDER/jars/watchdogDesigner.jar" $@
exit $?
