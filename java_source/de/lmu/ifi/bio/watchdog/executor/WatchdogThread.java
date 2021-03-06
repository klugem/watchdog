package de.lmu.ifi.bio.watchdog.executor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.ggf.drmaa.JobInfo;

import de.lmu.ifi.bio.multithreading.ConvertedMonitorRunnable;
import de.lmu.ifi.bio.multithreading.MonitorRunnable;
import de.lmu.ifi.bio.multithreading.RunPool;
import de.lmu.ifi.bio.multithreading.StopableLoopThread;
import de.lmu.ifi.bio.watchdog.executor.local.LocalExecutor;
import de.lmu.ifi.bio.watchdog.executor.local.LocalExecutorInfo;
import de.lmu.ifi.bio.watchdog.executor.remote.RemoteJobInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.helper.ReadExitCodes;
import de.lmu.ifi.bio.watchdog.helper.ShutdownManager;
import de.lmu.ifi.bio.watchdog.helper.SyncronizedLineWriter;
import de.lmu.ifi.bio.watchdog.resume.AttachInfo;
import de.lmu.ifi.bio.watchdog.slave.Master;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.task.TaskAction;
import de.lmu.ifi.bio.watchdog.task.TaskActionTime;
import de.lmu.ifi.bio.watchdog.task.TaskStatus;
import de.lmu.ifi.bio.watchdog.task.TaskStore;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
/**
 * Schedules the Tasks on the DRM and monitors them.
 * @author Michael Kluge
 *
 */
public class WatchdogThread extends StopableLoopThread {

	public static final String DEFAULT_WORKDIR = "/usr/local/storage/";
	public static final String EXIT_CODE_PATH = ".." + File.separator + "core_lib" + File.separator + "exitCodes.sh";
	public static final int RETRY_WAIT_TIME = 10000; // wait 10s before next contact try
	private static final int SLEEP_MILIS = 500; // sleeps 5 seconds before trying to schedule new stuff
	private static final int MAX_TASKS_NOT_FINISHED = 1024;
	private static final int WORKER_THREADS = 8;
	private static final int MANAGEMENT_THREADS = 8;
	public static final int DEFAULT_HTTP_PORT = 8080;
	
	@SuppressWarnings("unused")
	private static WatchdogThread watchdogThread;
	
	private final Set<Task> TASKS = Collections.synchronizedSet(new LinkedHashSet<Task>());
	private final ArrayList<TaskAction> ON_SHUTDOWN = new ArrayList<>();
	
	private final boolean SIMULATE;
	private final boolean SLAVE_MODE;
	private SyncronizedLineWriter LOG_FILE; 
	private final LocalExecutorInfo SLAVE_EXEC_INFO;
	private final ShutdownManager SHUTDOWN_MANAGER;
	private static RunPool RUN_POOL;
	private HTTPListenerThread webserver;
		
	/**
	 * Constructor
	 * @param simulate
	 * @param watchdogXSDPath
	 * @param executionLog
	 */ 
	public WatchdogThread(boolean simulate, Integer maxRunningOnSlave, File watchdogXSDPath, File executionLog) {
		super(WatchdogThread.class.getSimpleName());
		WatchdogThread.watchdogThread = this;
		TaskStore.clean();
		boolean slaveMode = maxRunningOnSlave != null;
		if(maxRunningOnSlave == null) maxRunningOnSlave = 1;
		this.SIMULATE = simulate;
		this.SLAVE_MODE = slaveMode;

		this.SLAVE_EXEC_INFO = new LocalExecutorInfo(XMLParser.LOCAL, "slave executor", false, false, null, maxRunningOnSlave, watchdogXSDPath.getAbsoluteFile().getParentFile().getParent(), new Environment(XMLParser.DEFAULT_LOCAL_COPY_ENV, true, true), "", null, null, null, null, null);
		
		// set start date
		String qName = AttachInfo.ATTACH_INITIAL_START_TIME;
		if(!AttachInfo.hasLoadedData(qName))
			AttachInfo.setValue(qName, Functions.getCurrentDateAndTime());
		else {
			AttachInfo.setValue(qName, AttachInfo.getLoadedData(qName));
		}
		
		if(executionLog != null) {
			try {
				Task.createFolder(executionLog, true); // create the folder
				executionLog.createNewFile(); // create the file
				this.LOG_FILE = new SyncronizedLineWriter(new BufferedWriter(new FileWriter(executionLog)));
			} catch(Exception e) {
				e.printStackTrace();
				LOGGER.error("Could not open log file '"+executionLog.getAbsolutePath()+"' for writing.");
			}
		}
		// do not store the stuff at any place
		else 
			this.LOG_FILE = new SyncronizedLineWriter();
		
		ReadExitCodes.readExitCodes(Paths.get(watchdogXSDPath.getAbsoluteFile().getParent() + File.separator + EXIT_CODE_PATH));
		
		// register the shutdown manager
		this.SHUTDOWN_MANAGER = new ShutdownManager(this.TASKS);
		Runtime.getRuntime().addShutdownHook(new Thread(this.SHUTDOWN_MANAGER, "ShutdownManager"));	
	}

