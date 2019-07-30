package de.lmu.ifi.bio.watchdog.validator.github;

import com.beust.jcommander.Parameter;

public class GithubCheckerParameters {

	@Parameter(names={"-check", "-c"}, description="name of the check that should be performed", required=true)
	protected String check;
	
	@Parameter(names={"-help", "-h", "--help", "--man", "-man"}, description="print usage message and exit", help=true)
	protected boolean help = false;
}