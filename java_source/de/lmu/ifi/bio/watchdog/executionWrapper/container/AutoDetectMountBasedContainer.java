package de.lmu.ifi.bio.watchdog.executionWrapper.container;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.bio.watchdog.executionWrapper.ExecutionWrapper;
import de.lmu.ifi.bio.watchdog.helper.ReplaceSpecialConstructs;
import de.lmu.ifi.bio.watchdog.helper.graphFolderPath.GraphFolderpath;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.task.Task;

/**
 * Abstract class that tries to detect the required mount points for the container automatically
 * @author kluge
 *
 */
public abstract class AutoDetectMountBasedContainer extends ExecutionWrapper {
	
	private static final Logger LOGGER = new Logger();
	private static final String PATTERN = "^[\"']?(" + ReplaceSpecialConstructs.REGEX_PATH + ")[\"']?$";
	private static final Pattern PATH_PATTERN = Pattern.compile(PATTERN);
	private static final long serialVersionUID = 7099505020419438171L;
	private static final String POTENTIAL_PATH_SEP = "[;,:]";
	private static final int MIN_PATH_DEPTH = 3;
	private static final int MIN_PATH_LENGTH = 8;
	private static final String PATH_END = "[\\/]$";
	private static final String NOT_PATH_SEP_CAHRS = "[^\\/]";
	private static final String FILE_ENDING = "\\." + NOT_PATH_SEP_CAHRS + "+$";

	public AutoDetectMountBasedContainer(String name, String watchdogBaseDir) {
		super(name, watchdogBaseDir);
	}
	
	@Override
	public boolean isPackageManager() { return false; }
	
	private boolean isNOTOnBlackList(String test, Predicate<String> isOnBlacklist) {
		return isOnBlacklist == null || !isOnBlacklist.test(test);
	}
	
	/**
	 * collects path from candidate string that are not matched by the blacklist function
	 * @param candidates
	 * @param allowSplit split at ;,: if complete string does not match
	 * @param isOnBlacklist function that must return true, if string is on blacklist
	 * @return
	 */
	protected ArrayList<String> collectedPath(ArrayList<String> candidates, boolean allowSplit, Predicate<String> isOnBlacklist) {
		Matcher m = null;
		ArrayList<String> collectedPath = new ArrayList<>();
		for(String s : candidates) {
			m = PATH_PATTERN.matcher(s);
			if(m.matches()) {
				String add = this.removeEnding(m.group(1));
				if(this.isNOTOnBlackList(add, isOnBlacklist))
					collectedPath.add(add);
			} else if(allowSplit) {
				// if it does not match, try to split it at sep chars
				String[] tmp = s.split(POTENTIAL_PATH_SEP);
				for(String t : tmp) {
					m = PATH_PATTERN.matcher(t);
					if(m.matches()) {
						String add = m.group(1);
						// heuristic to avoid wrong detections - ensure minimal path depth
						if(add.length() >= MIN_PATH_LENGTH && this.findPathDepth(add) >= MIN_PATH_DEPTH) {
							if(this.isNOTOnBlackList(add, isOnBlacklist))
								collectedPath.add(add);
						}
					}
				}
			}
		}
		return collectedPath;
	}
	
	private String removeEnding(String s) {
		if(s.matches(".+" + PATH_END)) {
			return s;
		}
		else if(s.matches(".+" + FILE_ENDING)) {
			s = s.replaceFirst(FILE_ENDING, "") + File.separator;
			return s;
		}
		return s + File.separator;
	}

	/**
	 * ready to add list of the after script commands
	 * @return
	 */
	protected HashMap<String, String> getMountList(Task t, ArrayList<String> otherPaths, Predicate<String> isOnBlacklist) {
		HashMap<String, String> map = new HashMap<>();
		ArrayList<String> cp = collectedPath(t.getArguments(), true, isOnBlacklist);
		cp.addAll(otherPaths);
		Collections.sort(cp);
		
		// find LCPs
		ArrayList<String> lcps = this.getLCPs(cp);
		for(String l : lcps) {
			if(!isOnBlacklist.test(l)) {
				map.put(l, l);
				LOGGER.info("auto detected mount for "+ t.getID() +": " +l);
			} else {
				LOGGER.info("black listed mount for "+ t.getID() +": " +l);
			}
		}
		return map;
	}
	
	protected int findPathDepth(String t) {
		return t.replaceAll(NOT_PATH_SEP_CAHRS, "").length(); 
	}	
	
	/**
	 * returns a set of LCPs that are not too general
	 * @param paths
	 */
	protected ArrayList<String> getLCPs(ArrayList<String> paths) {
		if(paths.size() == 1)
			return paths;
		
		GraphFolderpath graph = new GraphFolderpath();
		for(String p : paths) {
			graph.addPath(p);
		}
		return graph.getLCPs(MIN_PATH_DEPTH);
	}
}
