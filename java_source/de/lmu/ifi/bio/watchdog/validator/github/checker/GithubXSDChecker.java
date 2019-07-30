package de.lmu.ifi.bio.watchdog.validator.github.checker;

import java.io.File;

import de.lmu.ifi.bio.watchdog.validator.XSDModuleValidator;
import de.lmu.ifi.bio.watchdog.validator.github.APICompareInfo;
import de.lmu.ifi.bio.watchdog.validator.github.GithubLogEventhandler;

/**
 * Tests if the XSD module file is valid
 * @author kluge
 *
 */
public class GithubXSDChecker extends GithubCheckerIO {
	
	public GithubXSDChecker(String name) {
		super(name);
	}

	@Override
	public boolean test(){
		if(super.test()) {
			APICompareInfo info;
			try { 
				info = new APICompareInfo(this.TRAVIS_INFO.getFullOriginRepoName(), DEFAULT_BRANCH, this.TRAVIS_INFO.getSHA());
			} catch(Exception e) {
				this.error("Failed to make API call!");
				e.printStackTrace();
				return false;
			}
			if(info.hasModuleFolder()) {
				String baseFolder = info.getModuleFolder();
				File intFolder = new File(this.GIT_CLONE_DIR + File.separator + baseFolder);
				XSDModuleValidator xv = new XSDModuleValidator(intFolder.getAbsolutePath(), new File(this.WATCHDOG_BASE), new GithubLogEventhandler(this));
				return xv.validate();
			}
			this.error("Failed to get the module name from the pull request.");
		}
		return false;
	}
}
