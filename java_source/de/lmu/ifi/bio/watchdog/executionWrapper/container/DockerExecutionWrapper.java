package de.lmu.ifi.bio.watchdog.executionWrapper.container;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.bio.watchdog.executionWrapper.OS;
import de.lmu.ifi.bio.watchdog.executor.Executor;
import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import de.lmu.ifi.bio.watchdog.xmlParser.plugins.executionWrapperParser.DockerExecutionWrapperParser;

/**
 * Package manager based on Conda
 * @author Michael Kluge
 *
 */
public class DockerExecutionWrapper extends AutoDetectMountBasedContainer {

	private static final long serialVersionUID = -2319878681560343735L;

	public static final String SPACE = " ";
	public static final String MOUNT_PARAM = "-v";
	public static final String MOUNT_PARAM_SEP = ":";
	
	public static final String DEFAULT_EXEC_KEYWORD = "run";
	
	private final String WATCHDOG_BASE_DIR;
	private final String DOCKER_PATH;
	private final String IMAGE;
	private final String EXEC_KEYWORD;
	private final String ADD_PARAMS;
	private final boolean DISABLE_AUTO_DETECT;
	private final HashMap<String, String> MOUNTS;
	private final HashMap<String, Pattern> BLACKLIST = new HashMap<>();
	private final ArrayList<String> CONSTANTS_PATH = new ArrayList<>();
	
	public DockerExecutionWrapper(String name, String watchdogBaseDir, String path2docker, String image, String execKeyword, String addParams, boolean disableAutoDetection, HashMap<String, String> mounts, ArrayList<String> blacklist, ArrayList<String> constants) {
		super(name, watchdogBaseDir);
		this.WATCHDOG_BASE_DIR = watchdogBaseDir;
		this.DOCKER_PATH = path2docker;
		this.IMAGE = image;
		this.EXEC_KEYWORD = execKeyword;
		this.ADD_PARAMS = addParams;
		this.DISABLE_AUTO_DETECT = disableAutoDetection;
		this.MOUNTS = mounts;
		
		for(String p : blacklist) {
			String pp = p;
			// ensure that pattern is bound to the start of the string (if pattern is actually a path)
			if(!pp.startsWith("^"))
				pp = "^" + pp;
			if(!pp.endsWith("$"))
				pp = pp + ".*$";

			this.BLACKLIST.put(p, Pattern.compile(pp));
		}
		
		// we don't do that for the moment
		// find mount points in the constants
		//this.CONSTANTS_PATH.addAll(this.collectedPath(constants, false, (x -> this.matchBlacklist(x))));
	}
	
	/**
	 * @return path to docker base directory
	 */
	protected String getDockerPath() {
		return this.DOCKER_PATH;
	}
	
	/**
	 * name of the image that should be started
	 * @return
	 */
	protected String getImageName() {
		return this.IMAGE;
	}
		
	/**
	 * if true, no auto mount detection is performed
	 * @return
	 */
	protected boolean isAutoMountDetectionDisabled() {
		return this.DISABLE_AUTO_DETECT;
	}
	
	/**
	 * additional parameters that are passed to the command
	 * @return
	 */
	protected String getAdditionalCallParams() {
		return this.ADD_PARAMS;
	}
	
	/**
	 * exec keyword to execute a command within an image
	 * @return
	 */
	protected String getExecKeyword() {
		return this.EXEC_KEYWORD;
	}
	
	/**
	 * mounts that are used
	 * @return
	 */
	protected HashMap<String, String> getMounts() {
		return this.MOUNTS;
	}
	
	/**
	 * blacklist patterns
	 * @return
	 */
	protected ArrayList<String> getBlacklist() {
		ArrayList<String> bl = new ArrayList<String>();
		bl.addAll(this.BLACKLIST.keySet());
		return bl;
	}
	
	/**
	 * base dir of watchdog
	 * @return
	 */
	protected String getWatchdogBaseDir() {
		return this.WATCHDOG_BASE_DIR + File.separator;
	}
	
