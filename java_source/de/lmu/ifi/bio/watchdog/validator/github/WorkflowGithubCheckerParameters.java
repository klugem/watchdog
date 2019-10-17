package de.lmu.ifi.bio.watchdog.validator.github;

public class WorkflowGithubCheckerParameters extends GithubCheckerParameters {
	@Override
	public String getDescription() {
		return "Validates that workflows fulfil specific test criteria. "
			   +"Mainly it ensures that XML workflow files are syntactically correct.";
	}
}
