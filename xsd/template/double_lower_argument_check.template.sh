if [ $(echo "$FLAGS_{%VAR_NAME%} > {%VAR_VALUE%}" | bc -l) -eq 1 ] ; then
	echoError "Parameter -{%VAR_NAME_SHORT%} must be lower than {%VAR_VALUE%}. (see --help for details)";
	exit $EXIT_INVALID_ARGUMENTS
fi

