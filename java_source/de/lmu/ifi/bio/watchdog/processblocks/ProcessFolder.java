package de.lmu.ifi.bio.watchdog.processblocks;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.apache.commons.io.comparator.NameFileComparator;

import de.lmu.ifi.bio.watchdog.helper.PatternFilenameFilter;
import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Stores information about some process groups which are basically folders which can be restricted to a filename pattern.
 * @author Michael Kluge
 *
 */
public class ProcessFolder extends ProcessBlock {
	
	private static final long serialVersionUID = 357505272230166850L;
	private final ArrayList<File> ROOT_PATH = new ArrayList<>();
	private final ArrayList<String> PATTERN = new ArrayList<>();
	private final ArrayList<String> IGNORE = new ArrayList<>();
	private final ArrayList<Integer> MAX_DEPTH = new ArrayList<>();
	private final HashSet<File> FOUND_FILES = new HashSet<>();
	private static final LinkedHashMap<String, String> OFFER_VAR = new LinkedHashMap<>();
	
	// only used as data store for GUI
	public String gui_rootPath;
	public String gui_pattern;
	public String gui_ignorePattern;
	public int gui_maxDepth;
	public boolean gui_disableExistanceCheck;	

	
	static {
		OFFER_VAR.put("{}", "absolute path to the file");
		OFFER_VAR.put("()", "absolute path to the parent folder of the file");
		OFFER_VAR.put("[]", "name of the file");
		OFFER_VAR.put("[n]", "n suffixes of the filename are truncated using . as separator");
		OFFER_VAR.put("(n)", "n suffixes of the parent folder are truncated using / as separator");
		OFFER_VAR.put("([{n, sep}])", "suffixes of the value are truncated using sep as separator (might also be a regex)");
	}
	
	/**
	 * empty constructor, only for internal use!
	 */
	public ProcessFolder() { super("", null, null); }
	
	/**
	 * Constructor
	 * @param path
	 * @param pattern
	 * @param ignore
	 * @param maxDepth
	 */
	public ProcessFolder(String name, File rootPath, String pattern, String ignore, int maxDepth) {
		super(name, null, null);
		this.ROOT_PATH.add(rootPath);
		this.PATTERN.add(pattern);
		this.IGNORE.add(ignore);
		this.MAX_DEPTH.add(maxDepth);
	}
	
	/**
	 * Constructor
	 * @param path
	 * @param pattern
	 * @param ignore
	 * @param maxDepth
	 */
	public ProcessFolder(String name, ArrayList<File> rootPath, ArrayList<String> pattern, ArrayList<String> ignore, ArrayList<Integer> maxDepth) {
		super(name, null, null);
		if(!(rootPath.size() == pattern.size() && rootPath.size() == maxDepth.size() && rootPath.size() > 0)) {
			throw new IllegalArgumentException("Size of the three ArrayLists must be equal!");
		}
		this.ROOT_PATH.addAll(rootPath);
		this.PATTERN.addAll(pattern);
		this.IGNORE.addAll(ignore);
		this.MAX_DEPTH.addAll(maxDepth);
	}
	
	public ProcessFolder(String name, String rootPath, String baseName, String pattern, String ignore, int maxDepth, boolean append, boolean disableExistanceCheck) {
		super(name, rootPath, baseName);
		this.gui_rootPath = this.getSplitPath(); // calculated by parent class;
		this.gui_pattern = pattern;
		this.gui_ignorePattern = ignore;
		this.gui_maxDepth = maxDepth;
		this.gui_append = append;
		this.gui_disableExistanceCheck = disableExistanceCheck;
		
		if(this.gui_basename != null && this.gui_basename.length() > 0) {
			this.gui_rootPath = this.gui_basename + File.separator + this.gui_rootPath;
		}
	}

	/**
	 * adds a pattern to the process folder
	 * @param rootPath
	 * @param pattern
	 * @param ignore
	 * @param maxDepth
	 */
	public void append(File rootPath, String pattern, String ignore, int maxDepth) {
		this.ROOT_PATH.add(rootPath);
		this.PATTERN.add(pattern);
		this.IGNORE.add(ignore);
		this.MAX_DEPTH.add(maxDepth);
	}
	
