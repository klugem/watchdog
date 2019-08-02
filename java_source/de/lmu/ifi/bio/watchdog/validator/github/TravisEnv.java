package de.lmu.ifi.bio.watchdog.validator.github;

/**
 * Parses some of the environment variables that are set by travis-ci
 * Info on environment variables: https://docs.travis-ci.com/user/environment-variables
 * @author kluge
 *
 */
public class TravisEnv {
	
	public static final String ENV_TRAVIS_PULL_REQUEST_SHA = "TRAVIS_PULL_REQUEST_SHA"; // if the current job is a pull request, the commit SHA of the HEAD commit of the PR.
	public static final String ENV_TRAVIS_PULL_REQUEST_SLUG = "TRAVIS_PULL_REQUEST_SLUG"; // if the current job is a pull request, the slug (in the form owner_name/repo_name) of the repository from which the PR originated.
	public static final String ENV_TRAVIS_BUILD_ID = "TRAVIS_BUILD_ID"; // The id of the current build that Travis CI uses internally.
	public static final String ENV_TRAVIS_JOB_ID = "TRAVIS_JOB_ID"; // The id of the current job that Travis CI uses internally.
	public static final String ENV_TRAVIS_REPO_SLUG = "TRAVIS_REPO_SLUG"; // The slug (in form: owner_name/repo_name) of the repository currently being built.
	
	public static final String BASE_URL = "https://travis-ci.com/";		
	public static final String BUILD_URL_PART = "/builds/";
	public static final String JOB_URL_PART = "/jobs/";
	
	private final String TRAVIS_PULL_REQUEST_SHA;
	private final String TRAVIS_PULL_REQUEST_SLUG;
	private final String TRAVIS_BUILD_ID;
	private final String TRAVIS_JOB_ID;
	private final String TRAVIS_REPO_SLUG;
	private final boolean IS_VALID;
		
	// constructor that does the parsing
	public TravisEnv() {
		this.TRAVIS_PULL_REQUEST_SHA = getValue(ENV_TRAVIS_PULL_REQUEST_SHA);
		this.TRAVIS_PULL_REQUEST_SLUG = getValue(ENV_TRAVIS_PULL_REQUEST_SLUG);
		this.TRAVIS_BUILD_ID = getValue(ENV_TRAVIS_BUILD_ID);
		this.TRAVIS_JOB_ID =  getValue(ENV_TRAVIS_JOB_ID);
		this.TRAVIS_REPO_SLUG =  getValue(ENV_TRAVIS_REPO_SLUG);
		
		if(this.getFullOriginRepoName() == null || this.getFullBuildRepoName() == null || this.getSHA() == null || this.TRAVIS_BUILD_ID == null || this.TRAVIS_JOB_ID == null)
			this.IS_VALID = false;
		else
			this.IS_VALID = true;
	}
	
	/**
	 * valid if all required env variables were found
	 * @return
	 */
	public boolean isValid() {
		return this.IS_VALID;
	}
	
	/**
	 * gets the value from the environment variables
	 * @param name
	 */
	protected String getValue(String name) {
		if(System.getenv().containsKey(name))
			return System.getenv(name);
		return null;
	}
	
	/**
	 * SHA of the HEAD commit of the PR
	 * @return
	 */
	public String getSHA() {
		return TRAVIS_PULL_REQUEST_SHA;
	}

	/**
	 * slug (in the form owner_name/repo_name) of the origin repository
	 * @return
	 */
	public String getFullOriginRepoName() {
		return TRAVIS_PULL_REQUEST_SLUG;
	}
	

	/**
	 * slug (in the form owner_name/repo_name) of the repository in with the PR should be integrated
	 * @return
	 */
	public String getFullBuildRepoName() {
		return TRAVIS_REPO_SLUG;
	}
	
	/**
	 * gets the build URL for a test
	 * @return
	 */
	public String getBuildUrl() {
		return BASE_URL + this.getFullBuildRepoName() + BUILD_URL_PART + TRAVIS_BUILD_ID;
	}
	
	/**
	 * gets the job URL for a test
	 * @return
	 */
	public String getJobUrl() {
		return BASE_URL + this.getFullBuildRepoName() + JOB_URL_PART + TRAVIS_BUILD_ID;
	}
}
