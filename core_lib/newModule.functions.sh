function addArgument() {
	TEMPLATE=$1
	NAME=$(checkNameExistance "$2" 1 "name")
	SHORT_NAME=$(checkNameExistance "$3" 2 "shortcut")
	PARAM_TYPE="param${4}"
	SH_TYPE=$5
	ARGUMENT_MIN=$6
	ARGUMENT_MAX=$7
	DEFAULT_VALUE=$8

	# infer some stuff
	if [ $ARGUMENT_MIN -eq 0 ]; then
		OPTIONAL="[optional]"
	else
		OPTIONAL=""
	fi
	if [ "$SHORT_NAME" == "!" ]; then 
		SHORT_NAME=""
	fi
	if [ "$NAME" == "returnFilePath" ]; then
		OPTIONAL="path to the return variables file"
	fi

	REPLACE_ARGUMENTS="${REPLACE_ARGUMENTS}${SPACER}"$(cat "$TEMPLATE" | sed -e 's/'${ARGUMENT_NAME_REPLACE}'/'${NAME}'/g' -e 's/'${ARGUMENT_TYPE_REPLACE}'/'$PARAM_TYPE'/g' -e 's/'${ARGUMENT_MIN_REPLACE}'/'${ARGUMENT_MIN}'/g' -e 's/'${ARGUMENT_MAX_REPLACE}'/'${ARGUMENT_MAX}'/g' -e 's/"/\\"/g')

	# add the parameters to the watchdog module
	if [ $IS_WATCHDOG_MODULE -eq 1 ]; then
		REPLACE_SH="${REPLACE_SH}${SPACER}"$(cat $SCRIPT_FOLDER/../xsd/template/sh_parser.argument.template.sh | sed -e 's/'${VAR_NAME_REPLACE}'/'${NAME}'/g' -e 's/'${VAR_TYPE_REPLACE}'/'${SH_TYPE}'/g'  -e 's/'${VAR_NAME_SHORT_REPLACE}'/'${SHORT_NAME}'/g' -e 's/'${VAR_OPTIONAL_REPLACE}'/'${OPTIONAL}'/g' -e 's/'${VAR_DEFAULT_REPLACE}'/'${DEFAULT_VALUE}'/g' -e 's/"/\\"/g')

		if [ $ARGUMENT_MIN -gt 0 ]; then
			# add check
			REPLACE_CHECK="${REPLACE_CHECK}${SPACER}"$(cat $SCRIPT_FOLDER/../xsd/template/mandatory_argument_check.template.sh | sed -e 's/'${VAR_NAME_REPLACE}'/'${NAME}'/g' -e 's/'${VAR_NAME_SHORT_REPLACE}'/'${SHORT_NAME}'/g' -e 's/"/\\"/g')
		fi
	fi
	if [ "$SHORT_NAME" != "!" ]; then 
		echo "${NAME}|${SHORT_NAME}" >> $TMP_NAME
	fi
}

function addReturnArgument() {
	NAME="returnFilePath"
	PARAM_TYPE="paramString"
	SH_TYPE="string"

	REPLACE_RET=$(cat $SCRIPT_FOLDER/../xsd/template/sh_parser.argument.template.sh | sed -e 's/'${VAR_NAME_REPLACE}'/'${NAME}'/g' -e 's/'${VAR_TYPE_REPLACE}'/'${SH_TYPE}'/g'  -e 's/'${VAR_NAME_SHORT_REPLACE}'//g' -e 's/'${VAR_OPTIONAL_REPLACE}'/path to the return variables file/g' -e 's/ TODO: place description here!//' -e 's/'${VAR_DEFAULT_REPLACE}'//g' -e 's/"/\\"/g')
}

