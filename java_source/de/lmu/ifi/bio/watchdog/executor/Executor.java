package de.lmu.ifi.bio.watchdog.executor;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

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
	
	public static final String COMMAND_SEP = " && ";
	public static final String EXECUTE = "execute";
	public static final String WATCHGOD_ENV_IDENTIFIER = "IS_WATCHDOG_JOB";
	public static final String WATCHGOD_ENV_IDENTIFIER_VALUE = "1";
	protected static final String EXECUTE_PREFIX = "execute_";
	
	public static String default_working_dir;
	private static final String SEPARATOR = "=";
	private static XMLTask2TaskThread xml2taskThread;
	private static File watchdogBase;
	
	private final Logger LOGGER = new Logger(LogLevel.DEBUG);
	protected final Task TASK;
	protected final HashMap<String, String> ENVIRONMENT = new HashMap<>();
	protected final SyncronizedLineWriter LOG;
	protected final ArrayList<String> PRE_COMMAND = new ArrayList<>();
	protected final A EXEC_INFO;
	
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
		this.ENVIRONMENT.put(WATCHGOD_ENV_IDENTIFIER, WATCHGOD_ENV_IDENTIFIER_VALUE);
		
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
				XMLTask slave = Master.addSlave(slaveID, this.TASK, Executor.watchdogBase, (ExecutorInfo) this.EXEC_INFO.clone(), this.EXEC_INFO.env);
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
	 * sets new environment values
	 * @param environment
	 */
	public void setEnvironment(HashMap<String, String> environment) {
		this.ENVIRONMENT.putAll(environment);
	}
	
	/**
	 * returns a set of formated environment variables with name=value
	 * @return
	 */
	public ArrayList<String> getFormatedEnvironmentVariables() {
		ArrayList<String> e = new ArrayList<>();
		for(String v : this.ENVIRONMENT.keySet()) {
			e.add(v + SEPARATOR + this.ENVIRONMENT.get(v));
		}
		return e;
	}
	
	/**
	 * returns the set of environment variables
	 * @return
	 */
	public HashMap<String, String> getEnvironmentVariables() {
		return new HashMap<>(this.ENVIRONMENT);
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
	public void addPreCommand(String command) {
		this.PRE_COMMAND.add(command);
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
			w.write(this.TASK.getShebang());
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
		return this.PRE_COMMAND.size() == 0;
	}
	
	/**
	 * either returns the command itself, if it is only one command are sum up all command in a script
	 * @return
	 */
	public String[] getFinalCommand(boolean removeQuoting) {
		ArrayList<String> c = new ArrayList<>();
		c.addAll(this.PRE_COMMAND);
		
		// no script needed
		if(c.size() == 0) {
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
			c.add(this.TASK.getBinaryCall() + (this.TASK.getArguments().size() > 0 ? " " + StringUtils.join(this.TASK.getArguments(), " ") : ""));
			return new String[] {this.writeCommandsToFile(StringUtils.join(c, Executor.COMMAND_SEP))};
		}
	}
	
	/**
	 * returns the final joined string
	 * @return
	 */
	protected String getFinalJoinedCommand() {
		return StringUtils.join(this.getFinalCommand(false), " ");
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
	public static void setWatchdogBase(File watchdogBase) {
		Executor.watchdogBase = watchdogBase;
		Executor.default_working_dir = ExecutorInfo.getWorkingDir(watchdogBase.getAbsolutePath());
	}
}
