package de.lmu.ifi.bio.watchdog.task;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.Pair;
import org.ggf.drmaa.JobInfo;

import de.lmu.ifi.bio.network.server.ServerConnectionHandler;
import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.executor.local.LocalExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.ActionType;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.FileWatcherLockguard;
import de.lmu.ifi.bio.watchdog.helper.Mailer;
import de.lmu.ifi.bio.watchdog.helper.ReplaceSpecialConstructs;
import de.lmu.ifi.bio.watchdog.interfaces.ErrorChecker;
import de.lmu.ifi.bio.watchdog.interfaces.SuccessChecker;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessBlock;
import de.lmu.ifi.bio.watchdog.resume.ResumeJobInfo;
import de.lmu.ifi.bio.watchdog.slave.Master;
import de.lmu.ifi.bio.watchdog.slave.SlaveStatusHandler;
import de.lmu.ifi.bio.watchdog.slave.clientEvents.TerminateTaskEvent;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask;

/**
 * Implements a task which can be executed by different executor classes
 * @author Michael Kluge
 *
 */
public class Task implements Serializable {

	private static final long serialVersionUID = -7420829492775645742L;
	protected final Logger LOGGER = new Logger(LogLevel.INFO);
	protected static Mailer mailer;
	public static final String SEP = "-";
	public static final String PFEIL = ">";
	private static final String RES_ENDING = ".res";
	
	protected final int TASK_ID;
	protected final int SUB_TASK_ID;
	protected final String NAME;
	protected ExecutorInfo executor;
	protected final String COMMAND;
	protected final LinkedHashMap<String, Pair<Pair<String, String>, String>> DETAIL_ARGUMENTS;
	protected final ArrayList<String> ARGUMENTS = new ArrayList<>();
	protected final ArrayList<ErrorChecker> ERROR_CHECKER = new ArrayList<>();
	protected final ArrayList<SuccessChecker> SUCCESS_CHECKER = new ArrayList<>();
	protected final HashSet<String> DEPENDENCIES = new HashSet<>();
	protected final HashSet<Integer> SLAVE_DEPENDENCIES = new HashSet<>();
	protected final String GROUP_FILE_NAME;
	protected final TreeMap<String, Double> USED_RESOURCES = new TreeMap<>();
	protected final ArrayList<String> ERRORS = new ArrayList<>();
	protected final Class<? extends ProcessBlock> PROCESS_BLOCK_CLASS;
	protected final HashMap<String, Integer> PROCESS_TABLE_MAPPING;
	protected final HashMap<String, String> RETURN_PARAMS = new HashMap<>();
	protected final HashMap<TaskActionTime, ArrayList<TaskAction>> TASK_ACTIONS = new HashMap<>();
	
	private String project;
	protected JobInfo info;
	protected String host;
	protected TaskStatus status = TaskStatus.WAITING_DEPENDENCIES;
	protected Integer exitStatus ;
	protected String terminationSignal;
	protected int executionCounter = 0;
	protected int maxRunningAtOnce = -1;
	protected boolean isOnHold = false;
	protected File STD_IN = null;
	protected File STD_OUT = null;
	protected File STD_ERR = null;
	protected File WORKING_DIR = null;
	private final boolean SAVE_RES;
	protected boolean STR_OUT_APPEND = false;
	protected boolean STR_ERR_APPEND = false;
	protected ActionType notify = null;
	protected ActionType notifyBackup = null;
	protected ActionType checkpoint = null;
	protected boolean isBlocked = false;
	protected boolean terminateTask = false;
	private boolean isScheduledOnSlave = false;
	private boolean isTaskAlreadyRunning4StartANDStop = false;
	protected boolean consumeResources = true;
	protected final Environment ENV;
	private final ArrayList<StatusHandler> STATUS_HANDLER = new ArrayList<>(); // do not send it over network --> transient --> not final
	private String slaveID = null;
	private boolean isRunningOnSlave = false;
	private boolean forceSingleSlaveMode = false; 
	private boolean taskStatusUpdateFinished = false;
	private boolean MIGHT_PB_CONTAIN_FILE_NAMES;
	private String externalExecutorID;
	
