package de.lmu.ifi.bio.watchdog.executor;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;

import org.apache.commons.lang3.StringUtils;

import de.lmu.ifi.bio.watchdog.executionWrapper.ExecutionWrapper;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.helper.SyncronizedLineWriter;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.slave.Master;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.task.TaskStatus;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask2TaskThread;

/**
 * Executor which can execute tasks
 * Class must ensure that the corresponding MonitorThread object is created and running!
 * @author Michael Kluge
 *
 */
public abstract class Executor<A extends ExecutorInfo> {
	
	public static final String WATCHGOD_CORES = "WATCHDOG_CORES";
	public static final String WATCHGOD_MEMORY = "WATCHDOG_MEMORY";
	public static final String VERSION_INFO_PREFIX = "version_";

	public static final String QUOTE = "'";
	public static final String COMMAND_SEP = System.lineSeparator();
	public static final String EXECUTE = "execute";
	public static final String WATCHGOD_ENV_IDENTIFIER = "IS_WATCHDOG_JOB";
	public static final String WATCHGOD_ENV_IDENTIFIER_VALUE = "1";
	protected static final String EXECUTE_PREFIX = "execute_";
	protected static final String ENV_PREFIX = "env_";
	public static final String ZERO_SEP = "\0";
	protected static String MAIN_COMMAND_RETURN="MAIN_COMMAND_RETURN=$?";
	protected static String EXIT_MAIN_COMMAND_RETURN="exit ${MAIN_COMMAND_RETURN}";
	
	public static String default_working_dir;
	private static XMLTask2TaskThread xml2taskThread;
	private static File watchdogBase;
	
