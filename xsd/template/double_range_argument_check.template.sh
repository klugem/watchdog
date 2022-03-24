if [ $(echo "$FLAGS_{%VAR_NAME%} > {%MAX%}" | bc -l) -eq 1 ] || [ $(echo "$FLAGS_{%VAR_NAME%} < {%MIN%}" | bc -l) -eq 1 ]; then
	echoError "Parameter -{%VAR_NAME_SHORT%} must be between [{%MIN%}, {%MAX%}]. (see --help for details)";
	exit $EXIT_INVALID_ARGUMENTS
fi

