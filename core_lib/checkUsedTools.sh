#!/bin/bash
SCRIPT_FOLDER=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_FOLDER/includeBasics.sh
PERL_MODE=0
R_MODE=0
FAILED=0

# check, if parameters are ok
if [ $# -ne 1 ] && [ $# -ne 2 ]  && [ "${#1}" -ge 1 ]; then
	echo "[ERROR] Illegal number of parameters! First must name of programs which should be checked separated by ':'. If first argument is 'perl' followed by a list of perl modules separated by '|' then it checks, if these perl modules are installed. If second parameter is set the requirements for shflags.sh are checked."
	exit $EXIT_MISSING_ARGUMENTS
else 
	if [ $# -eq 2 ]; then
		USED_TOOLS="$1:grep:getopt:awk:sed:uname"
	else
		USED_TOOLS=$1
	fi

	if [[ "$USED_TOOLS" =~ "^" ]]; then
		# check, if first argument is perl
		if [[ "$USED_TOOLS" =~ "^perl" ]]; then
			PERL_MODE=1
			USED_TOOLS=${USED_TOOLS#^perl|}
		fi
		if  [[ "$USED_TOOLS" =~ "^R" ]]; then
			R_MODE=1
			USED_TOOLS=${USED_TOOLS#^R|}
		fi
	else
		if [ ! -z "$USED_TOOLS_FUNCTIONS" ] && [ "${#USED_TOOLS_FUNCTIONS}" -ge 1 ] ; then
			USED_TOOLS=$USED_TOOLS:$USED_TOOLS_FUNCTIONS
		fi
	fi
fi

if [ $PERL_MODE -eq 0 ] && [ $R_MODE -eq 0 ]; then
	# read input arguments into array
	IFS=':' read -a USED_TOOLS <<< "$USED_TOOLS"
	unset IFS
	eval USED_TOOLS=($(printf "%q\n" "${USED_TOOLS[@]}" | sort -u))

	# check each of the tools
	for BIN_NAME in "${USED_TOOLS[@]}"
	do
		BIN_PATH=$({ which "$BIN_NAME"; } 2>&1)
		NOT_RET=$?
		NOT=$({ echo $BIN_PATH | grep -E "^which: no $BIN_NAME" -c; } 2>&1)
		NOT=$((NOT+NOT_RET))

		if [ $NOT -ne 0 ]; then
			echoError "Could not find binary '$BIN_NAME'."
			FAILED=1
		fi
	done
else 
	IFS='|' read -a MODULES <<< "$USED_TOOLS"
	unset IFS
	eval MODULES=($(printf "%q\n" "${MODULES[@]}" | sort -u))

	if [ $PERL_MODE -eq 1 ]; then
		# try to load perl
		BIN_PATH=$({ which "perl"; } 2>&1)
		NOT_RET=$?
		NOT=$({ echo $BIN_PATH | grep -E "^which: no perl" -c; } 2>&1)
		NOT=$((NOT+NOT_RET))

		if [ $NOT -ne 0 ]; then
			echoError "Could not find 'perl' binary."
			FAILED=1
		else
			# check each of the modules
			for M in "${MODULES[@]}"
			do
				RET=$({ perl -I "$SCRIPT_FOLDER/perl" -M${M} -e 1; } 2>&1)
				CODE=$?
			echo $RET
				if [ $CODE -ne 0 ] || [ "$RET" != "" ] ; then
					echoError "Could not load perl module '$M'."
					FAILED=1
				fi
			done
		fi
	fi
	if [ $R_MODE -eq 1 ]; then
		# try to load R
		BIN_PATH=$({ which "Rscript"; } 2>&1)
		NOT_RET=$?
		NOT=$({ echo $BIN_PATH | grep -E "^which: no Rscript" -c; } 2>&1)
		NOT=$((NOT+NOT_RET))

		if [ $NOT -ne 0 ]; then
			echoError "Could not find 'Rscript' binary."
			FAILED=1
		else
			# get a list of all installed modules
			TMP_FILE1=$(getTmpFile)
			TMP_FILE2=$(getTmpFile)
			TMP_FILE3=$(getTmpFile)
			echo 'args <- commandArgs(trailingOnly = F)' > "$TMP_FILE1"
			echo 'print(paste("called binary:", args[1]))' >> "$TMP_FILE1"
			echo 'print(version)' >> "$TMP_FILE1"
			echo 'write.table(file = "'$TMP_FILE2'", rownames(installed.packages()), quote=F, row.names=F, col.names=F)' >> "$TMP_FILE1"
			echo 'q()' >>  "$TMP_FILE1"
			Rscript "$TMP_FILE1" 2>&1 > "$TMP_FILE3"
			
			if [ $? -ne 0 ]; then
				echoError "Failed to get list of installed modules."
				echoError "Check temporary file for more information: '$TMP_FILE3'"
			fi

			# check each of the modules
			for M in "${MODULES[@]}"
			do
				RET=$({ grep -E -c "^$M\$" "$TMP_FILE2"; } 2>&1)
				CODE=$?
				if [ $CODE -ne 0 ] || [ "$RET" == "0" ] ; then
					echoError "Could not find R module '$M'."
					FAILED=1
				fi
			done
			if [ $FAILED -ne 1 ]; then
				rm "$TMP_FILE1" 2>&1
				rm "$TMP_FILE2" 2>&1
				rm "$TMP_FILE3" 2>&1
			else
				echoError "Check temporary files for more information: '$TMP_FILE2' '$TMP_FILE3'"
			fi
		fi
	fi 
fi

# check if all binaries were found
if [ $FAILED -eq 1 ]; then
	exit $EXIT_FAILED
else
	exit $EXIT_OK
fi
