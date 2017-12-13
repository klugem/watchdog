package de.lmu.ifi.bio.watchdog.helper;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

public class PatternFilenameFilter implements FilenameFilter {
	
	private final String PATTERN;
	private final boolean ADD_DIRS;
	private final Pattern P;
	
	public PatternFilenameFilter(String pattern, boolean addAllDirs) {
		// prepare pattern
		pattern = pattern.replace(".", "\\."); // point is just a normal point in a filename
		pattern = pattern.replace("*", ".*"); // star means random characters
		pattern = "^" + pattern + "~{0}$"; // pattern must match the complete filename and can not end with ~
		this.PATTERN = pattern;
		this.ADD_DIRS = addAllDirs;
		this.P = Pattern.compile(this.PATTERN);
	}
	
	public boolean matchesFilename(String filename) {
		return this.P.matcher(filename).matches();
	}
	
	@Override
	public boolean accept(File dir, String name) {
		return (this.ADD_DIRS && new File(dir + File.separator + name).isDirectory()) || this.matchesFilename(name);
	}
}
