#!/bin/bash
SCRIPT_FOLDER=$(cd $(dirname $(readlink -f "${BASH_SOURCE[0]}" 2>/dev/null || readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")) && pwd -P)
source "${SCRIPT_FOLDER}/../core_lib/includeBasics.sh"

# some constants
URL='https://github.com/watchdog-wms/watchdog-wms-modules/archive/master.tar.gz'
GIT_NAME_IN_ARCHIVE="watchdog-wms-modules-master"
DOWNLOAD_FILE="tmp-modules.zip"
EXTRACT_DIR="/tmp/tmp_modules_github_tmp_modules"$(date +%s)

TARGET_DIR="${SCRIPT_FOLDER}"
MOVE_DIR="${TARGET_DIR}/.BAK/"
OK=0
NOASK=0

if [ "$1" == "--auto" ]; then
	NOASK=1
fi

if [ -d "${MOVE_DIR}" ]; then
	echo "Backup folder '${MOVE_DIR}' already exists. Please check if you need these modules any longer and delete the folder afterwards."
	exit 0
fi

# get MODE to work with
if [ ${NOASK} -eq 0 ]; then
	MODE=$(getInput "Do you want to install all (a) or only selected modules (s)? Use any other input to abort.")
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
else
	MODE="a"
fi

# move all modules
if [ "${MODE}" == "a" ]; then
	if [ $(ls -d ${TARGET_DIR}/*/ 2> /dev/null | wc -l) -ne 0 ]; then
		confirm "Do you really want to move the content of '${TARGET_DIR}' to '${MOVE_DIR}' and install all modules stored in the community repository? [y/n]"
		if [ $CONFIRM_RETURN -eq 1 ]; then
			confirm "Are you sure?! [y/n]"

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
else
	OK=1
fi

# start with download
cd "${TARGET_DIR}"
if [ ${OK} -eq 1 ]; then
	echo "Downloading modules..."
	wget -qO- -O "${DOWNLOAD_FILE}" "${URL}" 
	mkdir "${EXTRACT_DIR}"
	tar -zxf "${DOWNLOAD_FILE}" -C "${EXTRACT_DIR}"
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
			COUNT=$((COUNT+1))
		done

		NUMBERS=$(getInput "Please enter the number of modules you want to install (format e.g. 1,2,3-5): ")
		NUMBERS=$(echo "${NUMBERS}" | sed 's/ //g')
		# get numbers
		IFS=',' read -r -a array <<< "${NUMBERS}"
		USE=()
		for N in "${array[@]}"; do
			if [[ "${N}" =~ ^[0-9]+-[0-9]+$ ]]; then
				IFS='-' read -ra ST_EN <<< "${N}"
				USE+=($(seq "${ST_EN[0]}" "${ST_EN[1]}"))
			else
				if [[ "${N}" =~ ^[0-9]+$ ]]; then
					USE+=("${N}")
				else	
					echoError "It seems that '${NUMBERS}' is no valid input in format e.g. 1,2,3-5."
					exit 1
				fi
			fi
		done

		# sort array
		IFS=$'\n' USE=($(sort <<<"${USE[*]}"))

		# colltect names of modules
		COUNT=1
		INDEX=0
		MOD_NAMES=""
		for M in `ls -d ${EXTRACT_DIR}/${GIT_NAME_IN_ARCHIVE}/*/`; do
			if [ $INDEX -lt ${#USE[@]} ] && [ ${USE[${INDEX}]} -eq $COUNT ] ; then
				B=$(basename "${M}")
				if [ ${INDEX} == 0 ]; then
					MOD_NAMES="'${B}"
				else
					MOD_NAMES="${MOD_NAMES}', '${B}"
				fi
				INDEX=$((INDEX+1))
			fi
			COUNT=$((COUNT+1))
		done
		if [ "${MOD_NAMES}" == "" ]; then
			echoError "It seems that '${NUMBERS}' is no valid input in format e.g. 1,2,3-5."
			exit 1
		fi
		MOD_NAMES="${MOD_NAMES}'"

		confirm "Do you really want to install modules ${MOD_NAMES} and move module duplicates in '${TARGET_DIR}' to '${MOVE_DIR}'? [y/n]"
		mkdir -p "${MOVE_DIR}"

		if [ $CONFIRM_RETURN -eq 1 ]; then
			# copy or move
			COUNT=1
			INDEX=0
			for M in `ls -d ${EXTRACT_DIR}/${GIT_NAME_IN_ARCHIVE}/*/`; do

				if [ $INDEX -lt ${#USE[@]} ] && [ ${USE[${INDEX}]} -eq $COUNT ]; then
					B=$(basename "${M}")
					# test if folder exists
					if [ -d "${TARGET_DIR}/${B}" ]; then 
						mv "${TARGET_DIR}/${B}" "${MOVE_DIR}/${B}"
						echoInfo "Moved '"${TARGET_DIR}/${B}"' into '${MOVE_DIR}${B}'."
					fi

					mv "${EXTRACT_DIR}/${GIT_NAME_IN_ARCHIVE}/${B}" "${TARGET_DIR}/"
					echoInfo "Installed ${B}..."
					INDEX=$((INDEX+1))
				fi
				COUNT=$((COUNT+1))
			done
		fi

	fi
	rm -r "${EXTRACT_DIR}/"
fi

exit 0

