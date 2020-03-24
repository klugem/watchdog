WATCHDOG_CONDA_PATH_PLUGIN=$1

source "${WATCHDOG_CONDA_PATH_PLUGIN}/functions.unix.sh"

# ensure that a specific conda environment is installed and can be used
function conda_init() {
	PATH_TO_ENV=$1
	PATH_TO_YML=$2

	CONDA_READY_FILE=$(condaReadyFile "${PATH_TO_ENV}")

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
			conda env create --prefix "${PATH_TO_ENV}" --quiet --file "${PATH_TO_YML}"
			EXIT_CODE=$?

			# test if installation was ok
			if [ ${EXIT_CODE} -eq 0 ]; then
				echoInfo "Created conda environment form '${PATH_TO_YML}' into '${PATH_TO_ENV}'."
				touch "${CONDA_READY_FILE}"
			else 
				echoError "Failed with exit code '$EXIT_CODE' to create conda environment form '${PATH_TO_YML}' into '${PATH_TO_ENV}'."
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