#!/bin/bash
SCRIPT_FOLDER=$(cd $(dirname $(readlink -f "${BASH_SOURCE[0]}" 2>/dev/null || readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")) && pwd -P)
source "${SCRIPT_FOLDER}/core_lib/includeBasics.sh"

REQUIRED_JFX_MODULES=javafx.base,javafx.controls,javafx.graphics,javafx.fxml,javafx.controls
JAVA_FX_TEST_NAME='javafx.fxml.jar'
BASHRC_FILE="${HOME}/.bashrc"

# try to get javafx sdk path from environment variable
if [ "${JFX_SDK_LIB_PATH_ENV}" != "" ]; then
	JFX_SDK_LIB_PATH=${JFX_SDK_LIB_PATH_ENV}
fi

# manually modify javafx SDK path here
#JFX_SDK_LIB_PATH='some/other/path'

# try to locate the javafx SDK
if [ "${JFX_SDK_LIB_PATH}" == "" ]; then
	RET=$(locate ${JAVA_FX_TEST_NAME})
	CODE=$?
	if [ ${CODE} -eq 0 ]; then
		C=1
		SEL=()
		for I in $RET; do
			SEL[$C]=$(dirname $I)
			echo $C') '.$I
			C=$((C+1))
		done

		USE=$(getInputNumber "Please select the javafx SDK you want to use: ")
		if [ ${USE} -ge 1 ] && [ ${USE} -lt $C ]; then
			JFX_SDK_LIB_PATH=${SEL[${USE}]}
			echo -e "\nUsing '${JFX_SDK_LIB_PATH}' as path for the javafx SDK."

			# ask if user want's to add that to his bashrc config
			if [ -f ${BASHRC_FILE} ]; then
				confirm "Do you want to add the javafx SDK path to your bashrc config ('${BASHRC_FILE}')?"
				if [ $CONFIRM_RETURN -eq 1 ]; then
					echo "export JFX_SDK_LIB_PATH_ENV='$JFX_SDK_LIB_PATH'" >> ${BASHRC_FILE}
				fi
			fi
		else
			echoError "Please enter a number between 1 and ${C}."
			exit 1
		fi
	else
		echoError "Failed to locate javafx SDK. Please set the path to the javafx SDK manually using the 'JFX_SDK_LIB_PATH' variable."
		exit 1
	fi
fi

java -Djdk.gtk.version=2 --add-opens javafx.fxml/javafx.fxml=ALL-UNNAMED --add-opens javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED --module-path "${JFX_SDK_LIB_PATH}" --add-modules ${REQUIRED_JFX_MODULES} -jar "${SCRIPT_FOLDER}/jars/watchdogDesigner.jar" $@
exit $?
