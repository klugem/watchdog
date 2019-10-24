#!/bin/bash
SCRIPT_FOLDER=$(cd $(dirname $(readlink -f "${BASH_SOURCE[0]}" 2>/dev/null || readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")) && pwd -P)
source $SCRIPT_FOLDER/includeBasics.sh

DEFAULT_KEY_WORDS="error:exception:failure:killed:segmentation fault:core dump:failed:permission denied"
BEFORE_LINES=10

# check, if used tools are installed
USED_TOOLS='sed:readlink:echo:head:tail'
MESSAGE=$($LIB_SCRIPT_FOLDER/checkUsedTools.sh "$USED_TOOLS" "check_shflag_tools")
CODE=$?

if [ $CODE -ne 0 ]; then
	echoInfo "$MESSAGE"
	exit $EXIT_TOOLS_MISSING
fi

# check, if parameters are ok
if [ "$#" -ne 1 ] && [ "$#" -ne 2 ] && [ "$#" -ne 3 ]; then
	echo "Illegal number of parameters! First must be path to error file and second keywords to search for. Second might be empty than it is searched for: '$DEFAULT_KEY_WORDS' Multiple values can be seperated by ':'. If third parameter is set, the default keywords are not added to the user specific ones!" 
	exit -1
fi

ERROR_FILES=$(echo $1 | sed 's|~|'$HOME'|g')
KEY_WORDS=$DEFAULT_KEY_WORDS
if [ "$#" -eq 2 ]; then
	if [ "$#" -eq 3 ]; then
		KEY_WORDS=$2
	else
		KEY_WORDS="${KEY_WORDS}:${2}"
	fi
fi

# replace : with |
KEY_WORDS=$(echo $KEY_WORDS | sed 's/:/|/g')

# split files to array
IFS=$':' read -rd '' -a ERROR_FILES <<<"$ERROR_FILES"

# process each file
RETURN=0
for I in "${!ERROR_FILES[@]}"; do 
	ERROR_FILE=$(readlink_own -m ${ERROR_FILES[$I]})

	# check, if file is existent
	if [ ! -e $ERROR_FILE ]; then
		echo "File: '$ERROR_FILE' was not found!"
		RETURN=$(($RETURN+1))
	else
		# get potential errors
		R_ALL=$(grep -n -E -i "$KEY_WORDS" $ERROR_FILE)

		# get line of errors
		L_ALL=$(echo -e "$R_ALL" | cut -f 1 -d ":")
		E_ALL=$(echo -e "$R_ALL" | cut -f 2 -d ":")

		# split into arrays
		IFS=$'\n' read -rd '' -a E_ALL <<<"$E_ALL"
		IFS=$'\n' read -rd '' -a L_ALL <<<"$L_ALL"

		# get for each error the last ten lines
		for i in "${!L_ALL[@]}"; do 
			L=${L_ALL[$i]}
			E=${E_ALL[$i]}

			echo "Found error '$E' in line $L in file '$ERROR_FILE'!"
			echo ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
			echo -e "$(head -n $(($L+$BEFORE_LINES+1)) $ERROR_FILE | tail -n $((2*$BEFORE_LINES)))"
			echo -e "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n"

			RETURN=$(($RETURN+1))
		done
	fi
done

# exit with return value
exit $RETURN
