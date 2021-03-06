#!/bin/bash
SCRIPT_FOLDER=$(cd $(dirname $(readlink -f "${BASH_SOURCE[0]}" 2>/dev/null || readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")) && pwd -P)
source "${SCRIPT_FOLDER}/includeBasics.sh"

REQUIRED_JFX_MODULES=javafx.base,javafx.controls,javafx.graphics,javafx.fxml
JAVA_FX_TEST_NAME='javafx.fxml.jar'
JAVA_FX_TEST_NAME_VAR='javafx[.-]fxml*.jar'
BASHRC_FILE="${HOME}/.bashrc"

function locateFile() {
	WHERE=$1
	WHAT=$2

	RET=$(find "${WHERE}" -maxdepth 1 -name "${WHAT}" 2> /dev/null | wc -l)
	if [ $? -eq 0 ] && [ $RET -gt 0 ]; then
		echo 1
	else
		echo 0
	fi
}

# get the name path to the jar to launch
if [ $# -eq 0 ]; then
	echoExit "Name of the jar that should be started must be first parameter."
else
	JAR2START=$(cd "${SCRIPT_FOLDER}/../jars" && pwd -P)"/$1"
	shift #remove first argument

	# test if jar exists
	if [ ! -e "${JAR2START}" ]; then
		echoError "Jar '${JAR2START}' does not exist."
		exit 1
	fi
	if [ ! -r "${JAR2START}" ]; then
		echoError "Jar '${JAR2START}' is not readable."
		exit 1
	fi
fi

# try to get javafx sdk path from environment variable
if [ "${JFX_SDK_LIB_PATH_ENV}" != "" ]; then
	JFX_SDK_LIB_PATH=${JFX_SDK_LIB_PATH_ENV}
fi

# if not set try to use version obtained with Maven
if [ "${JFX_SDK_LIB_PATH}" == "" ] && [ -e "${SCRIPT_FOLDER}/../jars/libs/modules/" ]; then
	
	# try to find version in base folder (self-compiled by maven script)
	RET=$(locateFile "${SCRIPT_FOLDER}/../jars/libs/modules/" "${JAVA_FX_TEST_NAME_VAR}")
	if [ "$RET" == "1" ]; then
		JFX_SDK_LIB_PATH="${SCRIPT_FOLDER}/../jars/libs/modules/"
	else # try to detect downloaded version from git repo
		# detect OS 
		if [[ "$OSTYPE" == "darwin"* ]]; then
			OS_VERSION="mac"
		else
			OS_VERSION="linux" 
		fi

		# try to find test file
		if [ -d "${SCRIPT_FOLDER}/../jars/libs/modules/${OS_VERSION}" ]; then
			RET=$(locateFile "${SCRIPT_FOLDER}/../jars/libs/modules/${OS_VERSION}" "${JAVA_FX_TEST_NAME_VAR}")
			if [ "$RET" == "1" ]; then
				JFX_SDK_LIB_PATH="${SCRIPT_FOLDER}/../jars/libs/modules/${OS_VERSION}/"
			fi
		fi
	fi
fi

# manually modify javafx SDK path here
#JFX_SDK_LIB_PATH='some/other/path'

# try to locate the javafx SDK
if [ "${JFX_SDK_LIB_PATH}" == "" ]; then
	echo "Path to javafx SDK is not set via 'JFX_SDK_LIB_PATH_ENV' environment variable or within this script."
	confirm "Do you want to search for it in ${CONDA_PREFIX}/usr, ${CONDA_PREFIX}/share and ${CONDA_PREFIX}/lib? (y / n)"
	if [ $CONFIRM_RETURN -ne 1 ]; then
		exit 1
	fi

	# use locate if available
	LOCATE_OK=0
	echoInfo "Searching for javafx SDK using locate."
	RET=$(locate "${JAVA_FX_TEST_NAME}" 2> /dev/null)
	if [ $? -eq 0 ] && [ ! -z "$RET" ]; then
		LOCATE_OK=1
	fi
	
	# use find
	if [ $LOCATE_OK -eq 0 ]; then
		echoInfo "Searching for javafx SDK using find; this might take a while..."
		RET=$(find "${CONDA_PREFIX}/usr" "${CONDA_PREFIX}/share" "${CONDA_PREFIX}/lib" -name "${JAVA_FX_TEST_NAME}" 2> /dev/null)
	fi

	# prepare results
	C=1
	SEL=()
	for I in $RET; do
		SEL[$C]=$(dirname $I)
		echo $C') '$I
		C=$((C+1))
	done

	if [ ${C} -eq 1 ]; then
		echoError "Failed to locate javafx SDK. Please set the path to the javafx SDK manually using the 'JFX_SDK_LIB_PATH_ENV' environment variable."
		exit 1
	fi

	USE=$(getInputNumber "Please select the javafx SDK you want to use: ")
	if [ ${USE} -ge 1 ] && [ ${USE} -lt $C ]; then
		JFX_SDK_LIB_PATH=${SEL[${USE}]}
		echo -e "\nUsing '${JFX_SDK_LIB_PATH}' as path for the javafx SDK."

		# ask if user want's to add that to his bashrc config
		if [ -f ${BASHRC_FILE} ]; then
			confirm "Do you want to add the javafx SDK path to your bashrc config ('${BASHRC_FILE}')? (y / n)"
			if [ $CONFIRM_RETURN -eq 1 ]; then
				echo "export JFX_SDK_LIB_PATH_ENV='$JFX_SDK_LIB_PATH'" >> ${BASHRC_FILE}
			fi
		fi
	else
		echoError "Please enter a number between 1 and ${C}."
		exit 1
	fi
fi

# ensure that it could be a valid javafx sdk install dir
JFX_SDK_LIB_PATH=$(abspath "${JFX_SDK_LIB_PATH}")
RET=$(locateFile "${JFX_SDK_LIB_PATH}" "${JAVA_FX_TEST_NAME_VAR}")
if [ "$RET" == "0" ]; then
	echoError "'${JFX_SDK_LIB_PATH}' seems to be no valid javafx SDK installation folder. "
	
	confirm "Do you want to use it anyhow? (y / n)"
	if [ $CONFIRM_RETURN -ne 1 ]; then
		exit 1
	fi
fi

# try to start the GUI
echoInfo "Using javafx SDK in '$JFX_SDK_LIB_PATH'... "
java -Djdk.gtk.version=2 --add-opens javafx.fxml/javafx.fxml=ALL-UNNAMED --add-opens javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED --module-path "${JFX_SDK_LIB_PATH}" --add-modules "${REQUIRED_JFX_MODULES}" -jar "${JAR2START}" $@
exit $?

