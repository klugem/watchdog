package de.lmu.ifi.bio.watchdog.validator.github;

import java.util.HashMap;

import de.lmu.ifi.bio.watchdog.validator.github.autogen.compare.CompareAPIv3;
import de.lmu.ifi.bio.watchdog.validator.github.autogen.compare.File;

/**
 * Class that can be used to get information about compares using the github V3 API
 * @author kluge
 *
 */
public class APICompareInfo extends APIRequest<CompareAPIv3> {
	
	public static final String PART1 = "https://api.github.com/repos/";
	public static final String PART2 = "/compare/";
	public static final String PART3 = "...";
	public static final String ADDED_STATUS = "added";

	public final CompareAPIv3 DATA;
	private final String REPO;
	private final String COMPARE_BRANCH;
	private final String SHA;
	
	/*public static void main(String[] args) throws Exception {
		APICompareInfo info = new APICompareInfo("watchdog-wms/watchdog-wms-modules", "master", "2271ab2bceb0a3b4b1b7c834463320862eb430");
		System.out.println(info.DATA.getMergeBaseCommit());
	}*/
	
	public APICompareInfo(String repository, String compareBranch, String sha) throws Exception {
		this.REPO = repository;
		this.COMPARE_BRANCH = compareBranch;
		this.SHA = sha;
		this.DATA = this.makeRequest();
	}
	

	@Override
	public String getURI() {
		return PART1 + this.REPO + PART2 + this.COMPARE_BRANCH + PART3 + this.SHA;
	}

	@Override
	public Class<CompareAPIv3> getJSONClass() {
		return CompareAPIv3.class;
	}
	
	/**
	 * counts how many files are affected of that PR on the first entry
	 * null if files on the root level are affected
	 * @return
	 */
	public HashMap<String, Integer> collectAffectedModulesBases() {
		// base module is the "first-level" directory
		HashMap<String, Integer> baseDir = new HashMap<>();
		// iterate over all files
		for(File f : this.DATA.getFiles()) {
			java.io.File parentFile = new java.io.File(java.io.File.separator + f.getFilename()).getParentFile();

			String base = null;
			while(parentFile != null && !parentFile.getAbsolutePath().equals(java.io.File.separator)) {
				base = parentFile.getAbsolutePath();
				parentFile = parentFile.getParentFile();
			}
			if(base != null)
				base = base.replaceFirst(java.io.File.separator, "");
			if(!baseDir.containsKey(base))
				baseDir.put(base, 1);
			else {
				baseDir.put(base, baseDir.get(base)+1);
			}
		}
		return baseDir;
	}
	
	
	/**
	 * true, if a module folder could be determined uniquely
	 * @return
	 */
	public boolean hasModuleFolder() {
		return this.getModuleFolder() != null;
	}
	
	/**
	 * returns the module folder if it could be detected
	 * @return
	 */
	public String getModuleFolder() {
	    HashMap<String, Integer> counts = this.collectAffectedModulesBases();
	    if(counts.size() == 1 && !counts.containsKey(null)) {
	    	return (String) counts.keySet().toArray()[0];
	    }
	    return null;
	}

	/**
	 * tests if all files part of that pull request have the status "added"
	 * @return
	 */
	public boolean isNewModule() {
		for(File f : this.DATA.getFiles()) {
			if(!f.getStatus().equals(ADDED_STATUS)) {
				return false;
			}
		}
		return true;
	}
}
