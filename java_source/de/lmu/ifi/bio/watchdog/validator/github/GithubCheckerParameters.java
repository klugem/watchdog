package de.lmu.ifi.bio.watchdog.validator.github;

import com.beust.jcommander.Parameter;

import de.lmu.ifi.bio.watchdog.runner.DescriptionParameters;

/**
 * Dummy class for generics
 * @author kluge
 *
 */
public abstract class GithubCheckerParameters extends DescriptionParameters {
	@Parameter(names={"-check", "-c"}, description="name of the check that should be performed")
	protected String check;
	
	@Parameter(names={"-list", "-l"}, description="list checks that can be performed locally")
	protected boolean list;
	
	@Parameter(names={"-folder", "-f"}, description="path to a folder on which the test should be applied locally")
	protected String folder;
	
	@Parameter(names={"-watchdogBase", "-w"}, description="uses a different watchdog installation directory; default: ${jarLocation}/..")
	protected String watchdogBase;
	
	@Parameter(names={"-help", "-h", "--help", "--man", "-man"}, description="print usage message and exit", help=true)
	protected boolean help = false;
}