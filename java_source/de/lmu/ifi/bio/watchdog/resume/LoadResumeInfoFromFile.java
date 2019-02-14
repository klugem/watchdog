package de.lmu.ifi.bio.watchdog.resume;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * Loads the resume info from a file
 * @author kluge
 *
 */
public class LoadResumeInfoFromFile {

	// TODO: add some checks: file ending, json format check for line
	public static HashMap<Integer, HashMap<String, ResumeInfo>> getResumeInfo(File resumeLogFile) {
		try {
			HashMap<Integer, HashMap<String, ResumeInfo>> info = new HashMap<>();
			// load all the stuff into the memory *diabolic*
			for(String line : Files.readAllLines(Paths.get(resumeLogFile.getAbsolutePath()))) {
				ResumeInfo r = ResumeInfo.parseResumeInfo(line);
				if(r != null) {
					int taskID = r.getTaskID();
					String key = r.getGroupFileName();
					
					if(!info.containsKey(taskID))
						info.put(taskID, new HashMap<String, ResumeInfo>());
					
					HashMap<String, ResumeInfo> put = info.get(taskID);
					put.put(key, r);
				}
			}
			return info;
		}
		catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
}
