#!/bin/bash
SCRIPT_FOLDER=$(cd $(dirname $(readlink -f "${BASH_SOURCE[0]}" 2>/dev/null || readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")) && pwd -P)
source "${SCRIPT_FOLDER}/../core_lib/includeBasics.sh"

# some constants
URL='https://github.com/watchdog-wms/watchdog-wms-modules/archive/master.zip'
GIT_NAME_IN_ARCHIVE="watchdog-wms-modules-master"
DOWNLOAD_FILE="tmp-modules.zip"
EXTRACT_DIR="/tmp/tmp_modules_github_tmp_modules"$(date +%s)

TARGET_DIR="${SCRIPT_FOLDER}"
MOVE_DIR="${TARGET_DIR}/.BAK/"
OK=0

if [ -d "${MOVE_DIR}" ]; then
	echo "Backup folder '${MOVE_DIR}' already exists. Please check if you need these modules any longer and delete the folder afterwards."
	exit 0
fi

confirm "Do you really want to move the content of '${TARGET_DIR}' to '${MOVE_DIR}' and download all modules stored in the community repository?"
if [ $CONFIRM_RETURN -eq 1 ]; then
	confirm "Are you sure?!"

	if [ $CONFIRM_RETURN -eq 1 ]; then
		mkdir -p "${MOVE_DIR}"
		for I in `ls -d ${TARGET_DIR}/*/`; do		
			mv -i "${I}" "${MOVE_DIR}"
		done
		echoInfo "Moved all modules into '${MOVE_DIR}'."
		OK=1
	fi
fi

# start with download
cd "${TARGET_DIR}"
if [ ${OK} -eq 1 ]; then
	wget -qO- -O "${DOWNLOAD_FILE}" "${URL}" 
	unzip -q -d "${EXTRACT_DIR}" "${DOWNLOAD_FILE}"
	rm "${DOWNLOAD_FILE}"
	mv "${EXTRACT_DIR}/${GIT_NAME_IN_ARCHIVE}/"* .
	rm -r "${EXTRACT_DIR}/"
fi

exit 0