	/**
	 * finds recursively files in a folder
	 * @param parent
	 * @param pattern
	 * @param ignore
	 * @param maxDepth
	 * @param newfoundFiles
	 * @param getOnlyNewOnces
	 * @param depth
	 */
	private void findMatchingFiles(File parent, String pattern, String ignore, int maxDepth, ArrayList<File> newfoundFiles, boolean getOnlyNewOnces, int depth) {
		PatternFilenameFilter patternMatcher = new PatternFilenameFilter(pattern, true);
		PatternFilenameFilter ignoreMatcher = null;
		if(ignore != null)
			ignoreMatcher = new PatternFilenameFilter(ignore, false);
		if(parent.exists() && parent.isDirectory() && parent.canRead()) {
			// get files to ignore
			HashSet<String> ignoreFiles = new HashSet<>();
			if(ignoreMatcher != null) {
				for(File f: parent.listFiles(ignoreMatcher)) {
					ignoreFiles.add(f.getAbsolutePath());
				}
			}

			// run through all file
			for(File f : parent.listFiles(patternMatcher)) {
				if(!ignoreFiles.contains(f.getAbsolutePath())) {
					// check, if file is already in HashSet
					if(f.canRead()) {
						if(f.isFile()) {
							if(!this.FOUND_FILES.contains(f) || !getOnlyNewOnces) {
								newfoundFiles.add(f);
								this.FOUND_FILES.add(f);
							}
						}
						else if(depth < maxDepth && f.isDirectory() && f.canExecute())
							this.findMatchingFiles(f, pattern, ignore, maxDepth, newfoundFiles, getOnlyNewOnces, depth+1);
					}
				}
			}
		}
	}
	
	@Override
	public LinkedHashMap<String, String> getValues() {
		ArrayList<File> files = new ArrayList<>();
		// find all matching files for all different process folders
		for(int i = 0; i < this.ROOT_PATH.size(); i++) {
			this.findMatchingFiles(this.ROOT_PATH.get(i), this.PATTERN.get(i), this.IGNORE.get(i), this.MAX_DEPTH.get(i), files, false, 0);
		}
		File[] fs = files.toArray(new File[0]);
		Arrays.sort(fs, NameFileComparator.NAME_COMPARATOR);
		LinkedHashMap<String, String> v = new LinkedHashMap<>();
		for(File f : fs) {
			v.put(f.getAbsolutePath(), f.getAbsolutePath());
		}
		return v;
	}

	@Override
	public int size() {
		return this.getValues().size();
	}


	@Override
	public String toXML() {
		XMLBuilder x = new XMLBuilder();
		// start with basic tag
		x.startTag(XMLParser.PROCESS_FOLDER, false);
		x.addQuotedAttribute(XMLParser.NAME, this.getName());
		x.addQuotedAttribute(XMLParser.FOLDER, ProcessBlock.cleanFilename(this.gui_rootPath));
		x.addQuotedAttribute(XMLParser.PATTERN, this.gui_pattern);
		
		// add optional attributes
		if(this.gui_append)
			x.addQuotedAttribute(XMLParser.APPEND, this.gui_append);
		if(this.gui_ignorePattern != null && this.gui_ignorePattern.length() > 0)
			x.addQuotedAttribute(XMLParser.IGNORE, this.gui_ignorePattern);
		if(this.gui_disableExistanceCheck)
			x.addQuotedAttribute(XMLParser.DISABLE_EXISTANCE_CHECK, true);
		if(this.gui_maxDepth > 0)
			x.addQuotedAttribute(XMLParser.MAX_DEPTH, this.gui_maxDepth);
		
		if(this.hasColor())
			x.addQuotedAttribute(XMLParser.COLOR, this.getColor());
		
		// end the tag
		x.endCurrentTag();
		return x.toString();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public LinkedHashMap<String, String> getOfferedVariables(ArrayList<String> replace) {
		return (LinkedHashMap<String, String>) OFFER_VAR.clone();
	}

	@Override
	public boolean isAppendAble() {
		return true;
	}

	@Override
	public boolean mightContainFilenames() {
		return true;
	}

	@Override
	public boolean addsReturnInfoToTasks() {
		return false;
	}
	
	@Override
	public Object[] getDataToLoadOnGUI() { 
		return new Object[] { this.gui_rootPath, this.gui_pattern, this.gui_ignorePattern, !this.gui_disableExistanceCheck, this.gui_maxDepth };
	}
	
	@Override
	public boolean isResumeReattachValueAddingRequired() {
		return true;
	}
}
