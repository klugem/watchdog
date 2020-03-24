package de.lmu.ifi.bio.watchdog.executor;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

import de.lmu.ifi.bio.watchdog.executionWrapper.ExecutionWrapper;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.helper.SyncronizedLineWriter;
import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import de.lmu.ifi.bio.watchdog.interfaces.XMLPlugin;
import de.lmu.ifi.bio.watchdog.runner.XMLBasedWatchdogRunner;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import de.lmu.ifi.bio.watchdog.xmlParser.plugins.executorParser.XMLExecutorInfoParser;

/**
 * Stores information about executors
 * @author Michael Kluge
 *
 */
public abstract class ExecutorInfo implements XMLDataStore, Cloneable, XMLPlugin {

	private static final long serialVersionUID = 3363943509420173849L;
	private final String NAME;
	private final boolean IS_DEFAULT;
	private final boolean IS_STICK2HOST;
	private final int MAX_RUNNING;
	private transient ConcurrentHashMap<String, Task> RUNNING_JOBS = new ConcurrentHashMap<>(); //do not send over network --> transient --> can not be final anymore
	private final String WATCHDOG_BASE_DIR;
	private final String PATH2JAVA;
	private final String WORKING_DIR;
	private final ArrayList<String> BEFORE_SCRIPT;
	private final ArrayList<String> AFTER_SCRIPT;
	private final ArrayList<String> PACKAGE_MANAGERS;
	private final String CONTAINER;
	private static final String LOCAL = "/usr/local/storage/";
	private static final String TMP = "/tmp/";
	private static final String JAVA = "/usr/bin/java"; 
	public static final String DEFAULT_SHEBANG = "#!/bin/bash";
	public static final String DEFAULT_EXEUCOTR_SCRIPT_PATH = "core_lib" + File.separator + "executor_scripts";
	
	private final Integer MAX_SLAVE_RUNNING;
	private final String TYPE;
	
	protected Environment env;
	protected boolean isClone = false;
	private String color;
	private String shebang = DEFAULT_SHEBANG;
	private ArrayList<String> beforeCommands = new ArrayList<String>();
	private ArrayList<String> afterCommands = new ArrayList<String>();
	private LinkedHashMap<String, ExecutionWrapper> wrappers = null;
	
	/**
	 * Constructor
	 * @param name
	 * @param isDefault
	 * @param maxRunning
	 * @param watchdogBaseDir
	 * @param environment
	 */
	public ExecutorInfo(String type, String name, boolean isDefault, boolean isStick2Host, Integer maxSlaveRunning, String path2java, int maxRunning, String watchdogBaseDir, Environment environment, String workingDir, String shebang, ArrayList<String> beforeScripts, ArrayList<String> afterScripts, ArrayList<String> packageManager, String container) {
		// test, if we can use some of the default working dirs
		if(workingDir == null)
			workingDir = ExecutorInfo.getWorkingDir(watchdogBaseDir);
		
		if(path2java == null || path2java.length() == 0)
			path2java = JAVA;
		
		this.TYPE = type;
		this.NAME = name;
		this.IS_DEFAULT = isDefault;
		this.MAX_RUNNING = maxRunning;
		this.WATCHDOG_BASE_DIR = watchdogBaseDir;
		this.IS_STICK2HOST = isStick2Host;
		this.PATH2JAVA = path2java;
		this.WORKING_DIR = workingDir;
		this.BEFORE_SCRIPT = beforeScripts;
		this.AFTER_SCRIPT = afterScripts;
		this.PACKAGE_MANAGERS = packageManager;
		this.CONTAINER = container;
		
		// set slave mode stuff
		if(isStick2Host)
			this.MAX_SLAVE_RUNNING = maxSlaveRunning;
		else
			this.MAX_SLAVE_RUNNING = null;
		
		this.setEnvironment(environment);
		
		// read before and after script command files
		if(this.BEFORE_SCRIPT != null)
			this.BEFORE_SCRIPT.forEach(path -> this.readAndAddScriptFile(path, this.beforeCommands, true));
		if(this.AFTER_SCRIPT != null)
			this.AFTER_SCRIPT.forEach(path -> this.readAndAddScriptFile(path, this.afterCommands, false));
	}
	
