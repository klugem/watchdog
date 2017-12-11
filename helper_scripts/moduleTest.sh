#!/bin/bash
SCRIPT_FOLDER=$(cd $(dirname $(readlink -f "${BASH_SOURCE[0]}" 2>/dev/null || readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")) && pwd -P)
source $SCRIPT_FOLDER/../core_lib/includeBasics.sh

# make dependency test first
echo "Checking which modules can be tested..."
INACTIVE_WATCHDOG_MODULES=$($SCRIPT_FOLDER/dependencyTest.sh -q 2> /dev/null | tail -n 1) # in quiet mode

# find all scripts to test modules
for I in `find . $SCRIPT_FOLDER/../modules -maxdepth 2 -name "test_*.sh" -type f | sort`;
do
	NAME=$(basename $(dirname $I))
	COUNT_INACTIVE=$(echo "$INACTIVE_WATCHDOG_MODULES" | grep -E -c "^${NAME}:|:${NAME}:|:${NAME}:?\$")
	if [ "$COUNT_INACTIVE" -eq "0" ]; then
		echo "-----------------------------------------------"
		echo -n "Testing module '$NAME'...";
		RET=$($I)
		ERRORS=$?
		RET=$(echo $RET | sed 's#\[#\\n\[#g')
		echo -e $RET
		echo "-----------------------------------------------"
		echo "Module '$NAME' was tested with $ERRORS errors.";
		echo -e ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n"
		if [ $ERRORS -gt 0 ]; then
			FAILED=1
			INACTIVE_TEST="$NAME:$INACTIVE_TEST"
		else 
			OK_TEST="$NAME:$OK_TEST"
		fi
	fi
done

echo -e "\n----------------------------------------"
echo "########################################"
echo "########################################"
echo "########################################"
echo "----------------------------------------"
echo "The following modules were tested successfully:"
OK_TEST=$(echo "$OK_TEST" | sed -E 's/:$//' | tr ':' '\n' | sort -u | tr '\n' ':')
IFS=':' read -a OK_TEST <<< "$OK_TEST"
unset IFS
for M in "${OK_TEST[@]}"
do
	echo "Module '$M'"
done
echo -e "########################################\n"

echo "########################################"
echo "The following modules can not be used because of missing dependencies:"
INACTIVE_WATCHDOG_MODULES=$(echo "$INACTIVE_WATCHDOG_MODULES" | sed -E 's/:$//' | tr ':' '\n' | sort -u | tr '\n' ':')
IFS=':' read -a INACTIVE_WATCHDOG_MODULES <<< "$INACTIVE_WATCHDOG_MODULES"
unset IFS
for M in "${INACTIVE_WATCHDOG_MODULES[@]}"
do
	echo "Module '$M'"
done
echo  "########################################"

# check, if all was ok or not
if [ $FAILED -eq 1 ]; then
	echo -e "\nThe following modules were tested with errors:"
	INACTIVE_TEST=$(echo "$INACTIVE_TEST" | sed -E 's/:$//' | tr ':' '\n' | sort -u | tr '\n' ':')
	IFS=':' read -a INACTIVE_TEST <<< "$INACTIVE_TEST"
	unset IFS
	for M in "${INACTIVE_TEST[@]}"
	do
		echo "Module '$M'"
	done
	exit $EXIT_TOOLS_MISSING
else
	#echo "[OK] All modules were tested without errors!"
	exit 0
fi
