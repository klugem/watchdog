package de.lmu.ifi.bio.watchdog.executionWrapper;

import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds module specific files for usage in wrappers
 * @author kluge
 *
 */
public class FindVersionedFile {
	
	public final static String MOD_VERSION = ".v([0-9])+";
	private final String MOD_VERSION_SUFFIX;
	private final Pattern MOD_VERSION_PATTERN; 
	
	private final ConcurrentHashMap<File, File> CACHE = new ConcurrentHashMap<>();
	private final FileFilter FILTER;
	private final String FILENAME_SUFFIX;
	
	/**
	 * 
	 * @param filter filename filter selection the files of interest
	 * @param suffixBeforeVersion suffix of the files before the version information
	 */
	public FindVersionedFile(FileFilter filter, String suffixBeforeVersion) {
		this.FILTER = filter;
		this.FILENAME_SUFFIX = suffixBeforeVersion;
		
		this.MOD_VERSION_SUFFIX = (MOD_VERSION + this.FILENAME_SUFFIX).replaceAll("\\.", "\\\\.");
		this.MOD_VERSION_PATTERN = Pattern.compile(".+" + MOD_VERSION_SUFFIX);
	}

	/**
	 * tests if a file matching the filename pattern and optionally containing a module version info exists 
	 * @param folder
	 * @return
	 */
	public File find(File folder, Integer moduleVersion) {
		if(folder != null && !this.CACHE.containsKey(folder)) {
			File[] yml = folder.listFiles(this.FILTER);
			int ok = 0;
			int okNoVersion = 0;
			File lastOK = null;
			File lastNoVersion = null;
			for(File y : yml) {
				Matcher m = MOD_VERSION_PATTERN.matcher(y.getName());
				
				// try to find specific file depending on module version 
				if(moduleVersion != null) {
					if(m.matches() && Integer.parseInt(m.group(1)) == moduleVersion) {
						lastOK = y;
						ok++;
					}
					else if(!m.matches()) {
						lastNoVersion = y;
						okNoVersion++;
					}
				} // generic file
				else if(!m.matches()) {
					lastNoVersion = y;
					okNoVersion++;
				}
			}
			// found one specific hit
			if(moduleVersion != null && ok == 1) 
				this.CACHE.put(folder, lastOK);
			else if(okNoVersion == 1)
				this.CACHE.put(folder, lastNoVersion);
		}
		return this.CACHE.get(folder);
	}
}