	/**
	 * tries to read a before/after script file and adds it to the ArrayList
	 * @param pathToScriptFile
	 * @param listToAdd
	 * @param beforeScript
	 */
	public void readAndAddScriptFile(String pathToScriptFile, ArrayList<String> listToAdd, boolean beforeScript) {
		if(pathToScriptFile == null || pathToScriptFile.length() == 0)
			return;
		// test if relative path
		if(!pathToScriptFile.startsWith(File.separator)) {
			pathToScriptFile = this.WATCHDOG_BASE_DIR + File.separator + DEFAULT_EXEUCOTR_SCRIPT_PATH + File.separator + pathToScriptFile;
		}
		// test if file exists
		File f = new File(pathToScriptFile);
		if(f.isFile() && f.exists() && f.canRead()) {
			try { listToAdd.addAll(Files.readAllLines(Paths.get(f.getAbsolutePath()))); }
			catch(Exception e) {
				if(beforeScript)
					System.out.println("[ERROR] Failed to read before script '"+ f.getAbsolutePath() +"'.");
				else 
					System.out.println("[ERROR] Failed to read after script '"+ f.getAbsolutePath() +"'.");
				e.printStackTrace();
				System.exit(1);
			}
		}
		else {
			if(beforeScript)
				System.out.println("[WARN] Before script '"+ f.getAbsolutePath() +"' was not found and will be ignored!");
			else 
				System.out.println("[WARN] After script '"+ f.getAbsolutePath()  +"' was not found and will be ignored!");
		}
	}
	
	/**
	 * returns the executor for task t
	 * @param t
	 * @param logFile
	 * @return
	 */
	public abstract Executor<?> getExecutorForTask(Task t, SyncronizedLineWriter logFile);
	
	public static String getWorkingDir(String watchdogBaseDir) {
		String envWork = System.getenv(XMLBasedWatchdogRunner.ENV_WATCHDOG_WORKING_DIR);
		String workingDir = null;
		
		if(envWork != null) {
			File env = new File(envWork);
			if(env.exists() && env.canWrite())
				workingDir = env.getAbsolutePath();
		}
		if(workingDir == null) {
			File local = new File(LOCAL);
			File tmp = new File(TMP);

			if(local.exists() && local.canWrite())
				workingDir = local.getAbsolutePath();
			else if(tmp.exists() && tmp.canWrite())
				workingDir = tmp.getAbsolutePath();
			else 
				workingDir = new File(watchdogBaseDir + File.separator + TMP).getAbsolutePath();
			
			workingDir = Functions.generateRandomWorkingDir(workingDir).getAbsolutePath();
		}		
		return workingDir;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		Object c = super.clone();
		if(c instanceof ExecutorInfo) {
			ExecutorInfo e = (ExecutorInfo) c;
			e.isClone = true;
			c = e;
		}
		return c;
	}
	
	@Override
	public String getRegisterName() {
		return XMLDataStore.getRegisterName(this.getName(), ExecutorInfo.class);
	}
	
	/**
	 * type of the executor
	 * @return
	 */
	public String getType() {
		return this.TYPE;
	}
	
	/**
	 * name of the executor
	 * @return
	 */
	public String getName() {
		return this.NAME;
	}
	
	/**
	 * initial set working directory
	 * @return
	 */
	public String getStaticWorkingDir() {
		return this.WORKING_DIR;
	}
	
	/**
	 * is this the default executor
	 * @return
	 */
	public boolean isDefaultExecutor() {
		return this.IS_DEFAULT;
	}
	
	/**
	 * before script(s) if some are set
	 * @return
	 */
	public ArrayList<String> getBeforeScriptNames() {
		return this.BEFORE_SCRIPT != null ? this.BEFORE_SCRIPT : new ArrayList<String>();
	}
	
	/**
	 * after script(s) if some are set
	 * @return
	 */
	public ArrayList<String> getAfterScriptNames() {
		return this.AFTER_SCRIPT != null ? this.AFTER_SCRIPT : new ArrayList<String>();
	}
	