	private final Logger LOGGER = new Logger(LogLevel.INFO);
	protected final Task TASK;
	protected final SyncronizedLineWriter LOG;
	protected final ArrayList<String> BEFORE_COMMAND = new ArrayList<>();
	protected final ArrayList<String> AFTER_COMMAND = new ArrayList<>();
	protected final A EXEC_INFO;
	protected HashMap<String, String> internalEnv;

	
	/**
	 * Constructor without any checker
	 * @param task
	 * @param log
	 */
	public Executor(Task t, SyncronizedLineWriter log, A execInfo) {
		this.TASK = t;
		this.LOG = log;
		this.EXEC_INFO = execInfo;
		
		 // add this value to allow the script to detect if it was called by watchdog
		this.TASK.addEnvVariable(WATCHGOD_ENV_IDENTIFIER, WATCHGOD_ENV_IDENTIFIER_VALUE);
		HashMap<String, String> esev  = this.EXEC_INFO.getExecutorSpecificEnvironmentVariables();
		for(String name : esev.keySet()) {
			this.TASK.addEnvVariable(name, esev.get(name));
		}
		
		// get the module version if task was created from a XMLTask
		XMLTask xmlTask = XMLTask.getXMLTask(t.getTaskID());
		if(xmlTask != null) 
			t.setModuleVersion(xmlTask.getVersion());
	
		// check, if the task should be executed on the same host as preceding or following tasks
		if(this.TASK.willRunOnSlave()) {
			try {				
				// set slave status
				t.setIsScheduledOnSlave(true);
				// check, if the task has already an assigned slave ID from preceding tasks
				if(this.TASK.getSlaveID() == null || !this.TASK.getExecutor().isStick2Host()) 
					this.TASK.setSlaveTaskID(Master.getNewSlaveID());
		
				// get the old or newly set slave ID!
				String slaveID = this.TASK.getSlaveID();
				LinkedHashSet<Integer> depToKeep = new LinkedHashSet<>();
				if(xmlTask != null)
					depToKeep = xmlTask.getSeparateSlaveDependencies();
				
				XMLTask slave = Master.addSlave(slaveID, this.TASK, Executor.watchdogBase, (ExecutorInfo) this.EXEC_INFO.clone(), this.EXEC_INFO.env, depToKeep);
				// slave must be spawned first!
				if(slave != null)
					Executor.xml2taskThread.addTask(slave);
			}
			catch(Exception e) {
				// was not able to create new slave
				this.LOGGER.error("Was not able to spawn new slave. Exiting now...");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	/**
	 * returns the task this executor should execute
	 * @return
	 */
	public Task getTask() {
		return this.TASK;
	}

	/**
	 * executes the task, monitors it and updates the status
	 */
	public abstract void execute();

	/**
	 * stops the execution
	 */
	public void stopExecution() {
		// remove it from the running job list
		this.TASK.getExecutor().removeIDofRunningJob(this.TASK);
	}
	
	/**
	 * returns the status of the task
	 * @return
	 */
	public TaskStatus getTaskStatus() {
		return this.TASK.getStatus();
	}
	
	/**
	 * gets the working dir which is set in the task or a default one
	 * @param createDir
	 * @return
	 */
	public String getWorkingDir(boolean createDir) {
		return this.TASK.getWorkingDir(createDir) == null ? Executor.default_working_dir : this.TASK.getWorkingDir(createDir).getAbsolutePath() + File.separator;
	}
	
	/**
	 * type of the executor
	 * @return
	 */
	public abstract String getType();
	
	/**
	 * Identifier of this executor
	 * @return
	 */
	public abstract String getID();
		
	@Override
	public String toString() {
		return this.getID();
	}

	/**
	 * Commands, which are executed in the order of insertion before the actual command is executed
	 * @param command
	 */
	public void addBeforeCommand(String command) {
		this.BEFORE_COMMAND.add(command);
	}
	
	/**
	 * Commands, which are executed in the order of insertion after the actual command is executed
	 * @param command
	 */
	public void addAfterCommand(String command) {
		this.AFTER_COMMAND.add(command);
	}
	
	/**
	 * writes the commands to a temporary file
	 * @param command
	 * @return
	 */
	public String writeCommandsToFile(String command) {
		File f = Functions.generateRandomTmpExecutionFile(EXECUTE_PREFIX, false);
		try {
			FileWriter w = new FileWriter(f);
			String shebang = this.TASK.getShebang();
			if(this.TASK.getShebang() == null)
				shebang = ExecutorInfo.DEFAULT_SHEBANG;
			
			w.write(shebang);
			w.write(System.lineSeparator());
			w.write(command);
			w.flush();
			w.close();
			f.setExecutable(true);
		}
		catch(Exception e) {
			LOGGER.error("Could not write to create temporary file '"+f.getAbsolutePath()+"'.");
			e.printStackTrace();
			System.exit(1);
		}
		return f.getAbsolutePath();
	}
	
	/**
	 * true, if it is only a single command
	 * @return
	 */
	public boolean isSingleCommand() {
		return this.BEFORE_COMMAND.size() == 0 && this.AFTER_COMMAND.size() == 0;
	}
	
	/**
	 * either returns the command itself, if it is only one command are sum up all command in a script
	 * @return
	 */
	public String[] getFinalCommand(boolean removeQuoting, boolean addBashWrappingScript) {
		ArrayList<String> c = new ArrayList<>();

		String vqp = this.TASK.getVersionQueryParameter();
		boolean addVersionQuery = (vqp != null && vqp.length() > 0);
		// no script needed
		if(this.isSingleCommand() && !addBashWrappingScript && !addVersionQuery && !this.EXEC_INFO.hasPackageManagers() && !this.EXEC_INFO.usesContainer()) {
			c.add(this.TASK.getBinaryCall());
			
			// remove quoting, if needed
			if(removeQuoting) {
				for(String arg : this.TASK.getArguments())
					c.add(arg.replaceAll("^['\"]", "").replaceAll("['\"]$", ""));
			}
			else 
				c.addAll(this.TASK.getArguments());
			
			return c.toArray(new String[0]);
		}
		// write the stuff to a file
		else {
			ExecutionWrapper w = null;
			ExecutionWrapper container = null;
			// process package manager
			if(this.EXEC_INFO.hasPackageManagers()) {
				for(String wrapperName : this.EXEC_INFO.getPackageManagers()) {
					// we don't need to test, wrapper existence must be enforced before
					// and we don't need to test OS support 
					w = this.EXEC_INFO.getExecutionWrappers().get(wrapperName);
					// get the first wrapper that can be applied on the task (if any)
					if(w.canBeAppliedOnTask(this.TASK)) {
						ArrayList<String> init = w.getInitCommands(this.TASK);
						if(init != null && init.size() > 0)
							c.addAll(init);
						
						break;
					}
					else {
						w = null;
					}
				}
			}
			// handle virtualizer
			if(this.EXEC_INFO.usesContainer()) {
				String containerName = this.EXEC_INFO.getContainer();
				container = this.EXEC_INFO.getExecutionWrappers().get(containerName);
				// make sure that it can be used for task t
				if(!container.canBeAppliedOnTask(this.TASK))
					container = null;
			}

			c.addAll(this.BEFORE_COMMAND);
			String taskParameter = (this.TASK.getArguments().size() > 0 ? " " + StringUtils.join(this.TASK.getArguments(), " ") : "");
			c.add(this.TASK.getBinaryCall() + taskParameter);
			c.add(MAIN_COMMAND_RETURN);

			// add after command that will query the software version
			if(addVersionQuery) {
				File v = Functions.generateRandomTmpExecutionFile(VERSION_INFO_PREFIX + this.TASK.getID(), false);
				this.addAfterCommand(this.TASK.getBinaryCall() + " " + this.TASK.getVersionQueryParameter() + taskParameter + " 2>&1 > " + v.getAbsolutePath());
				this.TASK.setVersionQueryInfoFile(v);
			}
			c.addAll(this.AFTER_COMMAND);
			
			// add clean-up if wrapper is used
			if(w != null) {
				ArrayList<String> clean = w.getCleanupCommands(this.TASK);
				if(clean != null && clean.size() > 0)
					c.addAll(clean);
			}
			c.add(EXIT_MAIN_COMMAND_RETURN);
			
			// write commands to file
			String tmpCommandFile = this.writeCommandsToFile(StringUtils.join(c, Executor.COMMAND_SEP));
			
			// test, if we need to wrap that into a container
			if(container != null) {
				ArrayList<String> conCom = new ArrayList<>();
				conCom.addAll(container.getInitCommands(this.TASK));
				
				// update the last command
				String lastCommand = conCom.remove(conCom.size()-1);
				lastCommand = lastCommand + Executor.QUOTE + tmpCommandFile + Executor.QUOTE;
				conCom.add(lastCommand);
				
				// add return exit code and clean up stuff
				conCom.add(MAIN_COMMAND_RETURN);
				conCom.addAll(container.getCleanupCommands(this.TASK));
				conCom.add(EXIT_MAIN_COMMAND_RETURN);
				
				return new String[] {this.writeCommandsToFile(StringUtils.join(conCom, Executor.COMMAND_SEP))};
			}
			else {
				return new String[] {tmpCommandFile};
			}
		}
	}
	
	/**
	 * returns the final joined string
	 * @return
	 */
	protected String getFinalJoinedCommand() {
		return StringUtils.join(this.getFinalCommand(false, false), " ");
	}

	/**
	 * sets a new xml2thread task in order to spawn new slaves!
	 * @param xml2taskThread
	 */
	public static void setXml2Thread(XMLTask2TaskThread xml2taskThread) {
		Executor.xml2taskThread = xml2taskThread;
	}
	
	/**
	 * sets a new watchdog base in order to spawn new slaves!
	 * @param watchdogBase
	 */
	public static void setWatchdogBase(File watchdogBase, File customTmpDir) {
		Executor.watchdogBase = watchdogBase;
		if(customTmpDir == null)
			customTmpDir = watchdogBase;
		Executor.default_working_dir = ExecutorInfo.getWorkingDir(customTmpDir.getAbsolutePath());
	}
	
	/**
	 * writes the lines to a temporary file
	 * @param command
	 * @return
	 */
	public static String writeStringsToFile(ArrayList<String> env, boolean zeroSeparated) {
		String sep = System.lineSeparator();
		if(zeroSeparated) {
			sep = ZERO_SEP;
		}
		File f = Functions.generateRandomTmpExecutionFile(ENV_PREFIX, false);
		try {
			FileWriter w = new FileWriter(f);
			for(String l : env) {
				w.write(l);
				w.write(sep);
			}
			w.flush();
			w.close();
		}
		catch(Exception e) {
			System.out.println("[ERROR] Could not write to create temporary file '"+f.getAbsolutePath()+"'.");
			e.printStackTrace();
			System.exit(1);
		}
		return f.getAbsolutePath();
	}

	/**
	 * sets environment variables that must be handled by some internal mechanisms of the specific executors
	 * @param envVales
	 */
	public void setEnvironmentVariablesToProcessInternally(HashMap<String, String> envVales) {
		this.internalEnv = envVales;
	}
	
	public HashMap<String, String> getInternalEnvVars() {
		return this.internalEnv;
	}
	
	public boolean hasInternalEnvVars() {
		return this.internalEnv != null && this.internalEnv.size() > 0;
	}
}
