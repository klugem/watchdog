#!/bin/bash
SCRIPT_FOLDER=$(cd $(dirname $(readlink -f "${BASH_SOURCE[0]}" 2>/dev/null || readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")) && pwd -P)
source "$SCRIPT_FOLDER/../core_lib/includeBasics.sh"
MISSING=0

cd "$SCRIPT_FOLDER/../"
# check the normal tools
for MODULE in `find modules -maxdepth 3 -name "*.sh" 2>/dev/null | grep -v "test"| grep -E "modules/.+/.+\\.sh"`
do
	HAS_DEP=0
	MOD=$(echo "$MODULE" | sed 's|modules/||' | sed -E 's|/.+||')

	VAR=$(grep -E '^USED_TOOLS=' "$MODULE" | sed 's/\W*USED_TOOLS=//' | sed "s/['\"]//g" | grep -v '!TODO!' | sort -u | grep -E -v "^\^" | tr ':' '\n' | sort -u | tr '\n' ':' | sed 's/::/:/g' | sed -E 's/^://' | sed -E 's/:$//' | sed -E 's/\W+#.+$//')
	if [ "$VAR" != "" ]; then
		echo "Checking binaries for '$MODULE'..."
		MESSAGE=$("$SCRIPT_FOLDER/../core_lib/checkUsedTools.sh" "$VAR" "check_shflag_tools")
		CODE=$?
		HAS_DEP=1

		if [ $CODE -ne 0 ]; then
			echoError "$MESSAGE"
			MISSING=1
			INACTIVE="$MOD:$INACTIVE"
		else
			OK_MOD="$MOD:$OK_MOD"
		fi
	fi


	# check perl packages
	VAR=$(grep -E '^USED_TOOLS=' "$MODULE" | sed 's/\W*USED_TOOLS=//' | sed "s/['\"]//g" | grep -v '!TODO!' | sort -u | grep "^\^perl" | tr '|' '\n' | sort -u | tr '\n' '|' | sed -E 's/\^perl//g' | sed 's/||/|/g' | sed -E 's/^\|//' | sed -E 's/\|$//')
	if [ "$VAR" != "" ]; then
		echo "Checking installed perl packages for '$MODULE'..."
		MESSAGE=$("$SCRIPT_FOLDER/../core_lib/checkUsedTools.sh" "^perl|$VAR")
		CODE=$?
		HAS_DEP=1

		if [ $CODE -ne 0 ]; then
			echoError "$MESSAGE"
			MISSING=1
			INACTIVE="$MOD:$INACTIVE"
		else
			OK_MOD="$MOD:$OK_MOD"
		fi
	fi

	# check for R packages
	VAR=$(grep -E '^USED_TOOLS=' "$MODULE" | sed 's/\W*USED_TOOLS=//' | sed "s/['\"]//g" | grep -v '!TODO!' | sort -u | grep "^\^R" | tr '|' '\n')
	# automatically find libs that are only mentioned in the R files
	if [ -d "$SCRIPT_FOLDER/../modules/$MOD" ]; then
		cd "$SCRIPT_FOLDER/../modules/$MOD"
		VAR="$VAR|"$(find . -maxdepth 2 -name "*.R" -exec grep -v "#NO_CHECK" {} \; | grep -o -E '(library|require)\([^)\\$]+\)' | sed 's/library(//' | sed 's/require(//' | sed 's/)//' | sed 's/"//g' | sed "s/'//g")  
		VAR=$(echo "$VAR" | tr '|' '\n' | sort -u | tr '\n' '|' | sed -E 's/\^R//g' | sed 's/||/|/g' | sed -E 's/^\|//' | sed -E 's/\|$//')
		cd "$SCRIPT_FOLDER/.."
		if [ "$VAR" != "" ]; then
			echo "Checking installed R packages for '$MODULE'..."
			MESSAGE=$("$SCRIPT_FOLDER/../core_lib/checkUsedTools.sh" "^R|$VAR")
			CODE=$?
			HAS_DEP=1

			if [ $CODE -ne 0 ]; then
				echoError "$MESSAGE"
				MISSING=1
				INACTIVE="$MOD:$INACTIVE"
			else
				OK_MOD="$MOD:$OK_MOD"
			fi
		fi
	fi

	# test if a module has no dependencies
	if [ "$HAS_DEP" -eq "0" ]; then
		OK_MOD="$MOD:$OK_MOD"
	fi
done

# check, if all was ok or not
if [ $MISSING -eq 1 ]; then
	echo -e "\n[ERROR] The following modules can not be used because of missing dependencies:"
	INACTIVE=$(echo "$INACTIVE" | sed -E 's/:+/:/' | sed -E 's/:$//' | tr ':' '\n' | sort -u | tr '\n' ':')
	IFS=':' read -a INACTIVE_SPLIT <<< "$INACTIVE"
	unset IFS
	for M in "${INACTIVE_SPLIT[@]}"
	do
		echo "Module '$M'"
		# remove the missing onces from the OK list
		OK_MOD=$(echo "$OK_MOD" | sed 's/:'"$M"':/:/' | sed -E 's/^'"$M"':/:/' | sed -E 's/:'"$M"'$/:/')
	done

	if [ "$1" == "-q" ]; then
		echo $INACTIVE
	fi

	echo -e "\n----------------------------------------"
	echo "########################################"
	echo "----------------------------------------"
	echo -e "\n----------------------------------------"
	echo "########################################"
	echo "----------------------------------------"
fi

echo -e "\n[SUCCESS] Dependencies for the following modules were satisfied:"

# print modules that are ok
OK_MOD=$(echo "$OK_MOD" | sed -E 's/:+/:/' | sed -E 's/^://' | sed -E 's/:$//' | tr ':' '\n' | sort -u | tr '\n' ':')
IFS=':' read -a OK_MOD_SPLIT <<< "$OK_MOD"
unset IFS
for M in "${OK_MOD_SPLIT[@]}"
do
	echo "Module '$M'"
done

if [ $MISSING -eq 1 ]; then
	exit $EXIT_TOOLS_MISSING
else
	exit 0
fi
