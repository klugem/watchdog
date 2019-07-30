package de.lmu.ifi.bio.watchdog.validator.github;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.HashMap;

/**
 * Class that updates the pull request status on github
 * @author kluge
 *
 */
@Deprecated
public class PRStatusUpdater extends GithubAPIPostRequester {
	private static final String PART1 = "https://api.github.com/repos/";
	private static final String PART2 = "/statuses/";
	
	/**
	 * generates a new instance of this class
	 * @param repository
	 * @param sha
	 * @return
	 * @throws URISyntaxException
	 */
	public static PRStatusUpdater getInstance(String repository, String sha) throws URISyntaxException {
		String url = PART1 + repository + PART2 + sha;
		return new PRStatusUpdater(url);
	}
	
	/**
	 * hide of constructor
	 * @param url
	 * @throws URISyntaxException
	 */
	private PRStatusUpdater(String url) throws URISyntaxException {
		super(url);
	}

	/**
	 * 
	 * @param name
	 * @param description
	 * @param state
	 * @param detailURL
	 * @return
	 */
	private HashMap<String, String> preparePostArguments(String name, String description, TestState state, String detailURL) {
		HashMap<String, String> params = new HashMap<>();
		params.put("context", name);
		params.put("description", description);
		params.put("state", state.toString());
		params.put("target_url", detailURL);
		return params;
	}
	
	/**
	 * sends an PR status update request and returns the HTTP response
	 * @param name
	 * @param description
	 * @param state
	 * @param detailURLs
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public HttpResponse<String> updatePRTestStatus(String name, String description, TestState state, String detailURLs) throws IOException, InterruptedException {
		return this.prepareAndSendRequest(this.preparePostArguments(name, description, state, detailURLs));
	}
}
