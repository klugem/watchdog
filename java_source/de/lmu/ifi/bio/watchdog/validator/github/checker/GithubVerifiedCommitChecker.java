package de.lmu.ifi.bio.watchdog.validator.github.checker;

import de.lmu.ifi.bio.watchdog.validator.github.APICommitInfo;

/**
 * Tests if commit was verified by a GPG key
 * @author kluge
 *
 */
public class GithubVerifiedCommitChecker extends GithubCheckerBase {

	public static final String VER = "verification";
	public static final String VERIFIED = "verified";
	
	public GithubVerifiedCommitChecker(String name) {
		super(name);
	}

	@Override
	public boolean test() {
		super.test();
		boolean ret = true;
		APICommitInfo info;
		try {
			info = new APICommitInfo(this.TRAVIS_INFO.getFullOriginRepoName(), this.TRAVIS_INFO.getSHA());
		} catch(Exception e) {
			this.error("Failed to make API call!");
			e.printStackTrace();
			return false;
		}
	
		if(info.DATA.getCommit().getVerification().getVerified()) {
			this.info("Commit is verified.");
			return true;
		}
		this.info("Commit is not verified!");
		return false;
	}
}