package de.lmu.ifi.bio.watchdog.validator.github.checker;

import java.io.File;

import de.lmu.ifi.bio.watchdog.validator.LocalValidator;
import de.lmu.ifi.bio.watchdog.validator.XSDModuleValidator;
import de.lmu.ifi.bio.watchdog.validator.github.APICompareInfo;
import de.lmu.ifi.bio.watchdog.validator.github.GithubLogEventhandler;

/**
 * Tests if the XSD module file is valid
 * @author kluge
 *
 */
public class GithubXSDChecker extends GithubCheckerIO implements LocalValidator {
	
	protected String localModuleFolder;
	
	public GithubXSDChecker(String name) {
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
			XSDModuleValidator xv = new XSDModuleValidator(intFolder.getAbsolutePath(), new File(this.watchdogBase), new GithubLogEventhandler(this));
			return xv.validate();
		}
		return false;
	}
	
	@Override
	public String getFolderToValidate() {	return this.localModuleFolder;	}
	@Override
	public void setFolderToValidate(String absDir) { this.localModuleFolder = absDir; }
}
