package de.lmu.ifi.bio.watchdog.validator.github;

import com.beust.jcommander.Parameter;

import de.lmu.ifi.bio.watchdog.runner.DescriptionParameters;

public class GithubCheckerParameters extends DescriptionParameters {

	@Parameter(names={"-check", "-c"}, description="name of the check that should be performed", required=true)
	protected String check;
	
	@Parameter(names={"-list", "-l"}, description="list checks that can be performed locally")
	protected boolean list;
	
	@Parameter(names={"-moduleFolder", "-m"}, description="path to a module folder on which the test should be applied locally")
	protected String moduleFolder;
	
	@Parameter(names={"-watchdogBase", "-w"}, description="uses a different watchdog installation directory; default: ${jarLocation}/..")
	protected String watchdogBase;
	
	@Parameter(names={"-help", "-h", "--help", "--man", "-man"}, description="print usage message and exit", help=true)
	protected boolean help = false;

	@Override
	public String getDescription() {
		return "Validates that modules fulfil specific test criteria. "
			   +"Mainly it ensures that XSD and XML files are syntactically correct and are compatible to each other.";
	}
}