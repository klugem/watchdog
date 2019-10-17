package de.lmu.ifi.bio.watchdog.validator.github.checker;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.docu.DocuXMLParser;
import de.lmu.ifi.bio.watchdog.validator.LocalValidator;
import de.lmu.ifi.bio.watchdog.validator.github.APICompareInfo;

/**
 * Tests if watchdog base and git clone directory are set (by travis) and exist
 * AND if tries to locate XSD module definition and XML documentation file. 
 * @author kluge
 *
 */
public abstract class GithubCheckerDocuBased extends GithubCheckerIO implements LocalValidator {
	
	protected APICompareInfo compareInfo;
	protected File xsdFile;
	protected File xmlDocuFile;
	protected String localModuleFolder;

	public GithubCheckerDocuBased(String name) {
		super(name);
	}

	@Override
	public boolean test() {
		if(super.test()) {
			// get the module folder
			String baseFolder;
			if(!this.isLocalTestMode()) {
				try {
					this.compareInfo = new APICompareInfo(this.TRAVIS_INFO.getFullBuildRepoName(), DEFAULT_BRANCH, this.TRAVIS_INFO.getSHA());
				} catch(Exception e) {
					this.error("Failed to make API call!");
					e.printStackTrace();
					return false;
				}
				if(!this.compareInfo.hasModuleFolder()) {
					this.error("Failed to get the module name from the pull request.");
					return false;
				}
				else 
					baseFolder =  this.compareInfo.getModuleFolder();
			}
			else {
				baseFolder = this.getFolderToValidate();
			}
			
			// find XML documentation file
			ArrayList<String> folderOfInterest = new ArrayList<>();
			folderOfInterest.add((!this.isLocalTestMode() ? this.GIT_CLONE_DIR + File.separator : "") + baseFolder);
			ArrayList<Pair<File, File>> res = DocuXMLParser.findAllDocumentedModules(this.watchdogBase, folderOfInterest, true);
			
			// check if a combination was found
			if(res.size() == 0) {
				this.error("No XML documentation file was found in the module folder '"+baseFolder+"'.");
				return false;
			}
			else if(res.size() > 1) {
				this.error("More than one XSD or documentation file was found in the module folder '"+baseFolder+"'.");
				return false;
			}
			else {
				this.xsdFile = res.get(0).getRight();
				this.xmlDocuFile = res.get(0).getLeft();
				return true;
			}
		}
		return false;
	}	
	
	@Override
	public String getFolderToValidate() {	return this.localModuleFolder;	}
	@Override
	public void setFolderToValidate(String absDir) { this.localModuleFolder = absDir; }
}
