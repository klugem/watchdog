package de.lmu.ifi.bio.watchdog.validator.github;

import de.lmu.ifi.bio.watchdog.validator.github.checker.GithubDocuChecker;
import de.lmu.ifi.bio.watchdog.validator.github.checker.GithubPermissionChecker;
import de.lmu.ifi.bio.watchdog.validator.github.checker.GithubSeparateFolderChecker;
import de.lmu.ifi.bio.watchdog.validator.github.checker.GithubVerifiedCommitChecker;
import de.lmu.ifi.bio.watchdog.validator.github.checker.GithubXSDChecker;

/**
 * Checks, that are run by travis-ci on github module pull-requests
 * @author kluge
 *
 */
public class ModuleGithubCheckerRunner extends GithubCheckerRunner<ModuleGithubCheckerParameters> {
	
	/** names for tests that can be performed */
	public final static String SIGNED_COMMIT_TEST = "SIGNED_COMMIT";
	public final static String WRITE_PERMISSION_TEST = "WRITE_PERMISSION";
	public final static String XSD_VALIDATION_TEST = "XSD_VALIDATION";
	public final static String SEPARATE_FOLDER_TEST = "SEPARATE_FOLDER";
	public final static String XML_DOCUMENTATION_TEST = "XML_DOCUMENTATION";
	
	// set the test names that are valid
	static {
		VALID_TEST_NAMES.put(SIGNED_COMMIT_TEST, new GithubVerifiedCommitChecker(SIGNED_COMMIT_TEST));
		VALID_TEST_NAMES.put(WRITE_PERMISSION_TEST, new GithubPermissionChecker(WRITE_PERMISSION_TEST));
		VALID_TEST_NAMES.put(XSD_VALIDATION_TEST, new GithubXSDChecker(XSD_VALIDATION_TEST));
		VALID_TEST_NAMES.put(SEPARATE_FOLDER_TEST, new GithubSeparateFolderChecker(SEPARATE_FOLDER_TEST, true));
		VALID_TEST_NAMES.put(XML_DOCUMENTATION_TEST, new GithubDocuChecker(XML_DOCUMENTATION_TEST));
	}
	
	@Override
	public ModuleGithubCheckerParameters getParamInstance() {
		return new ModuleGithubCheckerParameters();
	}
	
	/**
	 * Runner method
	 * @param args
	 */
	public static void main(String[] args) {		
		ModuleGithubCheckerRunner m = new ModuleGithubCheckerRunner();
		m.run(args);
	}
}
