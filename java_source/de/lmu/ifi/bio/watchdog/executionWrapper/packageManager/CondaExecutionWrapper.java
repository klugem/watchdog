package de.lmu.ifi.bio.watchdog.executionWrapper.packageManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

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
	private static final String CONDA_BIN = File.separator + "bin" + File.separator;
	private static final String CONDA_YML = "conda.yml";
	private static final String BEFORE_SCRIPT_PATH = "conda.before.sh";
	private static final String AFTER_SCRIPT_PATH = "conda.after.sh";
	
	/** names for the variables used in conda scripts */
	public static String CONDA_PATH_PLUGIN = "CONDA_PATH_PLUGIN";
	public static String CONDA_PATH_TO_BIN = "CONDA_PATH_TO_BIN";
	public static String CONDA_PATH_TO_ENV = "CONDA_PATH_TO_ENV";
	public static String CONDA_PATH_TO_YML = "CONDA_PATH_TO_YML";
	
	private final String CONDA_PATH;
	private final String CONDA_ENV_PREFIX_PATH;
	
	private boolean searched = false;
	private File condaDefFile = null;

	public CondaExecutionWrapper(String name, String watchdogBaseDir, String path2condaBasedir, String path2condaEnvironmentDir) {
		super(name, watchdogBaseDir);
		this.CONDA_PATH = path2condaBasedir;
		this.CONDA_ENV_PREFIX_PATH = path2condaEnvironmentDir;
	}
	
	/**
	 * @return path to conda base directory
	 */
	protected String getCondaPath() {
		return this.CONDA_PATH;
	}
	
	/**
	 * returns the path to a conda binary
	 * @param bin
	 * @return
	 */
	protected String getPathToBinary(String bin) {
		return this.getCondaBinaryPath() + File.separator + bin;
	}
	
	/**
	 * returns the path to the conda binary
	 * @return
	 */
	protected String getCondaBinaryPath() {
		return this.CONDA_PATH + CONDA_BIN;
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
		x.addQuotedAttribute(CondaExecutionWrapperParser.PATH2CONDA, this.getCondaPath());
		if(this.getCondaEnvironmentPrefixPath() != null && this.getCondaEnvironmentPrefixPath().length() > 0 && !this.getCondaEnvironmentPrefixPath().endsWith(CondaExecutionWrapperParser.DEFAULT_CONDA_ENV_NAME)) 
			x.addQuotedAttribute(CondaExecutionWrapperParser.PATH2ENV, this.getCondaEnvironmentPrefixPath());
		
		// end the tag
		x.endCurrentTag();
		return x.toString();
	}

	@Override
	public Object[] getDataToLoadOnGUI() { return new Object[] { this.getCondaPath(), this.getCondaEnvironmentPrefixPath() }; }

	@Override
	public boolean isPackageManager() {
		return true;
	}
	
	/**
	 * tests if a conda environment definition file is there 
	 * @param folder
	 * @return
	 */
	public File findCondaEnvDefinitionFile(File folder) {
		if(this.searched == true) return this.condaDefFile;
		
		this.searched = true;
		if(folder != null) {
			File[] yml = folder.listFiles(x -> x.getName().endsWith(CONDA_YML));
			int ok = 0;
			File lastOK = null;
			for(File y : yml) {
				lastOK = y;
				ok++;
			}
			if(ok == 1) this.condaDefFile = lastOK;
		}
		return this.condaDefFile;
	}

	@Override
	public boolean canBeAppliedOnTask(Task t) {
		return this.findCondaEnvDefinitionFile(t.getModuleFolder()) != null;
	}
	
	/**
	 * path to the conda dependency definition file
	 * @return
	 */
	protected String getPathToYmlFile() {
		return this.condaDefFile.getAbsolutePath(); 
	}
	
	/**
	 * name of the environment based on the file hash
	 * @return
	 */
	protected String getNameOfEnv() {
		try { 
			File f = new File(this.getPathToYmlFile());
			String hash = Functions.getFileHash(f);
		return hash;
		}
		catch(IOException e) { e.printStackTrace(); }
		return null;
	}

	@Override
	public ArrayList<String> getInitCommands(Task t) {
		return this.getBeforeScriptCommands();
	}
	
	@Override
	public ArrayList<String> getCleanupCommands(Task T) {
		return this.getAfterScriptCommands();
	}
	
	@Override
	protected HashMap<String, String> getAdditionalVariables() {
		try {
			String env_hash = Functions.getFileHash(this.condaDefFile);
			String path2env = this.getCondaEnvironmentPrefixPath() + File.separator + env_hash;
			
			// create the list of vars
			LinkedHashMap<String, String> vars = new LinkedHashMap<>();
			vars.put(CONDA_PATH_PLUGIN, this.getPluginScriptFolder());
			vars.put(CONDA_PATH_TO_BIN, this.getCondaBinaryPath());
			vars.put(CONDA_PATH_TO_ENV, path2env);
			vars.put(CONDA_PATH_TO_YML, this.getPathToYmlFile());
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