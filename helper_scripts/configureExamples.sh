#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/../core_lib/includeBasics.sh

# check, if used tools are installed
USED_TOOLS='tr:echo:sed'
MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS" "check_shflag_tools")
CODE=$?

if [ $CODE -ne 0 ]; then
	echoInfo "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi

# define parameters
DEFINE_string 'install' '' 'install directory of watchdog' 'i'
DEFINE_string 'mail' '' '[optional] mail for notification' 'm'
DEFINE_boolean 'fastTest' 'false' '[optional] replaces all wait parameters of sleep tasks with 1 second.' 'f'
DEFINE_boolean 'debug' 'false' '[optional] prints out debug messages.' ''

# parse parameters
FLAGS "$@" || exit $EXIT_INVALID_ARGUMENTS
eval set -- "${FLAGS_ARGV}"
printParamValues "initial parameters" # print param values, if in debug mode

# check if mandatory arguments are there
if [ -z "$FLAGS_install" ]; then
	echoError "Parameter -i must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi

# test, if base dir is a valid watchdog install dir
if [ ! -f "$FLAGS_install/xsd/watchdog.xsd" ] || [ ! -d "$FLAGS_install/examples/templates" ]; then
	echoError "No valid watchdog installation was found in '$FLAGS_install'.";
	exit $EXIT_INVALID_ARGUMENTS
fi

# ensure that after example data no / occours and that after install dir one is given!
FLAGS_install=$(echo "$FLAGS_install" | sed -E 's|/+$||')
FLAGS_install="${FLAGS_install}/"

# start replacing the values
cd $FLAGS_install/examples/templates

for F in `ls *.xml 2>/dev/null`; 
do
	FILE_NEW="$FLAGS_install/examples/$F"
	FILE_NEW=$(echo "$FILE_NEW" | sed -E 's|//|/|g')
	# copy the plain file
	cp "$FLAGS_install/examples/templates/$F" "$FILE_NEW"

	# replace the values
	for PARAM in $__flags_longNames; 
	do
		# ignore debug and help parameter
		if [ "$PARAM" == "debug" ] || [ "$PARAM" == "help" ]; then
			continue;
		fi
		# get the needed values
		V_NAME='FLAGS_'"$PARAM"
		VALUE=${!V_NAME}
		PARAM=$(echo $PARAM | tr '[:lower:]' '[:upper:]')
		PARAM="{%$PARAM%}"

		# replace it using sed
		sedinline 's#'${PARAM}'#'${VALUE}'#g' "$FILE_NEW"
		sedinline -E 's|[[:space:]]*/>| />|g' "$FILE_NEW"
	done
	# replace the example dir!
	PATH_I=$(echo "${FLAGS_install}/examples/test_data/simple#g" | sed -E 's|//|/|g') 
	sedinline 's#{%EXAMPLE_DATA%}#'$PATH_I "$FILE_NEW"
	sedinline -E 's#isTemplate="[(true)|1]+"##' "$FILE_NEW"
	if [ $FLAGS_fastTest -eq 0 ]; then
		sedinline -E 's#<wait>[0-9]+.*</wait>#<wait>1s</wait>#' "$FILE_NEW"
		sedinline -E 's#>[0-9]+s</#>1s</#' "$FILE_NEW"
	fi

	# remove empty mail attribute
	sedinline 's# mail=""##' "$FILE_NEW"
	echo "processed $FILE_NEW..."
done

# copy mail_config template
cp "$FLAGS_install/examples/templates/mail_config" "$FLAGS_install/examples/mail_config"

exit $EXIT_OK
