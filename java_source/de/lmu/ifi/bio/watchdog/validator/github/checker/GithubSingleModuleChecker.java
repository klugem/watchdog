package de.lmu.ifi.bio.watchdog.validator.github.checker;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import de.lmu.ifi.bio.watchdog.validator.github.APICompareInfo;

/**
 * Tests if all the files that should be changed are part of one module
 * @author kluge
 *
 */
public class GithubSingleModuleChecker extends GithubCheckerBase {
	
	public GithubSingleModuleChecker(String name) {
		super(name);
	}

	@Override
	public boolean test(){
		boolean ret = true;
		APICompareInfo info; 
		try {
			info = new APICompareInfo(this.TRAVIS_INFO.getFullOriginRepoName(), DEFAULT_BRANCH, this.TRAVIS_INFO.getSHA());
		} catch(Exception e) {
			this.error("Failed to make API call!");
			e.printStackTrace();
			return false;
		}
	
		HashMap<String, Integer> baseDir = info.collectAffectedModulesBases();
		
		if(baseDir.containsKey(null)) {
			this.error("Files located in the root level are not allowed to be modified.");
			ret = false;
		}
		// ensure that exactly one base folder is affected
		if(baseDir.size() > 1) {
			this.error("Only files for one module are allowed to be part of a pull request. Detected base folders: '"+ StringUtils.join(baseDir.keySet(), "', '") +"'");
			ret = false;
		}
		else if(baseDir.size() == 0) {
			this.error("The pull request affects no files of a module folder.");
			ret = false;
		}
		Integer fc = baseDir.values().stream().mapToInt(Integer::intValue).sum();
		this.info("All "+fc+" files of the pull request belong to the module folder '"+info.getModuleFolder()+"'.");
		return ret;
	}
}
