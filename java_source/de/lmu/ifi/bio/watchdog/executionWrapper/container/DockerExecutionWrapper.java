package de.lmu.ifi.bio.watchdog.executionWrapper.container;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.bio.watchdog.executionWrapper.FindVersionedFile;
import de.lmu.ifi.bio.watchdog.executionWrapper.OS;
import de.lmu.ifi.bio.watchdog.executor.Executor;
import de.lmu.ifi.bio.watchdog.helper.Functions;
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
	public static final String MOUNT_PARAM_SINGULARITY = "-B";
	public static final String MOUNT_PARAM_SEP = ":";
	public static final String ADDITIONAL_PARAMS_SINGULARITY = "--containall --vm-err";
	public static final String ADDITIONAL_PARAMS_DOCKER_PODMAN = "--rm";
	public static final String TERMINATE_WRAPPER = "core_lib"+ File.separator +"plugins" + File.separator + "docker_terminate_wrapper.sh";
	
	public static final String DEFAULT_EXEC_KEYWORD = "run";
	
	private final String WATCHDOG_BASE_DIR;
	private final String DOCKER_PATH;
	private final String BINARY_NAME;
	private final String IMAGE;
	private final String EXEC_KEYWORD;
	private final String ADD_PARAMS;
	private final boolean DISABLE_AUTO_DETECT;
	private final boolean DISABLE_TERMINATE_WRAPPER;
	private final HashMap<String, String> MOUNTS;
	private final HashMap<String, Pattern> BLACKLIST = new HashMap<>();
	private final ArrayList<String> CONSTANTS_PATH = new ArrayList<>();
	private final boolean LOAD_MODULE_SPECIFIC_IMAGE;
	
	private final static String DOCKER_ENDING = ".name";
	private final static String FILE_NAME_PATTERN = ("docker.image(" + FindVersionedFile.MOD_VERSION + ")?" + DOCKER_ENDING).replaceAll("\\.", "\\\\.");
	private final static FindVersionedFile FIND_VERSIONED_FILE = new FindVersionedFile(x -> x.getName().matches(FILE_NAME_PATTERN), DOCKER_ENDING);
	
	private static final String BIN_DOCKER = "docker";
	private static final String BIN_PODMAN = "podman";
	private static final String BIN_SINGULARITY = "singularity";
		
	private static final HashMap<String, String> CUSTOM_MOUNT_PARAM = new HashMap<>();
	private static final HashMap<String, String> CUSTOM_ADD_PARAM = new HashMap<>();

	// add some static settings
	static {
		CUSTOM_MOUNT_PARAM.put(BIN_SINGULARITY, MOUNT_PARAM_SINGULARITY);
		CUSTOM_ADD_PARAM.put(BIN_SINGULARITY, ADDITIONAL_PARAMS_SINGULARITY);
		CUSTOM_ADD_PARAM.put(BIN_DOCKER, ADDITIONAL_PARAMS_DOCKER_PODMAN);
		CUSTOM_ADD_PARAM.put(BIN_PODMAN, ADDITIONAL_PARAMS_DOCKER_PODMAN);	
	}
	
	public DockerExecutionWrapper(String name, String watchdogBaseDir, String path2docker, String image, String execKeyword, String addParams, boolean disableAutoDetection, HashMap<String, String> mounts, ArrayList<String> blacklist, ArrayList<String> constants, boolean loadModuleSpecificImage, boolean disableTerminateWrapper) {
		super(name, watchdogBaseDir);
		this.WATCHDOG_BASE_DIR = watchdogBaseDir;
		this.DOCKER_PATH = path2docker;
		this.LOAD_MODULE_SPECIFIC_IMAGE = loadModuleSpecificImage;
		this.IMAGE = image;
		this.EXEC_KEYWORD = execKeyword;
		this.ADD_PARAMS = addParams;
		this.DISABLE_AUTO_DETECT = disableAutoDetection;
		this.MOUNTS = mounts;
		this.BINARY_NAME = new File(this.DOCKER_PATH).getName();
		this.DISABLE_TERMINATE_WRAPPER = disableTerminateWrapper;
		
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
	 * name of the image obtained from file located in the module folder
	 * @param moduleFolder
	 * @param moduleVersion
	 * @return
	 */
	private String getImageName(File moduleFolder, Integer moduleVersion) {
		File f = FIND_VERSIONED_FILE.find(moduleFolder, moduleVersion);
		if(f != null) {
			try {
				List<String> lines = Files.readAllLines(f.toPath());
				if(lines.size() != 1)
					LOGGER.warn("File with name of docker image '"+ f.getAbsolutePath() +"' should contain only one line vs '"+ lines.size() +"'");
				if(lines.size() >= 1) {
					String image = lines.get(0);
					return image;
				}
			}
			catch(Exception e) {
				LOGGER.error("Failed to read name of docker image '"+ f.getAbsolutePath() +"'.");
				e.printStackTrace();
			}
		}
		return this.getImageName();
	}
		
	/**
	 * if true, no auto mount detection is performed
	 * @return
	 */
	protected boolean isAutoMountDetectionDisabled() {
		return this.DISABLE_AUTO_DETECT;
	}
	
	/**
	 * if true, names of module specific docker images are loaded
	 * @return
	 */
	protected boolean loadModuleSpecificImage() {
		return this.LOAD_MODULE_SPECIFIC_IMAGE;
	}
	
	/**
	 * if true, the podman terminate wrapper script is NOT used
	 * @return
	 */
	protected boolean disableTerminateWrapper() {
		return this.DISABLE_TERMINATE_WRAPPER;
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
		if(!this.loadModuleSpecificImage()) 
			x.addQuotedAttribute(DockerExecutionWrapperParser.LOAD_MODULE_SPECIFIC_IMAGE, false);
		if(this.disableTerminateWrapper())
			x.addQuotedAttribute(DockerExecutionWrapperParser.DISABLE_TERMINATE_WRAPPER, true);
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
	public Object[] getDataToLoadOnGUI() { return new Object[] { this.getDockerPath(), this.getImageName(), this.getExecKeyword(), this.getAdditionalCallParams(), this.isAutoMountDetectionDisabled(), this.getMounts(), this.getBlacklist(), this.loadModuleSpecificImage(), this.disableTerminateWrapper() }; }

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
		String watchdogBaseDir = this.getWatchdogBaseDir();
		// add watchdog mount
		HashMap<String, String> mounts = new HashMap<>(m);
		mounts.put(watchdogBaseDir, watchdogBaseDir);
		// add tmp dir if it is a symlink
		String tmpPath = Functions.getRealTemporaryFolder();
		if(!tmpPath.startsWith(watchdogBaseDir))
			mounts.put(tmpPath, tmpPath);
		
		String mountParam = MOUNT_PARAM;
		if(CUSTOM_MOUNT_PARAM.containsKey(this.getBinaryName()))
			mountParam = CUSTOM_MOUNT_PARAM.get(this.getBinaryName());
		
		for(String h : mounts.keySet()) {
			// check if some pattern matches
			if(applyBlacklist && !this.getWatchdogBaseDir().equals(h)) {
				if(this.matchBlacklist(h))
					continue;
			}	
			b.append(SPACE);
			b.append(mountParam);
			b.append(SPACE);
			b.append(this.quote(h + MOUNT_PARAM_SEP + mounts.get(h)));
		}
	}

	protected String getBinaryName() {
		return this.BINARY_NAME;
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
		if(!this.disableTerminateWrapper()) {
			com.append(this.getWatchdogBaseDir() + TERMINATE_WRAPPER);
			com.append(SPACE);
		}
		com.append(this.getDockerPath());
		com.append(SPACE);
		com.append(this.getExecKeyword());
		com.append(SPACE);
		
		// get the image to load 		
		String image = this.getImageName();
		if(this.loadModuleSpecificImage())
			image = this.getImageName(t.getModuleFolder(), t.getModuleVersion());
		
		// test if there are some custom params for the binary
		if(CUSTOM_ADD_PARAM.containsKey(this.getBinaryName())) {
			com.append(CUSTOM_ADD_PARAM.get(this.getBinaryName()));
			com.append(SPACE);
		}
		
		// add additional parameters that were set by the user
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
		com.append(this.quote(image));
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