LIB_SCRIPT_FOLDER=$(cd $(dirname $(readlink -f "${BASH_SOURCE[0]}" 2>/dev/null || readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")) && pwd -P)
# try to find an alternative for md5sum when not there (on mac)
# fixes sed inline mac vs unix --> sedinline
PATH="$PATH:$LIB_SCRIPT_FOLDER/wrapper"
export PATH

source $LIB_SCRIPT_FOLDER/functions.sh
WATCHDOG_TMP_DIR="$LIB_SCRIPT_FOLDER/../tmp"
CAPTURE_THIS=$(createOutputFolder "$WATCHDOG_TMP_DIR/dummy.file") # ensure that the tmp folder is there
WATCHDOG_HOSTNAME_CLUSTER_FILE="$WATCHDOG_TMP_DIR/.jobID2hostname.txt"
LOCK_TIMEOUT=15
MODULE_VERSION_IS_SET=0

if [ ! -z "${MODULE_VERSION_PARAMETER_NAME}" ]; then
	MODULE_VERSION=$(getSetModuleVersion "$@" "${MODULE_VERSION_PARAMETER_NAME}")
	MODULE_VERSION_IS_SET=1
	if [ "${IS_WATCHDOG_JOB}" == "1" ]; then
		echo "[Module version]: ${MODULE_VERSION}"
	fi
else
	# set default value
	MODULE_VERSION=1
fi

# get the last command
trap 'command_prev="${command_curr}" ; command_curr=$(echo "${BASH_COMMAND}")' DEBUG

# check, who is the caller and update the caller history
if [ "$_" != "$0" ]; then
	CALLER=$(basename "${BASH_SOURCE[${#BASH_SOURCE[@]} - 1]}")

	if [ -z "$PRIMARY_CALLER" ]; then
		PRIMARY_CALLER="$CALLER"
		export PRIMARY_CALLER

		# check, if job is running on cluster and if write ID to a file
		if [ ! -z ${JOB_ID} ] && [ "${IS_WATCHDOG_JOB}" == "1" ]; then
			if [ ! -e "$WATCHDOG_HOSTNAME_CLUSTER_FILE" ]; then
				touch "$WATCHDOG_HOSTNAME_CLUSTER_FILE"
			fi
			wait4Lock "$WATCHDOG_HOSTNAME_CLUSTER_FILE" $LOCK_TIMEOUT
			lockFile "$WATCHDOG_HOSTNAME_CLUSTER_FILE"
			echo -e "${JOB_ID}\t"$(hostname) >> "$WATCHDOG_HOSTNAME_CLUSTER_FILE"
			unlockFile "$WATCHDOG_HOSTNAME_CLUSTER_FILE"
		fi
		if [ "$PRIMARY_CALLER" != "watchdog.sh" ]; then
			echo -n "[Called command]: ${BASH_SOURCE[${#BASH_SOURCE[@]} - 1]} "
			if [ $MODULE_VERSION_IS_SET -eq 1 ]; then
				echo $(removeModuleVersionParamsAndPrint "$@" "${MODULE_VERSION_PARAMETER_NAME}")
			else 
				echo $(printParams "$@")
			fi
		fi
	else
		PRIMARY_CALLER="$PRIMARY_CALLER->$CALLER"
		export PRIMARY_CALLER
	fi
fi

# check if all needed files are there
INCLUDE="exitCodes.sh:functions.sh:external/shflags.sh"
# read input arguments into array
IFS=':' read -a INCLUDE <<< "$INCLUDE"
unset IFS

FAILED=0
# check each of the tools
for I in "${INCLUDE[@]}"
do
	if [ ! -e "$LIB_SCRIPT_FOLDER/$I" ]; then
		echo "[ERROR] Could not find file '$I' in '$LIB_SCRIPT_FOLDER' to source. ($PRIMARY_CALLER)"
		FAILED=1
	else
		source "$LIB_SCRIPT_FOLDER/$I"
	fi
done

# check if all include files were found
if [ $FAILED -eq 1 ]; then
	exit $EXIT_FAILED_INCLUDES
fi
