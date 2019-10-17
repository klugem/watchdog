package de.lmu.ifi.bio.watchdog.validator.github.checker;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import de.lmu.ifi.bio.watchdog.validator.github.APICompareInfo;

/**
 * Tests if all the files that should be changed are located in one folder
 * @author kluge
 *
 */
public class GithubSeparateFolderChecker extends GithubCheckerBase {
	
	protected final boolean IS_MODULE_TEST;
	
	public GithubSeparateFolderChecker(String name, boolean isModuleTest) {
		super(name);
		this.IS_MODULE_TEST = isModuleTest;
	}

	@Override
	public boolean test(){
		super.test();
		boolean ret = true;
		APICompareInfo info; 
		try {
			info = new APICompareInfo(this.TRAVIS_INFO.getFullBuildRepoName(), DEFAULT_BRANCH, this.TRAVIS_INFO.getSHA());
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
		// ignore shared utils folder
		if(IS_MODULE_TEST && baseDir.size() > 1 && baseDir.containsKey(SHARED_UTILS))
			baseDir.remove(SHARED_UTILS);

		// ensure that exactly one base folder is affected
		if(baseDir.size() > 1) {
			this.error("Only files located in one folder are allowed to be part of a pull request. Detected base folders: '"+ StringUtils.join(baseDir.keySet(), "', '") +"'");
			ret = false;
		}
		else if(baseDir.size() == 0) {
			this.error("The pull request affects no files of a folder.");
			ret = false;
		}
		Integer fc = baseDir.values().stream().mapToInt(Integer::intValue).sum();
		this.info("All "+fc+" files of the pull request belong to the folder '"+info.getModuleFolder()+"'.");
		return ret;
	}
}
