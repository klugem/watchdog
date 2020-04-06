#!/bin/bash
SCRIPT_FOLDER=$(cd $(dirname $(readlink -f "${BASH_SOURCE[0]}" 2>/dev/null || readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")) && pwd -P)
source $SCRIPT_FOLDER/../core_lib/includeBasics.sh
source $SCRIPT_FOLDER/../core_lib/includeBasics.sh

# check, if used tools are installed
USED_TOOLS='echo:sha256sum:cut'
MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS" "check_shflag_tools")
CODE=$?

if [ $CODE -ne 0 ]; then
	echoInfo "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi

# define parameters
DEFINE_string 'modules' '' 'path to module folder to search *.conda.yml files' 'm'
DEFINE_string 'path2conda' '' 'path to conda installation' 'c'
DEFINE_string 'path2environments' '' 'path where conda environments should be installed' 'e'
DEFINE_boolean 'verbose' 'false' '[optional] print detailed messages from conda' ''
DEFINE_boolean 'debug' 'false' '[optional] prints out debug messages' ''

# parse parameters
FLAGS "$@" || exit $EXIT_INVALID_ARGUMENTS
eval set -- "${FLAGS_ARGV}"
printParamValues "initial parameters" # print param values, if in debug mode

# check if mandatory arguments are there
if [ -z "$FLAGS_modules" ]; then
	echoError "Parameter -m must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_path2conda" ]; then
	echoError "Parameter -c must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi
if [ -z "$FLAGS_path2environments" ]; then
	echoError "Parameter -e must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi

# test, if conda bin is ther
if [ ! -e "$FLAGS_path2conda/bin/conda" ]; then
	echoError "No valid conda installation was found in '$FLAGS_path2conda'.";
	exit $EXIT_INVALID_ARGUMENTS
fi

if [ $FLAGS_verbose -eq 0 ]; then
	VERBOSE_VALUE=""
else
	VERBOSE_VALUE="--quiet"
fi
################ start with execution here ################
confirm "Do you really want to install all conda environments into '$FLAGS_path2environments'? [y/n]"
# start with installation process
if [ $CONFIRM_RETURN -eq 1 ]; then
	WATCHDOG_CONDA_PATH_PLUGIN="$SCRIPT_FOLDER/../core_lib/plugins/"
	if [ -e "$WATCHDOG_CONDA_PATH_PLUGIN/conda.functions.sh" ]; then
		source "$WATCHDOG_CONDA_PATH_PLUGIN/conda.functions.sh" "${WATCHDOG_CONDA_PATH_PLUGIN}"

		# create output folder
		if [ ! -d "$FLAGS_path2environments/" ]; then
			createOutputFolder "$FLAGS_path2environments/.dummy"
		fi

		# activate conda base
		conda_activate_base "$FLAGS_path2conda/bin"
	
		# try to init all conda environments
		for I in `find . "$FLAGS_modules" -maxdepth 2 -name "*.conda.yml" -type f | sort`; do
			echo "Process module '$I'...";
			HASH=$(sha256sum "$I" | cut -f 1 -d " ")
			conda_init "$FLAGS_path2environments/$HASH" "$I" "$VERBOSE_VALUE"
		done

		# clean up
		conda_deactivate
		exit $EXIT_OK
	else 
		echoError "Failed to source conda plugin at '$WATCHDOG_CONDA_PATH_PLUGIN/conda.functions.sh'"
		exit $EXIT_INVALID_ARGUMENTS
	fi
fi