	/**
	 * Constructor
	 * @param taskID
	 * @param name
	 * @param executor
	 * @param command
	 * @param arguments
	 * @param dependencies
	 * @param errorChecker
	 * @param successChecker
	 * @param groupFileName
	 * @param stdIn
	 * @param stdOut
	 * @param stdErr
	 * @param stdOutAppend
	 * @param stdErrAppend
	 * @param workingDir
	 * @param processBlockClass
	 * @param completeRawargumentList
	 * @param env
	 */
	public Task(int taskID, String name, ExecutorInfo executor, String command, LinkedHashMap<String, Pair<Pair<String, String>, String>> detailArguments, ArrayList<Task> dependencies,  ArrayList<ErrorChecker> errorChecker, ArrayList<SuccessChecker> successChecker, String groupFileName,
				 File stdIn, File stdOut, File stdErr, boolean stdOutAppend, boolean stdErrAppend, File workingDir, Class<? extends ProcessBlock> processBlockClass, HashMap<String, Integer> processTableMapping, Environment env, ArrayList<TaskAction> taskActions, boolean saveRes, boolean mightPBContainFileNames) {
		this.TASK_ID = taskID;
		this.NAME = name;
		this.executor = executor;
		this.DETAIL_ARGUMENTS = detailArguments;
		this.MIGHT_PB_CONTAIN_FILE_NAMES = processBlockClass != null && mightPBContainFileNames;

		String[] tmp = command.split(" ");
		if(tmp.length > 1)
			command = tmp[0];
		for(int i = 1; i < tmp.length; i++)
			this.ARGUMENTS.add(tmp[i]);
		
		this.COMMAND = command;
		if(detailArguments != null)
			this.ARGUMENTS.addAll(Task.parseArguments(detailArguments));
		if(errorChecker != null) 
			this.ERROR_CHECKER.addAll(errorChecker);
		if(successChecker != null)
			this.SUCCESS_CHECKER.addAll(successChecker);

		this.GROUP_FILE_NAME = groupFileName;
		
		// add dependencies
		if(dependencies != null) {
			for(Task t : dependencies) {
				DEPENDENCIES.add(t.getID());
			}	
		}
		
		// set stream stuff
		this.STD_IN = stdIn;
		this.STD_OUT = stdOut;
		this.STD_ERR = stdErr;
		this.WORKING_DIR = workingDir;
		this.STR_OUT_APPEND = stdOutAppend;
		this.STR_ERR_APPEND = stdErrAppend;
		this.PROCESS_BLOCK_CLASS = processBlockClass;
		this.PROCESS_TABLE_MAPPING = processTableMapping;
		this.SAVE_RES = saveRes;
		
		if(env != null)
			this.ENV = env;
		else {
			this.ENV = new Environment(null, false, false);
		}
		
		// add the task actions
		for(TaskAction a : taskActions) {
			TaskActionTime t = a.getActionTime();
			
			// check, if list for this type is already there
			if(!this.TASK_ACTIONS.containsKey(t)) 
				this.TASK_ACTIONS.put(t, new ArrayList<TaskAction>());
			
			// get the list and add the entry
			this.TASK_ACTIONS.get(t).add(a);
		}
		
		// update the sub task ID
		if(this.GROUP_FILE_NAME != null && this.GROUP_FILE_NAME.length() > 0) {
			if(!TaskStore.globalContainsKey(this.TASK_ID)) 
				TaskStore.globalPut(this.TASK_ID, 0);
			
			// increase the sub task id
			int subTaskID = TaskStore.globalGet(this.TASK_ID) + 1;
			TaskStore.globalPut(this.TASK_ID, subTaskID);
			
			this.SUB_TASK_ID = subTaskID;
		}
		else {
			this.SUB_TASK_ID = -1;
		}
		
		// save task in the global list
		TaskStore.addTask(this);
	} 
	
	/**
	 * converts the detailed list to an arraylist
	 * @param detailArguments
	 * @return
	 */
	public static ArrayList<String> parseArguments(LinkedHashMap<String, Pair<Pair<String, String>, String>> detailArguments) {
		// test if stuff is cached
		int hashCode = detailArguments.hashCode();
		if(!TaskStore.cacheContainsKey(hashCode)) {
			ArrayList<String> ret = new ArrayList<>();
			for(String name : detailArguments.keySet()) {
				Pair<Pair<String, String>, String> p = detailArguments.get(name);
				Pair<String, String> pi = p.getKey();
				String prefix = pi.getKey();
				String quote = pi.getValue();
				String value = p.getValue();
				
				// try to parse double
				boolean okNumeric = false;
				try { Double.parseDouble(value); okNumeric = true; } catch(Exception e) {}
				
				// add the flag or parameter with prefix
				if(prefix != null && !prefix.isEmpty())
					ret.add(prefix+name);
				// it is not a option!
				if(value != null) {
					if(quote != null  && !quote.isEmpty() && !okNumeric) // do not quote numierc arguments!
						ret.add(quote + value + quote);
					else
						ret.add(value);
				}
			}
			TaskStore.cachePut(hashCode, ret);
		}
		return TaskStore.cacheGet(hashCode);
	}