	/**
	 * returns the list of package managers
	 * @return
	 */
	public ArrayList<String> getPackageManagers() {
		return this.PACKAGE_MANAGERS != null ? this.PACKAGE_MANAGERS : new ArrayList<String>();
	}
	
	/**
	 * name of the container to use
	 * @return
	 */
	public String getContainer() {
		return this.CONTAINER;
	}
	
	/**
	 * tests if before scripts are set
	 * @return
	 */
	public boolean hasBeforeScripts() {
		return this.BEFORE_SCRIPT != null && this.BEFORE_SCRIPT.size() > 0;
	}
	
	/**
	 * tests if after scripts are set
	 * @return
	 */
	public boolean hasAfterScripts() {
		return this.AFTER_SCRIPT != null && this.AFTER_SCRIPT.size() > 0;
	}
	
	/**
	 * true, if package managers are used
	 * @return
	 */
	public boolean hasPackageManagers() {
		return this.PACKAGE_MANAGERS != null &&  this.PACKAGE_MANAGERS.size() > 0;
	}
	
	/**
	 * true, if a wrapping container is used
	 * @return
	 */
	public boolean usesContainer()  {
		return this.CONTAINER != null && this.CONTAINER.length() > 0;
	}
	
	/**
	 * is this a stick to host executor
	 * @return
	 */
	public boolean isStick2Host() {
		return this.IS_STICK2HOST && !this.isClone;
	}
	
	public Integer getMaxSlaveRunningTasks() {
		return this.MAX_SLAVE_RUNNING;
	}
	
	/**
	 * path to java for slave mode
	 * @return
	 */
	public String getPath2Java() {
		return this.PATH2JAVA;
	}
	
	/**
	 * path to working dir that is substituted with ${TMP} automatically
	 * @return
	 */
	public String getWorkingDir() {
		return this.WORKING_DIR;
	}
	
	
	/**
	 * number of tasks which can run at the same time on this executor
	 * @return
	 */
	public int getMaxSimRunning() {
		return this.MAX_RUNNING;
	}
	
	/**
	 * Adds an ID of a job which is currently running on this executor
	 * @param t
	 */
	public void addIDofRunningJob(Task t) {
		this.RUNNING_JOBS.put(t.getID(), t);
	}
	
	/**
	 * removes an ID of an job after the processing has finished on this executor
	 * @param t
	 */
	public void removeIDofRunningJob(Task t) {
		this.RUNNING_JOBS.remove(t.getID());
	}
	
	/**
	 * Tests, if too many jobs are currently running on this executor
	 * @return
	 */
	public boolean isMaxRunningRestrictionReached() {
		return this.getMaxSimRunning() >= 0 && this.getNumberOfRunningJobs() >= this.getMaxSimRunning();
	}

	/**
	 * number of running jobs which have resource restrictions
	 * @return
	 */
	private int getNumberOfRunningJobs() {
		int i = 0;
		for(Task t : this.RUNNING_JOBS.values()) {
			if(t.doesConsumeResources())
				i++;
		}
		return i;
	}

	/**
	 * Returns a list of IDs of currently running jobs
	 * @return
	 */
	public ArrayList<String> getIDsOfCurrentlyRunningJobs() {
		return new ArrayList<String>(this.RUNNING_JOBS.keySet());
	}
	
	/**
	 * sets a new environment
	 * @param environment
	 */
	public void setEnvironment(Environment environment) {
		this.env = environment;
	}

	/**
	 * returns the base directory of watchdog
	 * @return
	 */
	public String getWatchdogBaseDir() {
		return this.WATCHDOG_BASE_DIR;
	}

	public boolean hasDefaultEnv() {
		return this.env != null;
	}
	
	public Environment getEnv() {
		return this.env;
	}

	public void setClone() {
		this.isClone = true;
	}
	@Override
	public void setColor(String c) {
		this.color = c;
	}
	@Override
	public String getColor() {
		return this.color;
	}
	
	@Override
	public Class<? extends XMLDataStore> getStoreClassType() {
		return ExecutorInfo.class;
	}
	
	@Override
	public void onDeleteProperty() {}
	