	@Override
	public String toXML() {
		XMLBuilder x = new XMLBuilder();
		// start with basic tag
		x.startTag(DockerExecutionWrapperParser.DOCKER, false);
		
		// add attribute values
		x.addQuotedAttribute(XMLParser.NAME, this.getName());
		x.addQuotedAttribute(DockerExecutionWrapperParser.PATH2DOCKER, this.getDockerPath());
		x.addQuotedAttribute(DockerExecutionWrapperParser.IMAGE, this.getImageName());
		if(this.getExecKeyword().length() > 0 && !DEFAULT_EXEC_KEYWORD.equals(this.getExecKeyword()))
			x.addQuotedAttribute(DockerExecutionWrapperParser.EXEC_KEYWORD, this.getExecKeyword());
		if(this.getAdditionalCallParams() != null && this.getAdditionalCallParams().length() > 0) 
			x.addQuotedAttribute(DockerExecutionWrapperParser.ADD_PARAMS, this.getAdditionalCallParams());
		if(this.isAutoMountDetectionDisabled()) 
			x.addQuotedAttribute(DockerExecutionWrapperParser.DISABLE_AUTODETECT_MOUNT, true);
		x.endOpeningTag();
			
		// write mount part
		if(this.MOUNTS.size() > 0) {
			for(String host : this.MOUNTS.keySet()) {
				if(this.getWatchdogBaseDir().equals(host))
					continue;
				
				x.startTag(DockerExecutionWrapperParser.MOUNT, true, true);
				String container = this.MOUNTS.get(host);
				x.startTag(DockerExecutionWrapperParser.HOST_DIR, true);
				x.addContentAndCloseTag(host);
					
				// if not the same
				if(container != null && container.length() > 0 && !host.equals(container)) {
					x.startTag(DockerExecutionWrapperParser.CONTAINER_DIR, true, true);
					x.addContentAndCloseTag(container);
				}
				x.endCurrentTag();
			}
		}
		// end of mount part
		
		// write blacklist
		if(this.getBlacklist().size() > 0) {
			for(String p : this.getBlacklist()) {
				x.startTag(DockerExecutionWrapperParser.BLACKLIST, true, true);
				x.addQuotedAttribute(DockerExecutionWrapperParser.PATTERN, p);
				x.endCurrentTag();
			}
		}
		// end of blacklist
		
		// end the tag
		x.endCurrentTag();
		return x.toString();
	}

	@Override
	public Object[] getDataToLoadOnGUI() { return new Object[] { this.getDockerPath(), this.getName(), this.getExecKeyword(), this.getAdditionalCallParams(), this.isAutoMountDetectionDisabled(), this.getMounts(), this.getBlacklist() }; }

	@Override
	public boolean canBeAppliedOnTask(Task t) {
		return true;
	}

	/**
	 * adds mounts to the command that is currently build
	 * @param b
	 * @param mounts
	 */
	protected void addMounts(StringBuilder b, HashMap<String, String> m, boolean applyBlacklist) {	
		// add watchdog mount
		HashMap<String, String> mounts = new HashMap<>(m);
		mounts.put(this.getWatchdogBaseDir(), this.getWatchdogBaseDir());
		
		for(String h : mounts.keySet()) {
			// check if some pattern matches
			if(applyBlacklist && !this.getWatchdogBaseDir().equals(h)) {
				if(this.matchBlacklist(h))
					continue;
			}	
			b.append(SPACE);
			b.append(MOUNT_PARAM);
			b.append(SPACE);
			b.append(this.quote(h + MOUNT_PARAM_SEP + mounts.get(h)));
		}
	}

	/**
	 * checks, if a string matches a blacklist pattern
	 * @param h
	 * @return
	 */
	private boolean matchBlacklist(String h) {
		Matcher m = null;
		for(Pattern p : this.BLACKLIST.values()) {
			m = p.matcher(h);
			if(m.matches())
				return true;
		}
		return false;
	}

	@Override
	public ArrayList<String> getInitCommands(Task t) {
		// build the command
		StringBuilder com = new StringBuilder();
		com.append(this.getDockerPath());
		com.append(SPACE);
		com.append(this.getExecKeyword());
		com.append(SPACE);
		com.append(this.getAdditionalCallParams());
		
		// add explicit set mounts
		HashMap<String, String> mounts = new HashMap<>(this.MOUNTS);
		
		// add auto-detected mounts
		if(!this.isAutoMountDetectionDisabled()) {
			this.getMountList(mounts, t, this.CONSTANTS_PATH, (x -> this.matchBlacklist(x) || this.MOUNTS.containsKey(x)));
		}
	
		// add mounts
		this.addMounts(com, mounts, false);
		com.append(SPACE);
		com.append(this.quote(this.getImageName()));
		com.append(SPACE);
		// afterwards the command to execute must be added
		/////////////////////////////////////////////////////////
		
		// add docker command
		ArrayList<String> c = new ArrayList<>();
		c.add(com.toString());
		return c;
	}
	
	/**
	 * quotes an attribute
	 * @param a
	 * @return
	 */
	protected String quote(String a) {
		return Executor.QUOTE + a + Executor.QUOTE;
	}
	
	@Override
	public ArrayList<String> getCleanupCommands(Task t) {
		return new ArrayList<>();
	}

	@Override
	public boolean doesSupportOS(OS os) {
		return !OS.WIN.equals(os);
	}
}