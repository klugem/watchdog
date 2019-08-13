package de.lmu.ifi.bio.watchdog.validator.github.checker;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

import de.lmu.ifi.bio.watchdog.docu.DocuXMLParser;
import de.lmu.ifi.bio.watchdog.docu.Moduledocu;

/**
 * Tests, if all files that should be changed by that Pull-request are owned by the user or all files are new
 * @author kluge
 *
 */
public class GithubPermissionChecker extends GithubCheckerDocuBased {
	
	public GithubPermissionChecker(String name) {
		super(name);
	}

	@Override
	public boolean test() {
		if(super.test()) {
			// try to parse the docu
			DocumentBuilderFactory dbf = DocuXMLParser.prepareDBF(this.watchdogBase);
			String baseFolder = this.compareInfo.getModuleFolder();
			Moduledocu md = DocuXMLParser.parseXMLFile(dbf, this.xmlDocuFile, this.xsdFile, true);
			if(md == null) {
				this.error("Faild to parse the XML documentation file '"+ (baseFolder+File.separator+this.xmlDocuFile.getName()) +"'.");
				return false;
			}
			else {
				if(this.compareInfo.DATA.getMergeBaseCommit() != null) {
					String githubUser = this.compareInfo.DATA.getCommits().get(0).getCommitter().getLogin();
					this.info("Author of merged commit is github user '"+githubUser+"'");
					
					// test if module consists of only new files
					if(this.compareInfo.isNewModule()) 
						this.info("New module detected: '"+ baseFolder +"'");
	
					// test if committing user is on the write list
					if(md.getMaintainerUsernames().contains(githubUser)) {
						this.info("Author '"+githubUser+"' has write access to module '"+ baseFolder +"'.");
						return true;
					}
					else {
						this.error("Author '"+githubUser+"' has no write access to module '"+ baseFolder +"'.");
						return false;
					}
				}
				else {
					this.error("Merged base commit of pull request not included in API response.");
					return false;	
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean canNOTBeUsedLocally() {
		return true;
	}
}
