package de.lmu.ifi.bio.watchdog.validator.github.checker;

import java.util.ArrayList;

import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.validator.github.GithubCheckerRunner;
import de.lmu.ifi.bio.watchdog.validator.github.TravisEnv;

/**
 * Base class for github checker classes
 * @author kluge
 *
 */
public abstract class GithubCheckerBase {
	
	public final ArrayList<String> MESSAGES = new ArrayList<>();
	protected final TravisEnv TRAVIS_INFO = new TravisEnv();
	private final String NAME;
	protected final String GIT_CLONE_DIR = System.getenv("TRAVIS_BUILD_DIR");
	protected final String WATCHDOG_BASE = System.getenv("WATCHDOG_BASE");
	protected static final String DEFAULT_BRANCH = "master";
	private static final int ERROR_STREAM_FILTER_DURATION = 30; // filter longer as API calls might take their time
	
	/**
	 * Constructor
	 * @param name
	 */
	public GithubCheckerBase(String name) {
		if(!this.TRAVIS_INFO.isValid()) {
			GithubCheckerRunner.error("Failed to load required environment variable from travis.");
			System.exit(1);
		}
		
		this.NAME = name;
		Functions.filterErrorStream(ERROR_STREAM_FILTER_DURATION);
	}
	
	/**
	 * Name of the checker
	 * @return
	 */
	public String getName() {
		return this.NAME;
	}
	
	 /** method a github checker must implement
	 * @return
	 */
	public abstract boolean test();
	
	public ArrayList<String> getMessages() {
		return MESSAGES;
	}
	
	/**
	 * adds an error message
	 * @param error
	 */
	public void error(String error) {
		this.MESSAGES.add("[ERROR] " + error);
	}

	/**
	 * adds an info message
	 * @param info
	 */
	public void info(String info) {
		this.MESSAGES.add("[INFO] " + info);
	}
}
