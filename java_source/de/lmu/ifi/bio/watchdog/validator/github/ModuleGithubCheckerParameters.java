package de.lmu.ifi.bio.watchdog.validator.github;

public class ModuleGithubCheckerParameters extends GithubCheckerParameters {
	@Override
	public String getDescription() {
		return "Validates that modules fulfil specific test criteria. "
			   +"Mainly it ensures that XSD and XML files are syntactically correct and are compatible to each other.";
	}
}
