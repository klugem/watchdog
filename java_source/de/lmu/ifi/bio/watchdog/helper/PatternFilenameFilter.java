package de.lmu.ifi.bio.watchdog.helper;

import java.io.File;
import java.io.FilenameFilter;

public class PatternFilenameFilter implements FilenameFilter {
	
	private final String PATTERN;
	private final boolean ADD_DIRS;
	
	public PatternFilenameFilter(String pattern, boolean addAllDirs) {
		// prepare pattern
		pattern = pattern.replace(".", "\\."); // point is just a normal point in a filename
		pattern = pattern.replace("*", ".*"); // star means random characters
		pattern = "^" + pattern + "~{0}$"; // pattern must match the complete filename and can not end with ~
		this.PATTERN = pattern;
		this.ADD_DIRS = addAllDirs;
	}
	
	@Override
	public boolean accept(File dir, String name) {
		return (this.ADD_DIRS && new File(dir + File.separator + name).isDirectory()) || name.matches(this.PATTERN);
	}
}
