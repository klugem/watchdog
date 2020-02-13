#!/bin/bash
SCRIPT_FOLDER=$(cd $(dirname $(readlink -f "${BASH_SOURCE[0]}" 2>/dev/null || readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")) && pwd -P)
source "${SCRIPT_FOLDER}/../core_lib/includeBasics.sh"

cd "${SCRIPT_FOLDER}"
if [ ! -e "watchdog-SNAPSHOT.jar" ]; then
	echo "File 'watchdog-SNAPSHOT.jar' does not exist. Call mvn package first to build it!"
	exit 1
fi

REQUIRED_JFX_MODULES=javafx.base,javafx.controls,javafx.graphics,javafx.fxml,javafx.controls

NAMES[0]="watchdog.jar"
NAMES[1]="watchdogDesigner.jar"
NAMES[2]="refBookGenerator.jar"
NAMES[3]="docuTemplateExtractor.jar"
NAMES[4]="moduleValidator.jar"
NAMES[5]="workflowValidator.jar"
NAMES[6]="reportGenerator.jar"

MAINS[0]="de.lmu.ifi.bio.watchdog.runner.XMLBasedWatchdogRunner"
MAINS[1]="de.lmu.ifi.bio.watchdog.GUI.WorkflowDesignerRunner"
MAINS[2]="de.lmu.ifi.bio.watchdog.runner.ModuleLibraryCreatorRunner"
MAINS[3]="de.lmu.ifi.bio.watchdog.runner.ModuleDocuExtractorRunner"
MAINS[4]="de.lmu.ifi.bio.watchdog.validator.github.ModuleGithubCheckerRunner"
MAINS[5]="de.lmu.ifi.bio.watchdog.validator.github.WorkflowGithubCheckerRunner"
MAINS[6]="de.lmu.ifi.bio.watchdog.runner.MethodTemplateGeneratorRunner"

echo "Please select the JAR you want to launch:"
I=1
for N in "${NAMES[@]}"; do
	echo "${I}) ${N}"
	I=$((I+1))
done

# get the main method that should be started
	USE=$(getInputNumber "Waiting for your input... ")
	if [ ${USE} -ge 1 ] && [ ${USE} -lt ${I} ]; then
		MAIN=${MAINS[((USE-1))]}
	else
		echo "Please enter a number between 1 and 7."
		exit 1
	fi

echo "Launching...${NAMES[((USE-1))]}"
sleep 3
# start the main method

java --module-path libs/modules -cp libs/*:watchdog-SNAPSHOT.jar --add-modules "${REQUIRED_JFX_MODULES}" -Djdk.gtk.version=2 --add-opens javafx.fxml/javafx.fxml=ALL-UNNAMED "${MAIN}" $@
