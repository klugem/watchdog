#!/bin/bash
SCRIPT_FOLDER=$(cd $(dirname $(readlink -f "${BASH_SOURCE[0]}" 2>/dev/null || readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")) && pwd -P)
source $SCRIPT_FOLDER/../core_lib/includeBasics.sh
source $SCRIPT_FOLDER/../core_lib/newModule.functions.sh

# tmp file for arguments / flag names
TMP_NAME=$(getTmpFile "createNewModule" "/tmp")
touch "$TMP_NAME"
trap "rm -f \"$TMP_NAME\" 2>&1 > /dev/null" EXIT

NEWLINE_REPLACE='{%NEWLINE%}'
# variables in XSD file
MODULE_REPLACE='{%MODULE%}'
COMMAND_REPLACE='{%CALLED_COMMAND%}'
ARGUMENT_NAME_REPLACE='{%NAME%}'
ARGUMENT_TYPE_REPLACE='{%TYPE%}'
ARGUMENT_MIN_REPLACE='{%MIN%}'
ARGUMENT_MAX_REPLACE='{%MAX%}'
ARGUMENT_NUMERIC_TYPE='{%NUMERIC_TYPE%}'
ARGUMENT_XSD_TYPE='{%XSD_TYPE%}'
ARGUMENT_REPLACE='{%ARGUMENTS%}'
ARGUMENT_PATTERN='{%PATTERN%}'
PARAM_TYPES_REPLACE='{%NEW_PARAM_TYPES%}'
FLAG_REPLACE='{%FLAGS%}'
PARAM_PARENT_OPTIONAL='{%PARAM_PARENT_OPTIONAL%}'

# return param variables
RETURN_ELEMENTS='{%RETURN_TYPE_ELEMENTS%}'
RETURN_NAME_V='{%RETURN_NAME%}'
RETURN_TYPE_V='{%RETURN_TYPE%}'

# variables in sh file
VAR_TYPE_REPLACE='{%VAR_TYPE%}'
VAR_NAME_REPLACE='{%VAR_NAME%}'
VAR_DEFAULT_REPLACE='{%VAR_DEFAULT%}'
VAR_OPTIONAL_REPLACE='{%VAR_OPTIONAL%}'
VAR_NAME_SHORT_REPLACE='{%VAR_NAME_SHORT%}'
SH_VARIABLES_REPLACE='{%VARIABLES%}'
SH_VARIABLES_CHECK_REPLACE='{%MANDATORY_VAR_CHECK%}'
SH_VARIABLES_EXISTS_REPLACE='{%FILE_EXISTENCE_CHECK%}'
VAR_VALUE='{%VAR_VALUE%}'
VAR_MIN=$ARGUMENT_MIN_REPLACE 
VAR_MAX=$ARGUMENT_MAX_REPLACE
VAR_PARAM_PARENT_OPTIONAL=0
RETURN_VARIABLES='{%RETURN_VARIABLES%}'
SH_VARIABLES_RETURN_VARIABLES_BLOCK='{%RETURN_VARIABLES_BLOCK%}'

# define parameters
DEFINE_string 'name' '' 'name of module' 'n'
DEFINE_string 'binName' '' '[optional] if a name of a binary (e.g. sleep or cp) is given no bash scripts are generated' 'd'
DEFINE_integer 'paramFormat' '2' '[optional] 0: plain, 1: shortOnly, longShort: 2 ' 'p'
DEFINE_integer 'paramSpacing' '0' '[optional] 0: blankSeperated, 1: equalSeparated ' 's'
DEFINE_integer 'paramQuoting' '1' '[optional] 0: unquoted, 1: singleQuoted, doubleQuoted: 2 ' 'q'
DEFINE_string 'moduleFolder' 'modules' '[optional] name of the folder that should be used as parent folder' 'f'
DEFINE_boolean 'debug' 'false' '[optional] prints out debug messages.' ''

# parse parameters
FLAGS "$@" || exit $EXIT_INVALID_ARGUMENTS
eval set -- "${FLAGS_ARGV}"
printParamValues "initial parameters" # print param values, if in debug mode

# check if mandatory arguments are there
if [ -z "$FLAGS_name" ]; then
	echoError "Parameter -n must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi

# ensure that name contains no spaces
if [[ "$FLAGS_name" = *[[:space:]]* ]]; then
	echoError "The name of the module can not contain any whitespace characters";
	exit $EXIT_INVALID_ARGUMENTS
fi

# set output name, if none is set
if [ "$FLAGS_binName" == '' ]; then
	IS_WATCHDOG_MODULE=1
	FLAGS_binName="$FLAGS_name.sh"
	CALLED_COMMAND="$FLAGS_name.sh"
else
	IS_WATCHDOG_MODULE=0
	CALLED_COMMAND=$FLAGS_binName
fi

# set folder where the new module should be stored
if [ -z "$FLAGS_moduleFolder" ]; then
	MODULE_FOLDER="modules"
else
	MODULE_FOLDER="$FLAGS_moduleFolder"
	# test if folder is there and if not create it
	if [ ! -e "$SCRIPT_FOLDER/../${MODULE_FOLDER}" ]; then
		mkdir "$SCRIPT_FOLDER/../${MODULE_FOLDER}"
	fi
fi

# ensure valid types for the parameter formating
if [ "$FLAGS_paramFormat" -le -1 ] || [ "$FLAGS_paramFormat" -ge 3 ] ; then
	FLAGS_paramFormat=2
	echoWarn "ParamFormat value must be between [1:3]; User input was overwritten with $FLAGS_paramFormat!"
fi
if [ "$FLAGS_paramSpacing" -le -1 ] || [ "$FLAGS_paramSpacing" -ge 2 ] ; then
	FLAGS_paramSpacing=0
	echoWarn "ParamSpacing must be between [1:2]; User input was overwritten with $FLAGS_paramSpacing!"
fi
if [ "$FLAGS_paramQuoting" -le -1 ] || [ "$FLAGS_paramQuoting" -ge 3 ] ; then
	FLAGS_paramQuoting=1
	echoWarn "ParamQuoting must be between [1:3]; User input was overwritten with $FLAGS_paramQuoting!"
fi
printParamValues "parameters before actual script starts" # print param values, if in debug mode
##################################################### START with actual SCRIPT ##################################################### 

# test if module is not there
if [ ! -e "$SCRIPT_FOLDER/../${MODULE_FOLDER}/$FLAGS_name" ]; then
	# create a new folder
	mkdir "$SCRIPT_FOLDER/../${MODULE_FOLDER}/$FLAGS_name"

	XSD_FILENAME="$SCRIPT_FOLDER/../${MODULE_FOLDER}/$FLAGS_name/${FLAGS_name}.xsd"
	if [ $IS_WATCHDOG_MODULE -eq 1 ]; then
		MSCRIPT_FILENAME="$SCRIPT_FOLDER/../${MODULE_FOLDER}/$FLAGS_name/${FLAGS_name}.sh"
		MSCRIPT_TEST_FILENAME="$SCRIPT_FOLDER/../${MODULE_FOLDER}/$FLAGS_name/test_${FLAGS_name}.sh"

		# create test data folder
		mkdir "$SCRIPT_FOLDER/../${MODULE_FOLDER}/$FLAGS_name/test_data"

		# copy the template scripts
		cp "$SCRIPT_FOLDER/../xsd/template/module.template.sh" "$MSCRIPT_FILENAME"
		cp "$SCRIPT_FOLDER/../xsd/template/module.test.template.sh" "$MSCRIPT_TEST_FILENAME"

		# replace the module name
		sedinline 's/'${MODULE_REPLACE}'/'${FLAGS_name}'/g' "$MSCRIPT_FILENAME"
		sedinline 's/'${MODULE_REPLACE}'/'${FLAGS_name}'/g' "$MSCRIPT_TEST_FILENAME"
	fi

	# copy the template XSD
	cp "$SCRIPT_FOLDER/../xsd/template/module.template.xsd" "$XSD_FILENAME"
	# repace the module name
	sedinline 's/'${MODULE_REPLACE}'/'${FLAGS_name}'/g' "$XSD_FILENAME"
	sedinline 's/'${COMMAND_REPLACE}'/'${CALLED_COMMAND}'/g' "$XSD_FILENAME"

	# create arguments
	SPACER="\n"
	REPLACE_ARGUMENTS=""
	REPLACE_FLAGS=""
	REPLACE_SH=""
	REPLACE_CHECK=""
	PATTERN=""
	VERIFY_EXIST=""
	RETURN_VARS=""
	TEMPLATE_FILE="$SCRIPT_FOLDER/../xsd/template/argument.template.xsd"

	confirm "Do you want to add an argument?"
	while [ $CONFIRM_RETURN -eq 1 ]; do
		ARGUMENT_NAME=$(getInputName "Please enter a name for the argument: ")
		ARGUMENT_NAME_SHORT=$(echo "$ARGUMENT_NAME" | head -c 1)

		# get min occurs value
		confirm "Is the argument optional? "
		if [ $CONFIRM_RETURN -eq 1 ]; then
			ARGUMENT_MIN=0
			ARGUMENT_DEFAULT=$(getInput "Please enter an default value for argument '${ARGUMENT_NAME}': ")
		else
			ARGUMENT_MIN=1
			ARGUMENT_DEFAULT=""
			VAR_PARAM_PARENT_OPTIONAL=1
		fi

		# get max occurs value
		ARGUMENT_MAX=$(getInputNumber "How often can the argument occour? (-1 for unbounded) ")
		if [ $ARGUMENT_MAX -le 0 ]; then
			ARGUMENT_MAX="unbounded"
		fi

		ARGUMENT_TYPE=$(getInputType "Please enter a type for the argument (double (d), integer (i), string (s) or path (p)): ")
		NUMERIC_TYPE="n.a."
		###################################
		case "$ARGUMENT_TYPE" in
		p)	ARGUMENT_TYPE_SH="string"
			# ask what type of path it is
			PATH_TYPE=$(getInputSubType "Please enter a type for the path (filename (f), absolute file path (af), relative file path (rf), absolute folder path (ap) or relative folder path (rp)): ")
			CHECK_EX_TYPE="ile"
			case "$PATH_TYPE" in
				f)
				ARGUMENT_TYPE_XSD="Filename"
				;;
				af)
				ARGUMENT_TYPE_XSD="AbsoluteFilePath"
				;;
				rf)
				ARGUMENT_TYPE_XSD="RelativeFilePath"
				;;
				ap)
				ARGUMENT_TYPE_XSD="AbsoluteFolderPath"
				CHECK_EX_TYPE="older"
				;;
				rp)
				ARGUMENT_TYPE_XSD="RelativeFolderPath"
				CHECK_EX_TYPE="older"
				;;
			esac

			# ask the question and do it
			confirm "Should the bash script enforce the existence of that f${CHECK_EX_TYPE}? "
			if [ $CONFIRM_RETURN -eq 1 ]; then
				VERIFY_EXIST="${VERIFY_EXIST}${SPACER}verifyF${CHECK_EX_TYPE}Existence \"\$FLAGS_${ARGUMENT_NAME}\""
			fi
		   	;;
		s)	ARGUMENT_TYPE_XSD="String"
			ARGUMENT_TYPE_SH="string"
			confirm "Do you want to restrict the string with a pattern?"
			if [ $CONFIRM_RETURN -eq 1 ]; then
				ARGUMENT_TYPE_XSD="String_${NAME}_${FLAGS_name}"

				# get pattern
				PATTERN=$(getInput "Please enter the regex in XPath2 syntax: ")

				# replace the stuff in the XSD file
				CUSTOM_PARAM="${CUSTOM_PARAM}"$(cat "$SCRIPT_FOLDER/../xsd/template/string_pattern.template.xsd" | sed -e 's/'${ARGUMENT_PATTERN}'/'"${PATTERN}"'/g' -e 's/'${MODULE_REPLACE}'/'${FLAGS_name}'/g' -e 's/'${ARGUMENT_NAME_REPLACE}'/'${NAME}'/g' -e 's/"/\\"/g')"\n"  
			fi
		   	;;
		d)  	ARGUMENT_TYPE_XSD="Double"
			ARGUMENT_TYPE_SH="float"
			NUMERIC_TYPE=$ARGUMENT_TYPE_SH
			getMinMax Double "$ARGUMENT_NAME" "$ARGUMENT_NAME_SHORT"
		    	;;
		i)  	ARGUMENT_TYPE_XSD="Integer"
			ARGUMENT_TYPE_SH="integer"
			NUMERIC_TYPE=$ARGUMENT_TYPE_SH
			getMinMax Integer "$ARGUMENT_NAME" "$ARGUMENT_NAME_SHORT"
		    	;;
		esac

		################################### add the argument ###################################
		addArgument "$TEMPLATE_FILE" "$ARGUMENT_NAME" "$ARGUMENT_NAME_SHORT" "$ARGUMENT_TYPE_XSD" "$ARGUMENT_TYPE_SH" "$ARGUMENT_MIN" "$ARGUMENT_MAX" "$ARGUMENT_DEFAULT" "$PATTERN"
		confirm "Do you want to add another argument?"
	done

	################################################################################################
	# create flags
	REPLACE_FLAGS=""
	confirm "Do you want to add a flag?"
	while [ $CONFIRM_RETURN -eq 1 ]; do
		FLAG_NAME=$(getInputName "Please enter a name for the flag: ")
		FLAG_NAME_SHORT=$(echo "$FLAG_NAME" | head -c 1)
		
		# get default value			
		confirm "Should the flag be enabled by default? (to disable it, type --no${FLAG_NAME}) "
		if [ $CONFIRM_RETURN -eq 1 ]; then
			DEFAULT_FLAG_VALUE=0
		else
			DEFAULT_FLAG_VALUE=1
		fi

		################################### add the flag ###################################
		addFlag "$FLAG_NAME" "$FLAG_NAME_SHORT" "$DEFAULT_FLAG_VALUE"
		confirm "Do you want to add another flag?"
	done

	################################################################################################
	# ask for return variable
	REPLACE_COMPLETE_RETURN=""
	confirm "Do you want to add a return variable?"
	FIRST_RET=1
	while [ $CONFIRM_RETURN -eq 1 ]; do
		RETURN_NAME=$(getInputName "Please enter the name for the return variable: ")
		
		RET_TYPE=$(getInputReturnType "Please enter a type for the return value (string (s), double (d), integer (i) or boolean (b)): ")
		case "$RET_TYPE" in
			s)
			RETURN_TYPE_XSD="string"
			;;
			d)
			RETURN_TYPE_XSD="double"
			;;
			i)
			RETURN_TYPE_XSD="integer"
			;;
			b)
			RETURN_TYPE_XSD="boolean"
			;;
		esac

		################################### add the return variable ###################################
		addReturn "$RETURN_NAME" "$RETURN_TYPE_XSD" 
		if [ $FIRST_RET -eq 1 ]; then
			addReturnArgument
			if [ "${REPLACE_SH}" != "" ]; then
				REPLACE_SH="${REPLACE_SH}${SPACER}${REPLACE_RET}"
			else
				REPLACE_SH=${REPLACE_RET}
			fi
		fi
		FIRST_RET=0

		# add the SH write part
		RETURN_VARS="${RETURN_VARS}"$(cat "$SCRIPT_FOLDER/../xsd/template/returnElement.template.sh" | sed -e 's/'${RETURN_NAME_V}'/'"${RETURN_NAME}"'/g')"\n" 
		confirm "Do you want to add another return variable?"
	done

	################################################################################################
	# replace the variables in the xsd file
	if [ "${REPLACE_ARGUMENTS}" == "" ]; then
		START_LINE=$(grep -n "${ARGUMENT_REPLACE}" "$XSD_FILENAME" | cut -f 1 -d ":")
		if [ "$START_LINE" == "" ]; then
			echoError "Start line was not found for '${ARGUMENT_REPLACE}' in the XML file."
			exit $EXIT_MISFORMATED_INPUT
		fi
		sedinline "${START_LINE}d" "$XSD_FILENAME"
	else
		replaceInFile "${ARGUMENT_REPLACE}" "${REPLACE_ARGUMENTS}" "$XSD_FILENAME"
	fi
	if [ "${REPLACE_FLAGS}" == "" ]; then
		START_LINE=$(grep -n "${FLAG_REPLACE}" "$XSD_FILENAME" | cut -f 1 -d ":")
		if [ "$START_LINE" == "" ]; then
			echoError "Start line was not found for '${FLAG_REPLACE}' in the XML file."
			exit $EXIT_MISFORMATED_INPUT
		fi
		sedinline "${START_LINE}d" "$XSD_FILENAME"
	else
		replaceInFile "${FLAG_REPLACE}" "${REPLACE_FLAGS}" "$XSD_FILENAME"
	fi
	
	# if no parameters can be used, remove the section from the XSD file
	if [ "${REPLACE_ARGUMENTS}" == "" ] && [ "${REPLACE_FLAGS}" == "" ]; then
		sedinline "4,9d" "$XSD_FILENAME"
		sedinline "18d" "$XSD_FILENAME"
	fi
	
	# if no return parameters are used
	if [ "${REPLACE_COMPLETE_RETURN}" == "" ]; then
		# remove the line in the task definition
		START_LINE=$(grep -n "${FLAGS_name}TaskReturnType" "$XSD_FILENAME" | cut -f 1 -d ":" | head -n 1)
		if [ "$START_LINE" == "" ]; then
			echoError "Start line was not found for '${RETURN_ELEMENTS}' in the XML file."
			exit $EXIT_MISFORMATED_INPUT
		fi
		sedinline "${START_LINE}d" "$XSD_FILENAME"

		# remove the definition of the return block
		START_LINE=$(grep -n "${FLAGS_name}TaskReturnType" "$XSD_FILENAME" | cut -f 1 -d ":" | head -n 2)
		if [ "$START_LINE" == "" ]; then
			echoError "Start line was not found for '${FLAGS_name}TaskReturnType' in the XML file."
			exit $EXIT_MISFORMATED_INPUT
		fi

		START_CUT=$((START_LINE-1))
		STOP_CUT=$((START_LINE+9))	
		RANGE_CUT="${START_CUT},${STOP_CUT}d"
		sedinline $RANGE_CUT "$XSD_FILENAME"
	else
		replaceInFile "${RETURN_ELEMENTS}" "${REPLACE_COMPLETE_RETURN}" "$XSD_FILENAME"
		# prepare complete SH part
		RETURN_VARS_COMPLETE=$(cat "$SCRIPT_FOLDER/../xsd/template/returnBlock.template.sh" | sed -e 's/'${RETURN_VARIABLES}'/'"${RETURN_VARS}"'/g') 
	fi

	# add the custom param types
	replaceInFile "${PARAM_TYPES_REPLACE}" "${CUSTOM_PARAM}" "$XSD_FILENAME"
	replaceInFile "${PARAM_PARENT_OPTIONAL}" "${VAR_PARAM_PARENT_OPTIONAL}" "$XSD_FILENAME"

	# replace the variables in the module sh files
	if [ $IS_WATCHDOG_MODULE -eq 1 ]; then
		replaceInFile "${SH_VARIABLES_REPLACE}" "${REPLACE_SH}" "$MSCRIPT_FILENAME"
		replaceInFile "${SH_VARIABLES_CHECK_REPLACE}" "${REPLACE_CHECK}" "$MSCRIPT_FILENAME"
		replaceInFile "${SH_VARIABLES_EXISTS_REPLACE}" "${VERIFY_EXIST}" "$MSCRIPT_FILENAME"
		replaceInFile "${SH_VARIABLES_RETURN_VARIABLES_BLOCK}" "${RETURN_VARS_COMPLETE}" "$MSCRIPT_FILENAME"

		# make the script executable
		chmod uog+x "$MSCRIPT_FILENAME"
		chmod uog+x "$MSCRIPT_TEST_FILENAME"
	fi

	# delete the tmp file
	rm -rf "$TMP_NAME" 2>&1 > /dev/null
	exit $EXIT_OK

else
	echoError "Module '$FLAGS_name' does already exist."
	exit $EXIT_INVALID_ARGUMENTS
fi

# exit with exit status 0 or exit code shoul be there earlier
echoError "Reached end of script! Exit should be performed earlier..."
exit $EXIT_REACHED_END_OF_SCRIPT
