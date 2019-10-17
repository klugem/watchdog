package de.lmu.ifi.bio.watchdog.validator.github;

import de.lmu.ifi.bio.watchdog.validator.github.checker.GithubSeparateFolderChecker;
import de.lmu.ifi.bio.watchdog.validator.github.checker.GithubVerifiedCommitChecker;
import de.lmu.ifi.bio.watchdog.validator.github.checker.GithubXMLValidatorChecker;

/**
 * Checks, that are run by travis-ci on github workflow pull-requests
 * @author kluge
 *
 */
public class WorkflowGithubCheckerRunner extends GithubCheckerRunner<WorkflowGithubCheckerParameters> {
	
	/** names for tests that can be performed */
	public final static String SIGNED_COMMIT_TEST = "SIGNED_COMMIT";
	public final static String SEPARATE_FOLDER_TEST = "SEPARATE_FOLDER";
	public final static String XML_VALIDATION_TEST = "XML_VALIDATION";
	
	// set the test names that are valid
	static {
		VALID_TEST_NAMES.put(SIGNED_COMMIT_TEST, new GithubVerifiedCommitChecker(SIGNED_COMMIT_TEST));
		VALID_TEST_NAMES.put(SEPARATE_FOLDER_TEST, new GithubSeparateFolderChecker(SEPARATE_FOLDER_TEST, false));
		VALID_TEST_NAMES.put(XML_VALIDATION_TEST, new GithubXMLValidatorChecker(XML_VALIDATION_TEST));
	}
	
	@Override
	public WorkflowGithubCheckerParameters getParamInstance() {
		return new WorkflowGithubCheckerParameters();
	}
	
	/**
	 * Runner method
	 * @param args
	 */
	public static void main(String[] args) {		
		WorkflowGithubCheckerRunner m = new WorkflowGithubCheckerRunner();
		m.run(args);
	}
}