	public static Task getShutdownTask(ArrayList<TaskAction> shutdownActions, ExecutorInfo e) {
		return new Task(0, "on shutdown event", e, "", null, null, null, null, null, null, null, null, false, false, null, null, null, null, shutdownActions, false, false);
	}
	
	public void addModuleVersionParam() {
		
	}
	
	/**
	 * complete ID of that task (if task from processGroup --> taskID:subTaskID)
	 * @return
	 */
	public String getID() {
		if(this.SUB_TASK_ID == -1) {
			return Integer.toString(this.getTaskID());
		}
		else {
			return this.getTaskID() + SEP + this.getSubTaskID();
		}
	}
	
	/**
	 * performs the action types of a specific type
	 * @param time
	 * @return
	 */
	public boolean performAction(TaskActionTime time) {
		boolean ret = true;
		if(this.TASK_ACTIONS.containsKey(time)) {
			// perform all the actions
			for(TaskAction a : this.TASK_ACTIONS.get(time)) {
				if(!a.wasExecuted()) {
					if(a.isUncoupledFromExecutor() || this.isRunningOnSlave() || this.getExecutor() instanceof LocalExecutorInfo) {
						a.performAction();
						for(String error : a.getErrors()) {
							ret = false;
							this.addError(error);
						}
					}
				}
			}
		}
		// clears these files because task was not run because of errors in begin action!!!
		if(time.isBefore() && this.hasErrors()) {
			this.setStdout(null);
			this.setStderr(null);
		}
		return ret;
	}
	
	public void setStderr(File err) {
		this.STD_ERR = err;
	}
	
	public void setStdout(File out) {
		this.STD_OUT = out;
	}
	
	/**
	 * should only be used in case of slave mode!
	 * @param e
	 */
	public void updateExecutor(ExecutorInfo e) {
		this.executor = e;
	}
	
	/**
	 * Returns the task ID of that task
	 * @return
	 */
	public int getTaskID() {
		return this.TASK_ID;
	}
	
	/**
	 * returns the sub task id of that job
	 * @return
	 */
	public int getSubTaskID() {
		return this.SUB_TASK_ID;
	}
	
	/**
	 * gets the name of this task
	 * @return
	 */
	public String getName() {
		return this.NAME;
	}
	
	/**
	 * returns the command which should be executed
	 * @return
	 */
	public String getBinaryCall() {
		return this.COMMAND;
	}
	
	/**
	 * returns the arguments which should be used when this task is executed
	 * @return
	 */
	public ArrayList<String> getArguments() {
		return new ArrayList<String>(this.ARGUMENTS);
	}	
	
	/**
	 * returns the IDs of the dependencies of that task
	 * @return
	 */
	public HashSet<String> getDependencies() {
		return new HashSet<String>(this.DEPENDENCIES);
	}
	
	/**
	 * adds a dependency that is valid for all subtasks of the same base tasks that are scheduled on a slave
	 * @param id
	 */
	public void addSlaveDependency(int id) {
		this.SLAVE_DEPENDENCIES.add(id);
	}

	/**
	 * returns the group file name of that task which is needed to check, if a separate task depends on this one
	 * @return
	 */
	public String getGroupFileName() {
		return this.GROUP_FILE_NAME;
	}
	
	/**
	 * gets the job info
	 * @return
	 */
	public JobInfo getJobInfo() {
		return this.info;
	}
	
	/**
	 * true, if it is a slave task
	 * @return
	 */
	public boolean isRunningOnSlave() {
		return this.isRunningOnSlave;
	}
	
	/**
	 * retuns the slave ID if one is set!
	 * @return
	 */
	public String getSlaveID() {
		return this.slaveID;
	}
	
	/**
	 * sets a new statushandler
	 * @param sh
	 */
	public synchronized void addStatusHandler(StatusHandler sh) {
		this.STATUS_HANDLER.add(sh);
	}

	/**
	 * gets the status of the job
	 * @return
	 */
	public TaskStatus getStatus() {
		return this.status;
	}
	
	/**
	 * gets the name of the termination signal if one was sent
	 * @return
	 */
	public String getTerminationSignal() {
		return this.terminationSignal;
	}
	
