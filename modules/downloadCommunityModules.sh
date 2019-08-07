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

# get MODE to work with
MODE=$(getInput "Do you want to download all (a) or only selected modules (s)? ")
case "$MODE" in
	s)
	MODE="s"
	;;
	a)
	MODE="a"
	;;
	*)
	echoError "Only 'a' or 's' is valid as input!"
	exit 1
	;;
esac

# move all modules
if [ "${MODE}" == "a" ]; then
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
else
	OK=1
fi

# start with download
echo "Downloading modules..."
cd "${TARGET_DIR}"
if [ ${OK} -eq 1 ]; then
	wget -qO- -O "${DOWNLOAD_FILE}" "${URL}" 
	unzip -q -d "${EXTRACT_DIR}" "${DOWNLOAD_FILE}"
	rm "${DOWNLOAD_FILE}"
	if [ "${MODE}" == "a" ]; then
		mv "${EXTRACT_DIR}/${GIT_NAME_IN_ARCHIVE}/"* .
	else 
		# let user decide which should be moved
		COUNT=1
		for M in `ls -d ${EXTRACT_DIR}/${GIT_NAME_IN_ARCHIVE}/*/`; do
			B=$(basename "${M}")
			EXIST="[NEW]"
			# test if folder exists
			if [ -d "${TARGET_DIR}/${B}" ]; then 
				EXIST="[EXISTS]"
			fi
			echo "${COUNT}: ${B} ${EXIST}"
			COUNT=${COUNT}+1
		done

		NUMBERS=$(getInput "Please enter the number of modules you want to install (format: 1,2,3-5): ")
		confirm "Do you really want to install modules with the numbers '${NUMBERS}' and move module duplicates in '${TARGET_DIR}' to '${MOVE_DIR}'?"
		mkdir -p "${MOVE_DIR}"

		if [ $CONFIRM_RETURN -eq 1 ]; then
			# get numbers
			IFS=',' read -r -a array <<< "${NUMBERS}"
			USE=()
			for N in "${array[@]}"; do
				if [[ "${N}" =~ ^[0-9]+-[0-9]+$ ]]; then
					IFS='-' read -ra ST_EN <<< "${N}"
					USE+=($(seq "${ST_EN[0]}" "${ST_EN[1]}"))
				else
					USE+=("${N}")
				fi
			done
			# sort array
			IFS=$'\n' USE=($(sort <<<"${USE[*]}"))
	
			# copy or move
			COUNT=1
			INDEX=0
			for M in `ls -d ${EXTRACT_DIR}/${GIT_NAME_IN_ARCHIVE}/*/`; do

				if [ ${USE[${INDEX}]} -eq $COUNT ] && [ $INDEX -lt ${#USE[@]} ]; then
					B=$(basename "${M}")
					# test if folder exists
					if [ -d "${TARGET_DIR}/${B}" ]; then 
						mv "${TARGET_DIR}/${B}" "${MOVE_DIR}/${B}"
						echoInfo "Moved '"${TARGET_DIR}/${B}"' into '${MOVE_DIR}${B}'."
					fi

					mv "${EXTRACT_DIR}/${GIT_NAME_IN_ARCHIVE}/${B}" "${TARGET_DIR}/"
					echoInfo "Installed ${B}..."
					INDEX=${INDEX}+1
				fi
				COUNT=${COUNT}+1
			done
		fi

	fi
	rm -r "${EXTRACT_DIR}/"
fi

exit 0