	@Override
	public int executeLoop() {
		int s = this.scheduleTasks(this.SIMULATE, this.SLAVE_MODE);
		if(s > 0)
			WatchdogThread.LOGGER.debug(s + " tasks have been submitted");
		if(s == -1)
			return 10;	// give some extra time to resolve tasks
		else
			return 1;
	}	
	
	
	/** 
	 * adds a new task to the que
	 * @param t
	 */
	public void addToQue(Task t) {
		// save task in the global list,
		synchronized(this.TASKS) { this.TASKS.add(t); }
		if(this.webserver != null)
			this.webserver.registerNewTask(t);
	}
	
	/**
	 * Executes tasks with resolved dependencies.
	 * @return number of jobs which were submitted
	 */
	private int scheduleTasks(boolean simulate, boolean isSlaveExecutor) {
		int unfinished = TaskStore.getNumberOfUnfinishedTasks();		
		int executed = 0;
		// check each task
		ArrayList<Task> copy;
		synchronized(this.TASKS) { copy = new ArrayList<>(this.TASKS); }
		for(Task t : copy) {
			// check, if it is a detach/attach task --> is already running and must be only added to the monitor thread
			if(t.isTaskAlreadyRunning()) {
				this.execute(t, simulate, isSlaveExecutor);
				continue;
			}
			// NORMAL TEST FROM HERE
			
			// do not schedule normal tasks
			if(unfinished+executed >= MAX_TASKS_NOT_FINISHED && t.getTaskID() >= 0)
				continue;
			
			// ensure that max running property of task is not harmed in slave mode
			if(t.willRunOnSlave() && t.isMaxRunningRestrictionReached())
				continue;
			
			// task was never submitted
			if(t.getStatus().isWaitingOnDependencies()) {
				// check if dependencies are ok
				if(t.hasTaskUnresolvedDependencies(isSlaveExecutor) == false) {
					try {
						if(this.execute(t, simulate, isSlaveExecutor))
							executed++;
					}
					catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		if(unfinished+executed >= MAX_TASKS_NOT_FINISHED)
			return -1;
		return executed;
	}

	/**
	 * Executes the task
	 * @param t
	 * @param simulate
	 * @return
	 */
	private boolean execute(Task t, boolean simulate, boolean isSlaveExecutor) {
		Executor<?> exec = null;
		// ignore the information that comes from that task and only call it locally on an executor
		if(isSlaveExecutor) {
			exec = new LocalExecutor(t, this.LOG_FILE, this.SLAVE_EXEC_INFO);
			t.updateExecutor(this.SLAVE_EXEC_INFO);
		}
		else {
			ExecutorInfo execInfo = t.getExecutor();
			exec = execInfo.getExecutorForTask(t, this.LOG_FILE);
		}
		
		// test if any environment variables should be set
		Environment executorENV = t.getExecutor().getEnv();
		Environment taskENV = t.getTaskEnvironment();
		if(taskENV != null ||executorENV != null) {
			boolean useExternalCommand = false;
			if((taskENV != null && taskENV.useExternalCommand()) || (executorENV != null && executorENV.useExternalCommand()))
				useExternalCommand = true;
			
			// external commands are used --> get commands
			if(useExternalCommand) {
				ArrayList<String> commands = new ArrayList<>();
				if(executorENV != null) 
					commands.addAll(executorENV.getEnvironmentCommands(t.getTaskID(), t.getGroupFileName(), t.getProcessBlockClass(), t.getSubTaskID(), t.getProcessTableMapping()));
				if(taskENV != null) 
					commands.addAll(taskENV.getEnvironmentCommands(t.getTaskID(), t.getGroupFileName(), t.getProcessBlockClass(), t.getSubTaskID(), t.getProcessTableMapping()));
				
				// add env export commands as before commands
				for(String command : commands) 
					exec.addBeforeCommand(command);
			}
			// get HashMap that must be handled by the executor itself (update of variables will likely not work!!!)
			else {
				HashMap<String, String> envVales = new HashMap<>();
				if(executorENV != null) 
					envVales.putAll(executorENV.getEnvironment(t.getTaskID(), t.getGroupFileName(), t.getProcessBlockClass(), t.getSubTaskID(), t.getProcessTableMapping()));
				if(taskENV != null) 
					envVales.putAll(taskENV.getEnvironment(t.getTaskID(), t.getGroupFileName(), t.getProcessBlockClass(), t.getSubTaskID(), t.getProcessTableMapping()));
				
				exec.setEnvironmentVariablesToProcessInternally(envVales);
			}
		}
		
		// add before and after commands from executor info
		ExecutorInfo ei = t.getExecutor();
		for(String c : ei.getBeforeCommands())
			exec.addBeforeCommand(c);
		for(String c : ei.getAfterCommands())
			exec.addAfterCommand(c);

		// execute that task with the given executor
		if(!t.isTaskAlreadyRunning())
			t.setStatus(TaskStatus.WAITING_QUEUE);
		
		// process the command
		boolean alreadyRunning = t.isTaskAlreadyRunning();
		if(!simulate) {
			if(!alreadyRunning)
				exec.getTask().setIsOnHold(true);
			exec.execute();
		}
		else {
			LOGGER.info("Simulating to run command on " +exec.toString()+ ": '" + exec.getTask().getBinaryCall() + " " + StringUtils.join(exec.getTask().getArguments(), " ") + "'");
			t.setStatus(TaskStatus.FINISHED);
		}

		// increase execution counter
		if(!alreadyRunning)
			t.increaseExecutionCounter();
		return true;
	}
	
	
	/** 
	 * true, if it is running on a slave
	 * @return
	 */
	public boolean isRunningOnSlave() {
		return this.SLAVE_MODE;
	}
	
	/**
	 * adds tasks that are performed after watchdog thread is ended but before program terminates
	 * @param onShutdown
	 */
	public void addTerminateCommands(ArrayList<TaskAction> onShutdown) {
		this.ON_SHUTDOWN.addAll(onShutdown);
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<TaskAction> getShutdownEvents() {
		return (ArrayList<TaskAction>) this.ON_SHUTDOWN.clone();
	}
	
	public void stopExecution() {
		this.SHUTDOWN_MANAGER.run();
		MonitorThread.stopAllMonitorThreads(true);
		MonitorThread.stopAllMonitorThreads(false);
		
		// stop the update threads
		RUN_POOL.shutdown();
		// stop master server
		Master.stopServer();
		// send stop request
		this.requestStop(5, TimeUnit.SECONDS);
		WatchdogThread.watchdogThread = null;
		
		this.LOG_FILE.close();
	}
	
	/**
	 * tests how many jobs are currently running in the run pool
	 * @return
	 */
	public int getNumberOfJobsRunningInRunPool() {
		if(RUN_POOL == null)
			return 0;
		return RUN_POOL.getNumberOfShortRunningJobs();
	}
	
	/**
	 * tests if all constantly running tasks can be restarted 
	 * @return
	 */
	public boolean canAllConstantlyRunningTasksBeRestarted() {
		if(RUN_POOL == null)
			return true;
		return RUN_POOL.canAllConstantlyRunningTasksBeRestarted();
	}
	
	/**
	 * should be called when watchdog thread is shutdown because all tasks are finished
	 */
	public void shutdown() {
		Task t = Task.getShutdownTask(this.getShutdownEvents(), this.SLAVE_EXEC_INFO); 
		t.performAction(TaskActionTime.TERMINATE);
		JobInfo info;
		if(t.getErrors().size() > 0) // errors were found!
			info = new RemoteJobInfo(1, true, false);
		else // all ok
			info = new RemoteJobInfo(0, true, false);
		
		// let the checker, find some errors
		t.setJobInfo(info); 
		// stop the update threads
		RUN_POOL.shutdown();
		// stop all running threads
		this.stopExecution();
		WatchdogThread.watchdogThread = null;
	}
	
	public static boolean addUpdateThreadtoQue(MonitorRunnable r, boolean isConstantlyRunning) {
		ensureRunPool();
		return WatchdogThread.RUN_POOL.addRunnable(r, isConstantlyRunning) != null;
	}
	
	public static boolean addUpdateThreadtoQue(Runnable r, boolean isConstantlyRunning, boolean canBeStoppedForDetach) {
		ensureRunPool();
		return WatchdogThread.RUN_POOL.addRunnable(new ConvertedMonitorRunnable(r, canBeStoppedForDetach), isConstantlyRunning) != null;
	}

	public void setWebserver(HTTPListenerThread webserver) {
		this.webserver = webserver;
	}

	@Override
	public long getDefaultWaitTime() {
		return SLEEP_MILIS;
	}

	@Override
	public void afterLoop() {
		RUN_POOL = null;
	}

	@Override
	public void beforeLoop() {
		ensureRunPool();
	}

	private static void ensureRunPool() {
		if(RUN_POOL == null)
			RUN_POOL = new RunPool(WORKER_THREADS, MANAGEMENT_THREADS);
	}

	/**
	 * tests, if all re-attach tasks were scheduled
	 * @return
	 */
	public boolean wereAttachTasksScheduled() {
		ArrayList<Task> copy = null;
		synchronized(this.TASKS) { copy = new ArrayList<>(this.TASKS); }
		for(Task t : copy) {
			// this value is reset to false if a task is executed
			if(t.isTaskAlreadyRunning()) {
				return false;
			}
		}
		return true;
	}

}
