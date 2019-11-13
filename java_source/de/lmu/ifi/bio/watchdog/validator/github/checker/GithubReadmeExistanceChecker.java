package de.lmu.ifi.bio.watchdog.validator.github.checker;

import java.io.File;

import de.lmu.ifi.bio.watchdog.validator.LocalValidator;
import de.lmu.ifi.bio.watchdog.validator.github.APICompareInfo;

/**
 * Tests if the XML workflow file is valid using all modules part of the public repository
 * @author kluge
 *
 */
public class GithubReadmeExistanceChecker extends GithubCheckerIO implements LocalValidator {

	public static final String README_NAME = "README.md";
	protected String localModuleFolder;
	
	public GithubReadmeExistanceChecker(String name) {
		super(name);
	}

	@Override
	public boolean test(){
		if(super.test()) {
			String baseFolder;
			if(!this.isLocalTestMode()) {
				APICompareInfo info;
				try { 
					info = new APICompareInfo(this.TRAVIS_INFO.getFullBuildRepoName(), DEFAULT_BRANCH, this.TRAVIS_INFO.getSHA());
					if(info.hasModuleFolder())
						baseFolder = info.getModuleFolder();
					else {
						this.error("Failed to get the module name from the pull request.");
						return false;
					}
				} catch(Exception e) {
					this.error("Failed to make API call!");
					e.printStackTrace();
					return false;
				}
			} else {
				baseFolder= this.getFolderToValidate();
			}
			File intFolder = new File((!this.isLocalTestMode() ? this.GIT_CLONE_DIR + File.separator : "") + baseFolder);
			File readme = new File(intFolder.getAbsolutePath() + File.separator + README_NAME);
			boolean ret = intFolder.exists() && readme.exists();
			if(!ret) {
				this.error("Failed to find file with name '" + README_NAME + "'.");
			}
			return ret;
		}
		return false;
	}
	
	@Override
	public String getFolderToValidate() {	return this.localModuleFolder;	}
	@Override
	public void setFolderToValidate(String absDir) { this.localModuleFolder = absDir; }
}
