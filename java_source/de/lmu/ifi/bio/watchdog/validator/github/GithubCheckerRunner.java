package de.lmu.ifi.bio.watchdog.validator.github;

import java.util.ArrayList;
import java.util.HashMap;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import de.lmu.ifi.bio.watchdog.validator.github.checker.GithubCheckerBase;
import de.lmu.ifi.bio.watchdog.validator.github.checker.GithubDocuChecker;
import de.lmu.ifi.bio.watchdog.validator.github.checker.GithubPermissionChecker;
import de.lmu.ifi.bio.watchdog.validator.github.checker.GithubSingleModuleChecker;
import de.lmu.ifi.bio.watchdog.validator.github.checker.GithubVerifiedCommitChecker;
import de.lmu.ifi.bio.watchdog.validator.github.checker.GithubXSDChecker;

/**
 * Checks, that are run by travis-ci on github pull-requests
 * @author kluge
 *
 */
public class GithubCheckerRunner {
	
	/** names for tests that can be performed */
	public final static String SIGNED_COMMIT_TEST = "SIGNED_COMMIT";
	public final static String WRITE_PERMISSION_TEST = "WRITE_PERMISSION";
	public final static String XSD_VALIDATION_TEST = "XSD_VALIDATION";
	public final static String SINGLE_MODULE_TEST = "SINGLE_MODULE";
	public final static String XML_DOCUMENTATION_TEST = "XML_DOCUMENTATION";
	
	/** exit codes of the checker */
	public static int EXIT_OK = 0;
	public static int EXIT_CHECK_FAILED = 1;
	public static int EXIT_RUNNER_FAILURE = 2;
	public static int EXIT_CHECKER_FAILURE = 3;
	
	/**
	 * contains valid tests
	 */
	public final static HashMap<String, GithubCheckerBase> VALID_TEST_NAMES = new HashMap<>();
	static {
		VALID_TEST_NAMES.put(SIGNED_COMMIT_TEST, new GithubVerifiedCommitChecker(SIGNED_COMMIT_TEST));
		VALID_TEST_NAMES.put(WRITE_PERMISSION_TEST, new GithubPermissionChecker(WRITE_PERMISSION_TEST));
		VALID_TEST_NAMES.put(XSD_VALIDATION_TEST, new GithubXSDChecker(XSD_VALIDATION_TEST));
		VALID_TEST_NAMES.put(SINGLE_MODULE_TEST, new GithubSingleModuleChecker(SINGLE_MODULE_TEST));
		VALID_TEST_NAMES.put(XML_DOCUMENTATION_TEST, new GithubDocuChecker(XML_DOCUMENTATION_TEST));
	}
	
	/**
	 * Runner method
	 * @param args
	 */
	public static void main(String[] args) {
		// try to parse the parameters
		GithubCheckerParameters params = new GithubCheckerParameters();
		JCommander parser = null;
		try { 
			parser = new JCommander(params, args); 
		}
		catch(ParameterException e) { 
			e.getMessage();
			new JCommander(params).usage();
			System.exit(EXIT_RUNNER_FAILURE);
		}
		
		// display the help
		if(params.help) {
			parser.usage();
			System.exit(EXIT_OK);
		}
		else {
			// perform the check
			if(VALID_TEST_NAMES.containsKey(params.check)) {
				String checkName = params.check;
				
				// get the checker class
				GithubCheckerBase checker = VALID_TEST_NAMES.get(checkName);
				
				// perform the check
				try {
					boolean returnValue = checker.test();
					
					// print the info/errors
					printCheckInfoError(checker.getMessages());
					
					// exit the check with the correct exit code
					if(returnValue) {
						System.exit(EXIT_OK);
					}
					else {
						System.exit(EXIT_CHECK_FAILED);
					}
				}
				catch(Exception e) {
					error("Github checker threw an exception:");
					e.printStackTrace();
					System.exit(EXIT_CHECKER_FAILURE);
				}
			}
			else {
				error("Check with name '"+params.check+"' no known!");
				System.exit(EXIT_RUNNER_FAILURE);
			}
		}
		
		// here no call should exit
		System.exit(EXIT_RUNNER_FAILURE);
	}
		
	/**
	 * prints the info from checkers
	 * @param ineo
	 */
	public static void printCheckInfoError(ArrayList<String> ineo) {
		for(String s : ineo) {
			System.out.println(s);
		}
	}
	
	/**printCheckInfoError
	 * prints an error
	 * @param error
	 */
	public static void error(String error) {
		System.out.println("[ERROR] " + error);
	}

	/**
	 * prints an info
	 * @param info
	 */
	public static void info(String info) {
		System.out.println("[INFO] " + info);
	}
}
