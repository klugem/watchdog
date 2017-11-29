if [ -z "$FLAGS_{%VAR_NAME%}" ]; then
	echoError "Parameter -{%VAR_NAME_SHORT%} must be set. (see --help for details)";
	exit $EXIT_MISSING_ARGUMENTS
fi


