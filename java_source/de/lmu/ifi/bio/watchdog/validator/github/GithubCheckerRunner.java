package de.lmu.ifi.bio.watchdog.validator.github;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import de.lmu.ifi.bio.watchdog.runner.BasicRunner;
import de.lmu.ifi.bio.watchdog.validator.LocalValidator;
import de.lmu.ifi.bio.watchdog.validator.github.checker.GithubCheckerBase;

/**
 * Checks, that are run by travis-ci on github pull-requests
 * @author kluge
 *
 */
public abstract class GithubCheckerRunner<A extends GithubCheckerParameters> extends BasicRunner {
		
	/** exit codes of the checker */
	public static int EXIT_OK = 0;
	public static int EXIT_CHECK_FAILED = 1;
	public static int EXIT_RUNNER_FAILURE = 2;
	public static int EXIT_CHECKER_FAILURE = 3;
	
	/** contains valid tests */
	public final static HashMap<String, GithubCheckerBase> VALID_TEST_NAMES = new LinkedHashMap<>();
	
	/**
	 * must return a instance of GithubCheckerParameters
	 * @return
	 */
	public abstract A getParamInstance();
	
	public void run(String[] args) {
		// try to parse the parameters
		A params = getParamInstance();
		JCommander parser = null;
		try { 
			parser = new JCommander(params, args); 
		}
		catch(ParameterException e) { 
			e.getMessage();
			new JCommander(params).usage();
			System.out.println(params.getDescription());
			System.exit(EXIT_RUNNER_FAILURE);
		}
	
		// display the help
		if(params.help || (params.check == null && !params.list)) {
			parser.usage();
			System.exit(EXIT_OK);
		}
		else if(params.desc) {
			System.out.println(params.getDescription());
			System.exit(EXIT_OK);
		}
		else if(params.list) {		
			// find all test that can be run locally
			info("Tests that can be run locally in combination with the '-folder' parameter:");
			for(Entry<String, GithubCheckerBase> en : VALID_TEST_NAMES.entrySet()) {
				if(en.getValue() instanceof LocalValidator && !((LocalValidator) en.getValue()).canNOTBeUsedLocally()) {
					info(en.getKey());
				}
			}
			System.exit(EXIT_OK);
		}
		else {
			// perform the check
			if(VALID_TEST_NAMES.containsKey(params.check)) {
				String checkName = params.check;
				
				// get the checker class
				GithubCheckerBase checker = VALID_TEST_NAMES.get(checkName);
				
				// test if test should be performed locally
				if(params.folder != null) {
					// find Watchdog's base to work with
					File b = null;
					if(params.watchdogBase != null && params.watchdogBase.length() > 0)
						b = new File(params.watchdogBase);
					File watchdogBase = findWatchdogBase(b);
					if(watchdogBase == null) {
						error("Failed to find Watchdog's install directory! Please use -w to provide it.");
						System.exit(EXIT_CHECKER_FAILURE);
					}
					
					if(checker instanceof LocalValidator) {
						info("Test '"+ checkName +"' will be run locally on '"+params.folder+"'!");
						((LocalValidator) checker).setFolderToValidate(params.folder);
						checker.setWatchdogBase(watchdogBase.getAbsolutePath());
					}
					else {
						error("Test '"+ checkName +"' can not be run locally!");
						System.exit(EXIT_CHECKER_FAILURE);
					}
				}
				
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
