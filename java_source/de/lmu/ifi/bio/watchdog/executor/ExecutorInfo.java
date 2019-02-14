package de.lmu.ifi.bio.watchdog.executor;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.helper.SyncronizedLineWriter;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import de.lmu.ifi.bio.watchdog.interfaces.XMLPlugin;
import de.lmu.ifi.bio.watchdog.task.Task;


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
	protected Environment env;
	private final String WATCHDOG_BASE_DIR;
	protected boolean isClone = false;
	private final String PATH2JAVA;
	private final String WORKING_DIR;
	private final String STATIC_WORKING_DIR;
	private static final String LOCAL = "/usr/local/storage/";
	private static final String TMP = "/tmp/";
	private static final String JAVA = "/usr/bin/java"; 
	private String color;
	private final Integer MAX_SLAVE_RUNNING;
	private final String TYPE;
	
	/**
	 * Constructor
	 * @param name
	 * @param isDefault
	 * @param maxRunning
	 * @param watchdogBaseDir
	 * @param environment
	 */
	public ExecutorInfo(String type, String name, boolean isDefault, boolean isStick2Host, Integer maxSlaveRunning, String path2java, int maxRunning, String watchdogBaseDir, Environment environment, String workingDir) {
		// test, if we can use some of the default working dirs
		if(workingDir == null)
			workingDir = ExecutorInfo.getWorkingDir(watchdogBaseDir);
		else
			workingDir = Functions.generateRandomWorkingDir(workingDir).getAbsolutePath();
		
		if(path2java == null || path2java.length() == 0)
			path2java = JAVA;
		
		this.TYPE = type;
		this.NAME = name;
		this.IS_DEFAULT = isDefault;
		this.MAX_RUNNING = maxRunning;
		this.WATCHDOG_BASE_DIR = watchdogBaseDir;
		this.IS_STICK2HOST = isStick2Host;
		this.PATH2JAVA = path2java;
		this.STATIC_WORKING_DIR = workingDir;
		this.WORKING_DIR = workingDir;
		
		// set slave mode stuff
		if(isStick2Host)
			this.MAX_SLAVE_RUNNING = maxSlaveRunning;
		else
			this.MAX_SLAVE_RUNNING = null;
		
		this.setEnvironment(environment);
	}
	
	/**
	 * returns the executor for task t
	 * @param t
	 * @param logFile
	 * @return
	 */
	public abstract Executor<?> getExecutorForTask(Task t, SyncronizedLineWriter logFile);
	
	public static String getWorkingDir(String watchdogBaseDir) {
		String workingDir;
		File local = new File(LOCAL);
		File tmp = new File(TMP);
		
		if(local.exists() && local.canWrite())
			workingDir = local.getAbsolutePath();
		else if(tmp.exists() && tmp.canWrite())
			workingDir = tmp.getAbsolutePath();
		else 
			workingDir = new File(watchdogBaseDir + File.separator + TMP).getAbsolutePath();
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
		return this.STATIC_WORKING_DIR;
	}
	
	/**
	 * is this the default executor
	 * @return
	 */
	public boolean isDefaultExecutor() {
		return this.IS_DEFAULT;
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
	 * sets new environment values but does not remove the old one
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
}
