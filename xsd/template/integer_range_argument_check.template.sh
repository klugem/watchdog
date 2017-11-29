if [ "$FLAGS_{%VAR_NAME%}" -gt {%MAX%} ] || [ "$FLAGS_{%VAR_NAME%}" -lt {%MIN%} ]; then
	echoError "Parameter -{%VAR_NAME_SHORT%} must be between [{%MIN%}, {%MAX%}]. (see --help for details)";
	exit $EXIT_INVALID_ARGUMENTS
fi