function addFlag() {
	FLAG_NAME=$(checkNameExistance "$1" 1 "name")
	FLAG_NAME_SHORT=$(checkNameExistance "$2" 2 "shortcut")
	DEFAULT_FLAG_VALUE=$3

	if [ "$SHORT_NAME" == "!" ]; then 
		SHORT_NAME=""
	fi

	# replace the flag
	REPLACE_FLAGS="${REPLACE_FLAGS}${SPACER}"$(cat $SCRIPT_FOLDER/../xsd/template/flag.template.xsd | sed -e 's/'${ARGUMENT_NAME_REPLACE}'/'${FLAG_NAME}'/g' -e 's/"/\\"/g')

	# add the parameters to the watchdog module
	if [ $IS_WATCHDOG_MODULE -eq 1 ]; then
		REPLACE_SH="${REPLACE_SH}${SPACER}"$(cat $SCRIPT_FOLDER/../xsd/template/sh_parser.argument.template.sh | sed -e 's/'${VAR_TYPE_REPLACE}'/boolean/g' -e 's/'${VAR_NAME_REPLACE}'/'${FLAG_NAME}'/g' -e 's/'${VAR_DEFAULT_REPLACE}'/'${DEFAULT_FLAG_VALUE}'/g' -e 's/'${VAR_OPTIONAL_REPLACE}'/[optional]/g' -e 's/'${VAR_NAME_SHORT_REPLACE}'/'${FLAG_NAME_SHORT}'/g' -e 's/"/\\"/g')
	fi
	if [ "$FLAG_NAME_SHORT" != "!" ]; then 
		echo "${FLAG_NAME}|${FLAG_NAME_SHORT}" >> $TMP_NAME
	fi
}

function addReturn() {
	RETURN_NAME=$(checkNameExistance "RETURN_$1" 1 "return value")
	RETURN_NAME=${RETURN_NAME/RETURN_/}
	RETURN_TYPE=$2

	# replace the return value
	REPLACE_COMPLETE_RETURN="${REPLACE_COMPLETE_RETURN}${SPACER}"$(cat $SCRIPT_FOLDER/../xsd/template/returnElement.xsd | sed -e 's/'${RETURN_NAME_V}'/'${RETURN_NAME}'/g' | sed -e 's/'${RETURN_TYPE_V}'/'${RETURN_TYPE}'/g' -e 's/"/\\"/g')

	# store it in names used
	echo "RETURN_${RETURN_NAME}" >> $TMP_NAME
}

function checkNameExistance() {
	S=$1
	if [ "$S" != "!" ]; then 
		TYPE=$2 # 1 == name , 2 == shortcut
		NAME=$3
		RET=$(cut -f $TYPE -d '|' "$TMP_NAME" | grep -E "^$S\$" | wc -l)
		while [ $RET -ge 1 ]; do
			if [ $TYPE -eq 1 ]; then
				TEXT="Please enter a ${NAME} which was not used before as '${S/RETURN_/}' is: "
			else
				TEXT="Please enter a ${NAME} which was not used before as '${S/RETURN_/}' is. If non should be set, enter '!': "
			fi	
			S=$(getNewName "$TEXT")
			RET=$(cut -f $TYPE -d '|' $TMP_NAME | grep -E "^$S\$" | wc -l)
		done
	fi
	echo $S
}

function getNewName() {
	REPLY=$(getInput "$1")
	while [[ ! $REPLY =~ ^!|[A-Za-z]+$ ]] || [ "$REPLY" == "" ] ; do
		REPLY=$(getInput "$1")
	done
	echo $REPLY
}


