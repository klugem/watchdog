if [ "$FLAGS_{%VAR_NAME%}" -lt {%VAR_VALUE%} ] ; then
	echoError "Parameter -{%VAR_NAME_SHORT%} must be greater than {%VAR_VALUE%}. (see --help for details)";
	exit $EXIT_INVALID_ARGUMENTS
fi