	private synchronized void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		
		this.RUNNING_JOBS = new ConcurrentHashMap<>();
	}
	
	/**
	 * true, if detach/attach mode of Watchdog is supported by that executor while tasks are running on it
	 * @return
	 */
	public abstract boolean isWatchdogDetachSupported();
	
	/**
	 * returns all jobs that are currently executed
	 * @return
	 */
	public HashMap<String, Task> getRunningJobs() {
		return new HashMap<>(this.RUNNING_JOBS);
	}
	
	
	/**
	 * returns the shebang for the external command
	 * @return
	 */
	public String getShebang() {
		return this.shebang;
	}
	
	public void setShebang(String shebang) {
		if(shebang == null || shebang.isEmpty()) {
			System.out.println("[ERROR] Shebang can not be empty.");
			System.exit(1);
		}
		this.shebang = shebang;
	}
	
	/**
	 * sets the execution wrappers that are defined
	 * @param wrappers
	 */
	public void setWrappers(LinkedHashMap<String, ExecutionWrapper> wrappers) {
		this.wrappers = wrappers;
	}
	
	/**
	 * returns all defined execution wrappers
	 * @return
	 */
	public LinkedHashMap<String, ExecutionWrapper> getExecutionWrappers() {
		return this.wrappers;
	}
	
	/**
	 * true, if a non-default shebang was set
	 * @return
	 */
	public boolean hasCustomShebang() {
		return !DEFAULT_SHEBANG.equals(this.shebang);
	}
	
	/**
	 * commands that should be executed before the actual command
	 * @return
	 */
	public ArrayList<String> getBeforeCommands() {
		return this.beforeCommands;
	}

	/**
	 * commands that should be executed after the actual command
	 * @return
	 */
	public ArrayList<String> getAfterCommands() {
		return this.afterCommands;
	}
	
	/**
	 * true, if it has a custom java path
	 * @return
	 */
	public boolean hasCustomPath2Java() {
		return !PATH2JAVA.equals(this.getPath2Java());
	}
	
	/**
	 * adds all executor attributes that are common to all executors
	 * @param x
	 */
	public void addDefaultExecutorAttributes(XMLBuilder x) {

		if(this.isStick2Host())
			x.addQuotedAttribute(XMLParser.STICK2HOST, true);
		if(this.hasDefaultEnv())
			x.addQuotedAttribute(XMLParser.ENVIRONMENT, this.getEnv().getName());
		if(this.isDefaultExecutor())
			x.addQuotedAttribute(XMLParser.DEFAULT, true);
		if(this.getMaxSimRunning() >= 1)
			x.addQuotedAttribute(XMLParser.MAX_RUNNING, this.getMaxSimRunning());
		if(this.isStick2Host())
			x.addQuotedAttribute(XMLParser.STICK2HOST, true);
		if(this.hasColor())
			x.addQuotedAttribute(XMLParser.COLOR, this.getColor());
		if(this.hasCustomShebang()) 
			x.addQuotedAttribute(XMLParser.SHEBANG, this.getShebang());
		if(this.hasBeforeScripts()) 
			x.addQuotedAttribute(XMLParser.BEFORE_SCRIPTS, XMLExecutorInfoParser.joinString(this.getBeforeScriptNames()));
		if(this.hasAfterScripts()) 
			x.addQuotedAttribute(XMLParser.AFTER_SCRIPTS, XMLExecutorInfoParser.joinString(this.getAfterScriptNames()));
		if(this.hasPackageManagers())
			x.addQuotedAttribute(XMLParser.PACKAGE_MANAGERS, StringUtils.join(this.getPackageManagers(), XMLParser.WRAPPERS_SEP));
		if(this.usesContainer())
			x.addQuotedAttribute(XMLParser.CONTAINER, this.getContainer());
		if(this.hasCustomPath2Java())
			x.addQuotedAttribute(XMLParser.PATH2JAVA, this.getPath2Java());
		if(!LOCAL.equals(this.getWorkingDir()))
			x.addQuotedAttribute(XMLParser.WORKING_DIR_EXC, this.getWorkingDir());
	}

	/**
	 * might return some executor specific environment variables that should be set
	 * @return
	 */
	public abstract HashMap<String, String> getExecutorSpecificEnvironmentVariables();
}