	/**
	 * sets a JobInfo when the task was executed
	 * @param info
	 */
	public void setJobInfo(JobInfo info) {
		this.info = info;
		this.getExecutor().removeIDofRunningJob(this);
		this.updateStatus();
	}
	
	/**
	 * blocks a job because of a checkpoint
	 */
	public void blockTask() {
		if(!this.isBlocked())
			this.isBlocked = true;
	}
	
	/**
	 * releases a task out of its active checkpoint
	 */
	public boolean releaseTask() {
		if(this.isBlocked()) {
			this.isBlocked = false;
			this.setStatus(this.getStatus()); // ensure that symbol on the GUI is changed after all stuff is released
			return true;
		}
		return false;
	}
	
	public Integer getExitStatus() {
		return this.exitStatus;
	}
	
	/**
	 * resets a task and makes it ready for rescheduling
	 */
	public boolean restartTask() {
		if(!this.hasTaskFinishedWithoutBlockingInfo()) {
			this.terminateTask();
			try { Thread.sleep(2500); } catch(Exception e) {} // give the system some time to terminate the stuff
			this.isOnHold = false;
			this.isBlocked = false;
			this.terminateTask = false;
			this.terminationSignal = null;
			this.exitStatus = null;
			this.externalExecutorID = null;
			this.isTaskAlreadyRunning4StartANDStop = false;
			this.host = null;
			this.info = null;
			this.ERRORS.clear();
			this.setStatus(TaskStatus.WAITING_DEPENDENCIES);
			this.RETURN_PARAMS.clear();
			this.notify = this.notifyBackup;

			// delete the old log files or it might fail again
			this.deleteLogFiles();
			
			// reset the error checkers
			for(ErrorChecker e : this.ERROR_CHECKER)
				e.reset();
			
			return true;
		}
		return false;
	}
	
	/**
	 * true, if the job is currently blocked by an active checkpoint
	 * @return
	 */
	public boolean isBlocked() {
		return this.isBlocked;
	}
	
	/**
	 * sets an optional project name
	 * @param project
	 */
	public void setProject(String project) {
		this.project = project;
	}
	
	/**
	 * returns the used resources
	 * @return
	 */
	public TreeMap<String, Double> getUsedResources() {
		return new TreeMap<String, Double>(this.USED_RESOURCES);
	}
	
	 /**
	  * returns the errors this the error checkers found
	  * @return
	  */
	public ArrayList<String> getErrors() {
		return new ArrayList<String>(this.ERRORS);
	}
	
	public boolean hasErrors() {
		return this.ERRORS.size() > 0;
	}
	
	/**
	 * returns the execution counter
	 * @return
	 */
	public int getExecutionCounter() {
		return this.executionCounter;
	}
	
	/**
	 * Increases the execution counter
	 */
	public void increaseExecutionCounter() {
		this.executionCounter++;
		this.LOGGER.debug("Task with ID " + this.getID() + " was submitted the " + this.executionCounter + ". time.");
	}
	
	/**
	 * set the slave task ID of that task
	 */
	public void setSlaveTaskID(String slaveID) {
		this.slaveID = slaveID;
	}
	
	/**
	 * set the task to be a slave task
	 */
	public void setIsRunningOnSlave(boolean runningOnSlave) {
		this.isRunningOnSlave = runningOnSlave;
	}
	
	/**
	 * Must be used for slave mode but should not get zero!
	 */
	public void decreaseExecutionCounter() {
		if(this.executionCounter > 0)
			this.executionCounter--;
	}
	
	/**
	 * maximal n jobs of that type can run at the same time
	 * @param maxRunning
	 */
	public void setMaxRunning(int maxRunning) {
		if(maxRunning > 0) 
			this.maxRunningAtOnce = maxRunning;
	}
	
	/**
	 * returns the value of the max running parameter
	 * @return
	 */
	public int getMaxRunning() {
		return this.maxRunningAtOnce;
	}
	
	/**
	 * true, if no new job should be scheduled due to the max running restriction
	 * @return
	 */
	public boolean isMaxRunningRestrictionReached() {
		int maxRunning = this.getMaxRunning();
		return maxRunning > 0 && Task.getUnfinishedJobs(this.getTaskID(), true) >= maxRunning;
	}
	
	
	/**
	 * sets a new is on hold status
	 * @param isOnHold
	 */
	public void setIsOnHold(boolean isOnHold) {
		this.isOnHold = isOnHold;
		
		if(isOnHold == false) {
			this.getExecutor().addIDofRunningJob(this);
			this.setStatus(TaskStatus.WAITING_QUEUE);
		}
		else 
			this.setStatus(TaskStatus.WAITING_RESTRICTIONS);
	}
	
