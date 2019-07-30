package de.lmu.ifi.bio.watchdog.validator.github;

import de.lmu.ifi.bio.watchdog.validator.github.autogen.commit.CommitAPIv3;

/**
 * Class that can be used to get information about commits using the github V3 API
 * @author kluge
 *
 */
public class APICommitInfo extends APIRequest<CommitAPIv3> {
	
	public static final String PART1 = "https://api.github.com/repos/";
	public static final String PART2 = "/commits/";
	
	public final CommitAPIv3 DATA;
	private final String REPO;
	private final String SHA;
	
	public APICommitInfo(String repository, String sha) throws Exception {
		this.REPO = repository;
		this.SHA = sha;
		this.DATA = this.makeRequest();
	}

	@Override
	public String getURI() {
		return PART1 + this.REPO + PART2 + this.SHA;
	}

	@Override
	public Class<CommitAPIv3> getJSONClass() {
		return CommitAPIv3.class;
	}
}
