if [ "$FLAGS_{%VAR_NAME%}" -gt {%VAR_VALUE%} ] ; then
	echoError "Parameter -{%VAR_NAME_SHORT%} must be lower than {%VAR_VALUE%}. (see --help for details)";
	exit $EXIT_INVALID_ARGUMENTS
fi