	/**
	 * true, if task is on hold
	 * @return
	 */
	public boolean isTaskOnHold() {
		return this.isOnHold;
	}
	
	/**
	 * updates the status of the task when it is finished
	 */
	private void updateStatus()  {
		FileWatcherLockguard.addTask(this);
	}
	
	/********************************** CHECKER **********************************/
	public boolean hasTaskUnresolvedDependencies(boolean isSlaveExecutor) {
		// ensure that all subtasks that are scheduled on that slave are finished
		if(isSlaveExecutor) {
			for(int taskID : this.SLAVE_DEPENDENCIES) {
				if(Task.hasUnfinishedJobs(taskID))
					return true;
			}
		}
		else {
			for(String taskID : this.getDependencies()) {
				Task dep = getTask(taskID);
				// remove this dependency from the list because no information about it is available
				if(dep == null) {
					this.DEPENDENCIES.remove(taskID);
				}
	
				// if, it depends on a task, which is ignore, ignore it itself!
				if(!dep.isTaskIgnored()) {
					this.setStatus(TaskStatus.IGNORE);
					return true;
				}
			
				// test if task has finished correctly
				if(!dep.hasTaskFinished()) {
					return true;
				}
			}
		}
		return false; 
	}
	
	public boolean isTaskWaitingInQue() {
		return TaskStatus.WAITING_QUEUE.equals(this.getStatus());
	}
	
	public boolean isTaskWaitingOnDependencies() {
		return TaskStatus.WAITING_DEPENDENCIES.equals(this.getStatus());
	}
	
	public boolean isSyntaxWrong() {
		return TaskStatus.FAILED_SYNTAX.equals(this.getStatus());
	}
	
	public boolean isSubtask() {
		return this.getSubTaskID() != -1;
	}
	
	public boolean isTaskRunning() {
		return TaskStatus.RUNNING.equals(this.getStatus());
	}

	public boolean hasTaskFinished() {
		return !this.isBlocked && (TaskStatus.FINISHED.equals(this.getStatus()) || TaskStatus.RESOLVED.equals(this.getStatus()));
	}
	
	public boolean hasTaskFinishedWithoutBlockingInfo() {
		return TaskStatus.FINISHED.equals(this.getStatus()) || TaskStatus.RESOLVED.equals(this.getStatus());
	}

	public boolean hasTaskFailed() {
		return TaskStatus.FAILED.equals(this.getStatus());
	}
	
	public boolean isTaskIgnored() {
		return TaskStatus.IGNORE.equals(this.getStatus());
	}
	
	public boolean wasTaskKilled() {
		return TaskStatus.KILLED.equals(this.getStatus());
	}
	
	public boolean wasTaskKilledDueSignal() {
		return this.wasTaskKilled() && this.getTerminationSignal() != null;
	}
	
	public boolean hasJobInfo() {
		return this.info != null;
	}	
	
	public boolean hasGroupFileName() {
		return this.GROUP_FILE_NAME != null && this.GROUP_FILE_NAME.length() > 0;
	}
	/*****************************************************************************/
	
	/**
	 * returns a project name if some was set
	 * @return
	 */
	public String getProjectName() {
		return this.project;
	}
	
