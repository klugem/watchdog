#!/bin/bash
SCRIPT_FOLDER=$(cd $(dirname $(readlink -f "${BASH_SOURCE[0]}" 2>/dev/null || readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")) && pwd -P)
source "${SCRIPT_FOLDER}/../core_lib/includeBasics.sh"

# some constants
URL='https://github.com/watchdog-wms/moduleMaker/archive/master.tar.gz'
GIT_NAME_IN_ARCHIVE="moduleMaker-master"
DOWNLOAD_FILE="tmp-moduleMaker"$(date +%s)".tar.gz"
EXTRACT_DIR="/tmp/tmp_moduleMaker_github_tmp_moduleMaker"$(date +%s)
TARGET_DIR=$(readlink -f "${SCRIPT_FOLDER}/../moduleMaker")

# check if folder does not exist so far
if [ -d "${TARGET_DIR}" ]; then
	echoError "Installation folder '${TARGET_DIR}' already exists!"
	exit 1
fi

# let the user confirm the download
if [ "$1" == "--auto" ]; then
	CONFIRM_RETURN=1
else
	confirm "Do you really want to download the ModuleMaker and install it in '${TARGET_DIR}'? [y/n]"
fi

# start with installation process
if [ $CONFIRM_RETURN -eq 1 ]; then

	# create target folder
	mkdir  "${TARGET_DIR}"
	if [ ! -d "${TARGET_DIR}" ]; then
		echoError "Failed to create installation folder '${TARGET_DIR}'!"
		exit 1
	fi

	# start with download
	echoInfo "Downloading ModuleMaker..."
	downloadFile "${URL}" "${DOWNLOAD_FILE}"
	if [ $? -eq 0 ]; then
		mkdir "${EXTRACT_DIR}"

		# extract & move
		echoInfo "Extracting ModuleMaker..."
		DOWNLOAD_FILE_BASE=$(basename "${DOWNLOAD_FILE}" ".gz")
		gzip -fkd "${DOWNLOAD_FILE}" > "${DOWNLOAD_FILE_BASE}"
		tar -xf "${DOWNLOAD_FILE_BASE}" -C "${EXTRACT_DIR}"

		mv "${EXTRACT_DIR}/${GIT_NAME_IN_ARCHIVE}/distribute/"* "${TARGET_DIR}/."
		echoInfo "Installed ModuleMaker!"

		# clean up
		rm "${DOWNLOAD_FILE}" "${DOWNLOAD_FILE_BASE}"
		rm -r "${EXTRACT_DIR}/"
	fi
fi

# end of script
exit 0

