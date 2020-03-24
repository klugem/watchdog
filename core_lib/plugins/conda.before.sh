source "${WATCHDOG_CONDA_PATH_PLUGIN}/conda.functions.sh" "${WATCHDOG_CONDA_PATH_PLUGIN}"

# activate conda base
conda_activate_base "${WATCHDOG_CONDA_PATH_TO_BIN}"

conda_init "${WATCHDOG_CONDA_PATH_TO_ENV}" "${WATCHDOG_CONDA_PATH_TO_YML}"
		
# activate the environment 
conda_activate "${WATCHDOG_CONDA_PATH_TO_ENV}"
