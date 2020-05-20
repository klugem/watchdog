package de.lmu.ifi.bio.watchdog.executionWrapper.packageManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import de.lmu.ifi.bio.watchdog.executionWrapper.FindVersionedFile;
import de.lmu.ifi.bio.watchdog.executionWrapper.OS;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import de.lmu.ifi.bio.watchdog.xmlParser.plugins.executionWrapperParser.CondaExecutionWrapperParser;

/**
 * Package manager based on Conda
 * @author Michael Kluge
 *
 */
public class CondaExecutionWrapper extends FileBasedPackageManger {

	private static final long serialVersionUID = -7837779548673768374L;
	private static final String CONDA_YML = ".conda.yml";
	private static final String BEFORE_SCRIPT_PATH = "conda.before.sh";
	private static final String AFTER_SCRIPT_PATH = "conda.after.sh";
	
	/** names for the variables used in conda scripts */
	public static String CONDA_PATH_PLUGIN = "CONDA_PATH_PLUGIN";
	public static String CONDA_PATH_TO_BIN = "CONDA_PATH_TO_BIN";
	public static String CONDA_PATH_TO_ENV = "CONDA_PATH_TO_ENV";
	public static String CONDA_PATH_TO_YML = "CONDA_PATH_TO_YML";
	
	private final String CONDA_BIN_PATH;
	private final String CONDA_BIN_DIR;
	private final String CONDA_ENV_PREFIX_PATH;
	
	private static final FindVersionedFile FIND_VERSIONED_FILE = new FindVersionedFile(x -> x.getName().endsWith(CONDA_YML), CONDA_YML);

	public CondaExecutionWrapper(String name, String watchdogBaseDir, String path2condaBinary, String path2condaEnvironmentDir) {
		super(name, watchdogBaseDir);
		this.CONDA_BIN_PATH = path2condaBinary;
		this.CONDA_BIN_DIR = new File(path2condaBinary).getParentFile().getAbsolutePath();
		this.CONDA_ENV_PREFIX_PATH = path2condaEnvironmentDir;
	}
	
	/**
	 * path to conda binary
	 * @return
	 */
	protected String getCondaBinaryPath() {
		return this.CONDA_BIN_PATH;
	}
	
	/**
	 * @return path to conda bin directory
	 */
	protected String getCondaBinDir() {
		return this.CONDA_BIN_DIR;
	}
	
	/**
	 * path to the parent folder for the conda environments
	 * @return
	 */
	public String getCondaEnvironmentPrefixPath() {
		return this.CONDA_ENV_PREFIX_PATH;
	}
	
	@Override
	public String toXML() {
		XMLBuilder x = new XMLBuilder();
		// start with basic tag
		x.startTag(CondaExecutionWrapperParser.CONDA, false);
		
		// add attribute values
		x.addQuotedAttribute(XMLParser.NAME, this.getName());
		x.addQuotedAttribute(CondaExecutionWrapperParser.PATH2CONDA, this.getCondaBinaryPath());
		if(this.getCondaEnvironmentPrefixPath() != null && this.getCondaEnvironmentPrefixPath().length() > 0 && !this.getCondaEnvironmentPrefixPath().endsWith(CondaExecutionWrapperParser.DEFAULT_CONDA_ENV_NAME)) 
			x.addQuotedAttribute(CondaExecutionWrapperParser.PATH2ENV, this.getCondaEnvironmentPrefixPath());
		
		// end the tag
		x.endCurrentTag();
		return x.toString();
	}

	@Override
	public Object[] getDataToLoadOnGUI() { return new Object[] { this.getCondaBinaryPath(), this.getCondaEnvironmentPrefixPath() }; }
	
	/**
	 * tests if a conda environment definition file is there 
	 * @param folder
	 * @return
	 */
	public File findCondaEnvDefinitionFile(File folder, Integer moduleVersion) {
		return FIND_VERSIONED_FILE.find(folder, moduleVersion);
	}

	@Override
	public boolean canBeAppliedOnTask(Task t) {
		return this.findCondaEnvDefinitionFile(t.getModuleFolder(), t.getModuleVersion()) != null;
	}
	
	/**
	 * path to the conda dependency definition file
	 * @return
	 */
	protected File getPathToYmlFile(Task t) {
		return this.findCondaEnvDefinitionFile(t.getModuleFolder(), t.getModuleVersion());
	}
	
	/**
	 * name of the environment based on the file hash
	 * @return
	 */
	protected String getNameOfEnv(Task t) {
		try { 
			String hash = Functions.getFileHash(this.getPathToYmlFile(t));
		return hash;
		}
		catch(IOException e) { e.printStackTrace(); }
		return null;
	}

	@Override
	public ArrayList<String> getInitCommands(Task t) {
		return this.getBeforeScriptCommands(t);
	}
	
	@Override
	public ArrayList<String> getCleanupCommands(Task t) {
		return this.getAfterScriptCommands(t);
	}
	
	@Override
	protected HashMap<String, String> getAdditionalVariables(Task t) {
		try {
			String env_hash = Functions.getFileHash(this.getPathToYmlFile(t));
			String path2env = this.getCondaEnvironmentPrefixPath() + File.separator + env_hash;
			
			// create the list of vars
			LinkedHashMap<String, String> vars = new LinkedHashMap<>();
			vars.put(CONDA_PATH_PLUGIN, this.getPluginScriptFolder());
			vars.put(CONDA_PATH_TO_BIN, this.getCondaBinDir());
			vars.put(CONDA_PATH_TO_ENV, path2env);
			vars.put(CONDA_PATH_TO_YML, this.getPathToYmlFile(t).getAbsolutePath());
			return vars;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public String getBeforeScriptPath() {
		return BEFORE_SCRIPT_PATH;
	}

	@Override
	public String getAfterScriptPath() {
		return AFTER_SCRIPT_PATH;
	}

	@Override
	public boolean doesSupportOS(OS os) {
		return !OS.WIN.equals(os);
	}
}