function getMinMax() {
	TYPE=$1
	NAME=$2
	SHORT_NAME=$3
	L_TYPE=$(echo $TYPE | tr '[:upper:]' '[:lower:]')
	# set range for integer or double
	confirm "Should the ${L_TYPE} be restricted to a certain range? "
	if [ $CONFIRM_RETURN -eq 1 ]; then
		MIN_VALUE_V=$(getInputNumber "Minimum allowed number: ")
		MAX_VALUE_V=$(getInputNumber "Maximum allowed number: ")
		if [ "$TYPE" == "Double" ]; then
			MIN_VALUE_V=$(printf "%.*f\n" 10 $MIN_VALUE_V)
			MAX_VALUE_V=$(printf "%.*f\n" 10 $MAX_VALUE_V)
		fi

		XSD_TEMPLATE="integer_range.template.xsd"
		ARGUMENT_TYPE_XSD="${TYPE}Range_${NAME}_${FLAGS_name}"

		# add check
		REPLACE_CHECK="${REPLACE_CHECK}${SPACER}"$(cat $SCRIPT_FOLDER/../xsd/template/integer_range_argument_check.template.sh | sed -e 's/'${VAR_NAME_REPLACE}'/'${NAME}'/g' -e 's/'${VAR_NAME_SHORT_REPLACE}'/'${SHORT_NAME}'/g' -e 's/'${VAR_MIN}'/'${MIN_VALUE_V}'/g' -e 's/'${VAR_MAX}'/'${MAX_VALUE_V}'/g' -e 's/"/\\"/g')
		SPACER="\n"
	else 
		confirm "Should the ${TYPE} be greater or less than a specific value? "
		if [ $CONFIRM_RETURN -eq 1 ]; then
			VALUE_V=$(getInputNumber "Inclusive restriction value: ")
			if [ "$TYPE" == "Double" ]; then
				VALUE_V=$(printf "%.*f\n" $VALUE_V)
			fi
			MIN_VALUE_V=$VALUE_V
			MAX_VALUE_V=$VALUE_V
			confirm "Should the ${TYPE} be greater or equal than '$VALUE_V'? "

			# add check
			if [ $CONFIRM_RETURN -eq 1 ]; then
				REPLACE_CHECK="${REPLACE_CHECK}${SPACER}"$(cat $SCRIPT_FOLDER/../xsd/template/integer_greater_argument_check.template.sh | sed -e 's/'${VAR_NAME_REPLACE}'/'${NAME}'/g' -e 's/'${VAR_NAME_SHORT_REPLACE}'/'${SHORT_NAME}'/g' -e's/'${VAR_VALUE}'/'${VALUE_V}'/g' -e 's/"/\\"/g')

				XSD_TEMPLATE="integer_greater.template.xsd"
				ARGUMENT_TYPE_XSD="${TYPE}Greater_${NAME}_${FLAGS_name}"
			else
				REPLACE_CHECK="${REPLACE_CHECK}${SPACER}"$(cat $SCRIPT_FOLDER/../xsd/template/integer_lower_argument_check.template.sh | sed -e 's/'${VAR_NAME_REPLACE}'/'${NAME}'/g' -e 's/'${VAR_NAME_SHORT_REPLACE}'/'${SHORT_NAME}'/g' -e 's/'${VAR_VALUE}'/'${VALUE_V}'/g' -e 's/"/\\"/g')
			
				XSD_TEMPLATE="integer_lower.template.xsd"
				ARGUMENT_TYPE_XSD="${L_TYPE}Lower_${NAME}_${FLAGS_name}"
			fi
			SPACER="\n"
		fi
	fi

	if [ ! -z $XSD_TEMPLATE ]; then
		# replace the stuff in the XSD file
		CUSTOM_PARAM="${CUSTOM_PARAM}"$(cat "$SCRIPT_FOLDER/../xsd/template/${XSD_TEMPLATE}" | sed -e 's/'${VAR_MIN}'/'${MIN_VALUE_V}'/g' -e 's/'${VAR_MAX}'/'${MAX_VALUE_V}'/g' -e 's/'${ARGUMENT_NUMERIC_TYPE}'/'${TYPE}'/g' -e 's/'${MODULE_REPLACE}'/'${FLAGS_name}'/g' -e 's/'${ARGUMENT_NAME_REPLACE}'/'${NAME}'/g' -e 's/"/\\"/g')"\n"  
		unset XSD_TEMPLATE
	fi
}

function getInputType() {
	REPLY=""
	while [[ ! $REPLY =~ ^[disp]$ ]]; do
		REPLY=$(getInput "$1")
	done
	echo $REPLY
}

function getInputSubType() {
	REPLY=""
	while [[ ! $REPLY =~ ^f|af|rf|ap|rp$ ]]; do
		REPLY=$(getInput "$1")
	done
	echo $REPLY
}

function getInputReturnType() {
	REPLY=""
	while [[ ! $REPLY =~ ^s|d|i|b$ ]]; do
		REPLY=$(getInput "$1")
	done
	echo $REPLY
}
