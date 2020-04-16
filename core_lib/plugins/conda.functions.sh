WATCHDOG_CONDA_PATH_PLUGIN=$1

source "${WATCHDOG_CONDA_PATH_PLUGIN}/functions.unix.sh"

# ensure that a specific conda environment is installed and can be used
function conda_init() {
	PATH_TO_ENV=$1
	PATH_TO_YML=$2

	# ensure that folder exists
	if [ ! -d "${PATH_TO_ENV}" ]; then
		mkdir -p "${PATH_TO_ENV}"
		if [ ! -d "${PATH_TO_ENV}" ]; then
			echoError "Failed to create environment folder '${PATH_TO_ENV}'."
			exit 16
		fi
	fi

	CONDA_READY_FILE=$(condaReadyFile "${PATH_TO_ENV}")

	# get verbose param
	if [ $# -eq 3 ]; then
		VERBOSE_VALUE=$3
	else
		VERBOSE_VALUE="--quiet"
	fi

	# test if environment exists
	if [ ! -e "${CONDA_READY_FILE}" ]; then

		# we don't want multiple processes to install conda simultaneously
		wait4Lock "${PATH_TO_ENV}" "1800" # give a running conda installation time to complete
		lockFile "${PATH_TO_ENV}"

		# test if another instance finished the installation in the mean time
		if [ ! -e "${CONDA_READY_FILE}" ]; then

			# delete folder if it exists
			if [ -d "${PATH_TO_ENV}" ]; then
				rm -rf "${PATH_TO_ENV}" 2>&1 > /dev/null
			fi

			echoInfo "Creating new conda environment '${PATH_TO_ENV}'..."
			# try to install the environment
			conda env create --prefix "${PATH_TO_ENV}" "${VERBOSE_VALUE}" --file "${PATH_TO_YML}"
			EXIT_CODE=$?

			# test if installation was ok
			if [ ${EXIT_CODE} -eq 0 ]; then
				echoInfo "Created conda environment form '${PATH_TO_YML}' into '${PATH_TO_ENV}'."
				touch "${CONDA_READY_FILE}"
				sync
			else 
				echoError "Failed with exit code '$EXIT_CODE' to create conda environment form '${PATH_TO_YML}' into '${PATH_TO_ENV}'."
				rm -rf  "${PATH_TO_ENV}"
				exit 12
			fi

		fi
				
		# release the lock file
		unlockFile "${PATH_TO_ENV}"
	fi
}

# activate conda base environment
function conda_activate_base() {
	PATH_TO_BIN=$1

	# test if conda binary exists
	if [ ! -d "${PATH_TO_BIN}" ] || [ ! -r "${PATH_TO_BIN}" ] || [ ! -x "${PATH_TO_BIN}" ]; then
		echoError "Path to conda '${PATH_TO_BIN}' does not exist or is not accessible."
		exit 21
	fi
	CONDA_BIN="${PATH_TO_BIN}/conda"
	if [ ! -e "${CONDA_BIN}" ] || [ ! -r "${CONDA_BIN}" ] || [ ! -x "${CONDA_BIN}" ]; then
		echoError "Conda binary '${CONDA_BIN}' does not exist or is not accessible."
		exit 21
	fi

	# activate the base environment
	source "${PATH_TO_BIN}/activate" base
}

# activate a conda environment given that the base environment is already active
function conda_activate() {
	PATH_TO_ENV=$1	
	conda activate "${PATH_TO_ENV}"
}

# deactivate all conda environments
function conda_deactivate() {
	while [ ! -z "${CONDA_PREFIX}" ]; do
		conda deactivate
	done
}