	/**
	 * returns a project shortcut if a project was set
	 * @return
	 */
	public String getProjectShortCut() {
		if(this.project != null && this.project.length() > 0)
			return this.project.substring(0, 1);
		return PFEIL;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
	
	/******************************* STATIC STUFF ********************************/
	/*****************************************************************************/
	public static Task getTask(String taskID) {
		if(TaskStore.taskContainsKey(taskID)) {
			return TaskStore.taskGet(taskID);
		}
		return null;
	}
	
	/**
	 * Counts how many jobs which have the same ID are finished
	 * @param id
	 * @return
	 */
	public static int getFinishedJobs(int id) {
		return Task.getCountOfJobs(id, true, false);
	}
	
	/**
	 * Counts how many jobs which have the same ID are not finished yet
	 * @param id
	 * @return
	 */
	public static int getUnfinishedJobs(int id, boolean ignoreWithoutResources) {
		return Task.getCountOfJobs(id, false, ignoreWithoutResources);
	}
	
	/**
	 * true if some not finished tasks are there with that id
	 * @param id
	 * @return
	 */
	public static boolean hasUnfinishedJobs(int id) {
		return getUnfinishedJobs(id, false) > 0;
	}
	
	/**
	 * Counts how many jobs which have the same ID are finished or not finished depending on finished
	 * @param id
	 * @param finished
	 * @return
	 */
	private static int getCountOfJobs(int id, boolean finished, boolean ignoreWithoutResources) {
		int c = 0;
		for(Task t : TaskStore.getTasksToRead().values()) {
			if(ignoreWithoutResources && !t.consumeResources)
				continue;
			
			if(t.getTaskID() == id && (t.isScheduledOnSlave() || (t.getExecutionCounter() > 0  && !t.isTaskOnHold()))) {
				if(finished) {
					if(t.hasTaskFailed() || t.hasTaskFinished()) {
						c++;
					}
				}
				else if(!t.hasTaskFailed() && !t.hasTaskFinished() && !t.isBlocked()) {
					c++;
				}
			}
		}
		return c;
	}

	/**
	 * Gets a task which is currently on hold or null if none with that ID is on hold
	 * @param id
	 * @return
	 */
	public static Task getJobsOnHold(int id) {
		for(Task t : TaskStore.getTasksToRead().values()) {
		
			if(t.getExecutionCounter() > 0 && t.getTaskID() == id && !t.isTerminationPending() && t.isTaskOnHold())
				return t;
		}
		return null;
	}
	
	
	/**
	 * Returns a file for stdin or null if none is set
	 * @return
	 */
	public File getStdIn() {
		return this.STD_IN;
	}
	
	/**
	 * Returns a file for stderr or null if none is set
	 * creates the folder if not null
	 * @param createDir
	 * @return
	 */
	public File getStdErr(boolean createDir) {
		if(createDir)
			createFolder(this.STD_ERR, true);
		return this.STD_ERR;
	}
	
	/**
	 * Returns a file for stdout or null if none is set
	 * @param createDir
	 * @return
	 */
	public File getStdOut(boolean createDir) {
		if(createDir)
			createFolder(this.STD_OUT, true);
		return this.STD_OUT;
	}
	
	public static void createFolder(File f, boolean isFile) {
		if(f != null) {
			if(isFile && f.getParentFile() != null && !f.getParentFile().exists())
				f.getParentFile().mkdirs();
			else if(!isFile && !f.exists())
				f.mkdirs();
		}
	}
	
	/**
	 * Returns the current working directory
	 * @param createDir
	 * @return
	 */
	public File getWorkingDir(boolean createDir) {
		if(createDir)
			createFolder(this.WORKING_DIR, false);
		return this.WORKING_DIR;
	}
	
	/**
	 * true, if stdout should be appended if the file is already existing
	 * @return
	 */
	public boolean isOutputAppended() {
		return this.STR_OUT_APPEND;
	}
	
	/**
	 * true, if stderr should be appended if the file is already existing
	 * @return
	 */
	public boolean isErrorAppended() {
		return this.STR_ERR_APPEND;
	}
	
	/**
	 * returns the executor which should be used to execute this task
	 * @return
	 */
	public ExecutorInfo getExecutor() {
		return this.executor;
	}
	

	/**
	 * sets a new notify type
	 * @param notify
	 */
	public void setNotify(ActionType notify) {
		this.notify = notify;
		
		// store information for later, if task must be reseted.
		if(this.notifyBackup == null)
			this.notifyBackup = this.notify;
	}

	/**
	 * sets a new checkpoint type
	 * @param checkpoint
	 */
	public void setCheckpoint(ActionType checkpoint) {
		this.checkpoint = checkpoint;
	}
	
	/**
	 * returns the set notify type
	 * @return
	 */
	public ActionType getNotify() {
		return this.notify;
	}
	
	/**
	 * returns the set checkpoint type
	 * @return
	 */
	public ActionType getCheckpoint() {
		return this.checkpoint;
	}
	
	/**
	 * sets a mail which is used to notify a user when a task has been finished / failed
	 * @param mail
	 */
	public static void setMail(Mailer mailer) {
		if(mailer != null && mailer.hasMail())
			Task.mailer = mailer;
		else
			Task.mailer = null;
	}
	
	/**
	 * Returns the mailer
	 * @return
	 */
	public static Mailer getMailer() {
		return Task.mailer;
	}
	
	/**
	 * tests, if a mail is set
	 * @return
	 */
	public static boolean isMailSet() {
		return Task.mailer != null;
	}

	/**
	 * sets a hostname on which the task was running in case of grid execution
	 * @param hostname
	 */
	public void setHostname(String hostname) {
		this.host = hostname;
	}
	
	/**
	 * hostname on which the job was running or null if none was set
	 * @return
	 */
	public String getHost() {
		return this.host;
	}
	
	/**
	 * returns the type of the process block class which created this task or null if none did
	 * @return
	 */
	public Class<? extends ProcessBlock> getProcessBlockClass() {
		return PROCESS_BLOCK_CLASS;
	}

	public HashMap<String, Integer> getProcessTableMapping() {
		return this.PROCESS_TABLE_MAPPING;
	}
	
	public synchronized void setStatus(TaskStatus status) {
		this.status = status;
		if(!(this.info instanceof ResumeJobInfo)) {
			for(StatusHandler sh : this.STATUS_HANDLER)
				sh.handle(this);
		}
	}
	
 	/**
	 * call to terminate a task
	 * @return
	 */
	public boolean isTerminationPending() {
		return this.terminateTask;
	}
	
	/**
	 * true, if task should be terminated
	 * @return
	 */
	public void terminateTask() {
		this.terminateTask = true;

		// check, if it is running on a slave
		String id = this.getID();
		ServerConnectionHandler c = Master.getExecutingSlave(id);
		if(c != null) {
			try { 
				c.send(new TerminateTaskEvent(id));
				this.setStatus(TaskStatus.TERMINATED);
			}
			catch(Exception e) { e.printStackTrace(); }
		}
	}
	
	/**
	 * destroys the task and stops execution, if it is currently executed
	 */
	public void destroy() {
		TaskStore.taskRemove(this.getID());
		this.terminateTask();
	}

	/**
	 * adds an error checker later on
	 * @param watchdogErrorCatcher
	 */
	public void addErrorChecker(ErrorChecker watchdogErrorCatcher) {
		this.ERROR_CHECKER.add(watchdogErrorCatcher);
	}
	
	/** adds an success checker later on
	 * @param watchdogSuccessCatcher
	 */
	public void addSuccessChecker(SuccessChecker watchdogSuccessCatcher) {
		this.SUCCESS_CHECKER.add(watchdogSuccessCatcher);
	}

	/**
	 * delete 
	 */
	public void deleteLogFiles() {
		try {
			File out = this.getStdOut(false);
			File err = this.getStdErr(false);
			
			if(out != null) 
				out.delete();
			
			if(err != null)
				err.delete();
		}
		catch(Exception e) {}
	}

	/**
	 * can be used to set the return parameter of a task
	 * @param validReturnParameters
	 */
	public void setReturnParams(HashMap<String, String> validReturnParameters) {
		this.RETURN_PARAMS.putAll(validReturnParameters);
	}
	
	/**
	 * return the return parameter of this task
	 * @param validReturnParameters
	 */
	public HashMap<String, String> getReturnParams() {
		return this.RETURN_PARAMS;
	}
	
	/**
	 * true, if the task has some return parameters
	 * @return
	 */
	public boolean hasReturnParams() {
		return this.RETURN_PARAMS.size() > 0;
	}
	
	/**
	 * can be set to true to ignore the resource restrictions
	 * @param b
	 */
	public void setConsumeResources(boolean consumeResources) {
		this.consumeResources = consumeResources;
	}
	
	/**
	 * returns weather the task consumes resources or not.
	 * @return
	 */
	public boolean doesConsumeResources() {
		return this.consumeResources;
	}
	
	/**
	 * gets the environment that is set for that task
	 * @return
	 */
	public Environment getTaskEnvironment() {
		return this.ENV;
	}
		 
	/**
	 * shebang for external export
	 * @return
	 */
	public String getShebang() {
		if(this.executor == null)
			return null;
		
		return this.executor.getShebang();
	}

	/**
	 * adds a error to the thread
	 * @param message
	 */
	public void addError(String message) {
		this.ERRORS.add(message);
	}

	/**
	 * checks, if some task actions are set
	 * @return
	 */
	public boolean hasTaskActions() {
		return this.TASK_ACTIONS.size() > 0;
	}
	
	/**
	 * returns the actions that should be executed when the slave or watchdog thread will die
	 * @return
	 */
	public ArrayList<TaskAction> getOnKillSlaveActions() {
		return this.TASK_ACTIONS.get(TaskActionTime.TERMINATE) != null ? new ArrayList<>(this.TASK_ACTIONS.get(TaskActionTime.TERMINATE)) : new ArrayList<>();
	}

	/**
	 * true, if the task will run on a slave
	 * @return
	 */
	public boolean willRunOnSlave() {
		return !this.isRunningOnSlave() && (this.getExecutor().isStick2Host() || this.hasTaskActions() || this.isSingleSlaveModeForced());
	}

	/**
	 * actions can be deleted once after a task is transfered to the master
	 */
	public void deleteActions() {
		for(TaskActionTime t : this.TASK_ACTIONS.keySet()) {
			int i = 0;
			for(TaskAction a : new ArrayList<TaskAction>(this.TASK_ACTIONS.get(t))) {
				if(!a.isUncoupledFromExecutor())
					this.TASK_ACTIONS.get(t).remove(i);
				else
					i++;
			}
		}
	}

	/**
	 * forces the single slave mode in cases in which transfers should be made
	 * @param forceSingleSlaveMode
	 */
	public void setForceSingleSlaveMode(boolean forceSingleSlaveMode) {
		this.forceSingleSlaveMode = forceSingleSlaveMode;
	}
	
	/**
	 * can be set to force single slave mode
	 * @return
	 */
	public boolean isSingleSlaveModeForced() {
		return this.forceSingleSlaveMode;
	}

	/**
	 * returns the detailed arguments for display
	 * @return 
	 */
	public final LinkedHashMap<String, Pair<Pair<String, String>, String>> getDetailedArguments() {
		return this.DETAIL_ARGUMENTS;
	}

	public synchronized void removeStatusHandler(Class<SlaveStatusHandler> removeType) {
		ArrayList<StatusHandler> copy = new ArrayList<>();
		copy.addAll(this.STATUS_HANDLER);
		for(StatusHandler sh : copy) {
			if(removeType.isInstance(sh))
				this.STATUS_HANDLER.remove(sh);
		}
	}

	public void setIsScheduledOnSlave(boolean scheduledOnSlave) {
		this.isScheduledOnSlave = scheduledOnSlave;
	}
	
	public boolean isScheduledOnSlave() {
		return this.isScheduledOnSlave;
	}

	public String getDisplayGroupFileName() {
		if(this.hasGroupFileName())
			return this.getGroupFileName();
		else
			return this.GROUP_FILE_NAME;
	}
	
	/**
	 * is used to remove StatusHandler that are not serializable
	 * @param out
	 * @throws IOException
	 */
	private synchronized void writeObject(ObjectOutputStream out) throws IOException {
		// remove handlers that can not be saved temporary
		ArrayList<StatusHandler> tmp = new ArrayList<>();
		for(Iterator<StatusHandler> it = this.STATUS_HANDLER.iterator(); it.hasNext(); ) {
			StatusHandler s = it.next();
			if(!(s instanceof Serializable)) {
				it.remove();
				tmp.add(s);
			}
		}
        // default serialization
		try {
			out.defaultWriteObject();
		} catch(Exception e) { e.printStackTrace(); System.err.println("Task with id " + this.getID() + " failed to write to file."); throw(e);}

        // add handlers again
        this.STATUS_HANDLER.addAll(tmp);
    }

	public void setTaskStatusUpdateFinished() {
		this.LOGGER.debug("Task status update for task '"+this.getID()+"' is finished!");
		this.taskStatusUpdateFinished = true;
	}
	
	public boolean isTaskStatusUpdateFinished() {
		return this.taskStatusUpdateFinished;
	}
	
	public File getSaveResFilename() {
		if(this.SAVE_RES == false)
			return null;
		File f = this.getStdOut(false);
		return new File(f.getAbsolutePath() + RES_ENDING);
	}
	
	public boolean mightProcessblockContainFilenames() {
		return this.MIGHT_PB_CONTAIN_FILE_NAMES;
	}
	
	/** 
	 * for start&stop mode
	 */
	public boolean isTaskAlreadyRunning() {
		return this.isTaskAlreadyRunning4StartANDStop;
	}
	/** 
	 * for start&stop mode
	 */
	public void setTaskIsAlreadyRunning(boolean value) {
		this.isTaskAlreadyRunning4StartANDStop = value;
	}

	public String getExternalExecutorID() {
		return this.externalExecutorID;
	}
	public void setExternalExecutorID(String externalID) {
		this.externalExecutorID = externalID;
	}
	public boolean hasExternalExecutorID() {
		return this.getExternalExecutorID() != null;
	}

	/**
	 * sets environment variables that are used and set by Watchdog
	 * @param name
	 * @param value
	 */
	public void addEnvVariable(String name, String value) {
		this.ENV.add(name, value, null, false, false);
	}
}