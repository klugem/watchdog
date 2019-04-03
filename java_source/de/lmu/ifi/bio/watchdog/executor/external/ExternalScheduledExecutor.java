package de.lmu.ifi.bio.watchdog.executor.external;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.ggf.drmaa.DrmCommunicationException;
import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobTemplate;

import de.lmu.ifi.bio.watchdog.executor.Executor;
import de.lmu.ifi.bio.watchdog.executor.WatchdogThread;
import de.lmu.ifi.bio.watchdog.helper.SyncronizedLineWriter;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.task.TaskStatus;

/**
 * Executes tasks on an external workload manager (SGE, DRMAA, ...)
 * @author Michael Kluge
 *
 */
public abstract class ExternalScheduledExecutor<A extends ExternalExecutorInfo> extends Executor<A> {

	public static final int MAX_WAIT_FOR_INIT = 15000;
	public static final String JOBID2HOSTNAME_FILE = "/tmp/.jobID2hostname.txt";
	private static final String TAB = "\t";
	private static final int NUMBER_OF_TRIES = 10;
	private static final Logger LOGGER = new Logger(LogLevel.INFO);
	private final int RETRY_COUNT;
	private String gridJobID;
		
	/**
	 * must be shadow by overwriting classes
	 * should set thread to an static field
	 * @param thread
	 */
	public static void setExternalScheduledMonitorThread(ExternalScheduledMonitorThread<ExternalScheduledExecutor<?>> thread) {
		throw new IllegalStateException("setExternalScheduledMonitorThread() hasn't been set up in the subclass");
	}
	
	/**
	 * returns the thread that was set using the setExternalScheduledMonitorThread method
	 * @return
	 */
	public abstract ExternalScheduledMonitorThread<ExternalScheduledExecutor<?>> getMonitor();
		
	/**
	 * Constructor
	 * @param task
	 * @param log
	 * @param retryCount
	 * @param execInfo
	 */
	public ExternalScheduledExecutor(Task task, SyncronizedLineWriter log, int retryCount, A execInfo) {
		super(task, log, execInfo);
		this.RETRY_COUNT = retryCount;
	}
	
	public A getExecutorInfo() {
		return this.EXEC_INFO;
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void execute() {	
		this.ensureThatMonitorThreadIsRunning();
		// ignore tasks that should be executed on a slave
		if(this.TASK.willRunOnSlave())
			return;
		int i = 0;
		while(i < NUMBER_OF_TRIES) {
			try {			
				i++;
				if(!this.submitJob(this.TASK, this.RETRY_COUNT)) {
					throw new DrmCommunicationException("Failed to submit a job grid system.");
				}
				else {
					this.LOG.writeLog(this.TASK.getBinaryCall() + " " + StringUtils.join(this.TASK.getArguments()), this.getType(), this.TASK.getID(), EXECUTE);
					break;
				}
			}
			catch(Exception e) {
				e.printStackTrace();
				LOGGER.error("Can not communicate with the default grid system." + ((i < NUMBER_OF_TRIES) ? " Trying again..." : ""));
				try { Thread.sleep(2500); } catch(Exception e2) {}
			}
		}
	}
	
	private void ensureThatMonitorThreadIsRunning() {
		if(this.getMonitor() == null) {
			throw new IllegalStateException("setExternalScheduledMonitorThread() was not called before execute()!");
		}
		ExternalScheduledMonitorThread<ExternalScheduledExecutor<?>> mt = this.getMonitor();
		if(!mt.isAlive() && !mt.wasThreadStartedOnce()) {
			// add thread to run pool
			WatchdogThread.addUpdateThreadtoQue(mt, true, true);
			
			// block until thread is running and connection is active
			long w = mt.getDefaultWaitTime();
			int wait = 0;
			while(!mt.isInitComplete() && wait <= MAX_WAIT_FOR_INIT) {
				try { Thread.sleep(w); } catch(Exception e) {}
				wait += w;
			}
			// test if init is complete now
			if(!mt.isInitComplete()) {
				try {
					throw new IllegalStateException("init() of ExternalScheduledMonitorThread does not end!");
				} catch(Exception e) { e.printStackTrace(); }				
				System.exit(1);
			}
		}
	}

	/**
	 * Returns a JobTemplate without set parameters for the call (setArgs() not called yet)
	 * everything else can be set
	 * @return
	 * @throws DrmaaException
	 */
	protected JobTemplate getPlainJobTemplate() throws DrmaaException {
		this.ensureThatMonitorThreadIsRunning();
		return null; // must be overwritten!
	}
	
	/**
	 * Submits a job to the workload manager system
	 * @param task task to submit
	 * @param retryCount number of tries to contact the system
	 * @return true, if job was submitted successfully
	 */
	private boolean submitJob(Task task, int retryCount) throws Exception {
		boolean isTaskAlreadyRunning = task.isTaskAlreadyRunning();
		int i = 0;
		while(retryCount-i > 0) {
				// hold the job if the maximal number of jobs of that type are currently running
				if(!isTaskAlreadyRunning && task.doesConsumeResources() && (task.isMaxRunningRestrictionReached() || this.EXEC_INFO.isMaxRunningRestrictionReached())) {
					task.setIsOnHold(true);
					task.setStatus(TaskStatus.WAITING_RESTRICTIONS);
				}
				else {
					// don't change anything on already running jobs
					if(!isTaskAlreadyRunning) {
						task.setIsOnHold(false);
					}
					// if task is not on hold --> it is running
					else if(!task.isTaskOnHold()) {
						this.EXEC_INFO.addIDofRunningJob(this.TASK);
					}
				}
						
				String gridJobID = this.getMonitor().submit(task, this);
				i++;
				if(gridJobID != null) {
					this.setGridJobID(gridJobID);
					if(isTaskAlreadyRunning) {
						LOGGER.info("Re-attaching to task with name '" + task.getName()+ "', grid ID '" + gridJobID + "' and internal ID '" + this.TASK.getID() + "'.");
					}
					else {
						LOGGER.info("Task with name '" + task.getName()+ "' was submitted with grid ID '" + gridJobID + "' and internal ID '" + this.TASK.getID() + "'.");
					}
					return true;
				}
		}
		return false;
	}

	/**
	 * returns the grid job ID of the last call of the submitJob function
	 * @return
	 */
	public String getGridJobID() {
		return this.gridJobID;
	}

	/**
	 * sets a new grid job ID
	 * @param gridJobID
	 */
	private void setGridJobID(String gridJobID) {
		this.gridJobID = gridJobID;
	}

	@Override
	public String getType() {
		return this.getMonitor().getType();
	}

	@Override
	public String getID() {
		return this.getType() + "(" + this.EXEC_INFO.getType() + ")";
	}	
	
	@Override
	public void stopExecution() {	
		try {  this.getMonitor().stopExecution(this.gridJobID); }
		catch(Exception e) { e.printStackTrace(); }
		super.stopExecution();
	}
	
	
	/********************************************** STATIC METHODS **********************************************/
	/** 
	 * returns the hostname on which a job was running or null if the hostname could not be found in the mapping file
	 * @param gridID
	 * @param watchdogBaseDir
	 * @return
	 */
	public static String getHostname(String gridID, String watchdogBaseDir) {
		File f = new File(watchdogBaseDir + File.separator + JOBID2HOSTNAME_FILE);
		if(f.exists() && f.canRead()) {
			// read the file
			try {
				List<String> lines = Files.readAllLines(FileSystems.getDefault().getPath(f.getAbsolutePath()));
				for(String l : lines) {
					if(l.startsWith(gridID)) {
						return l.replace(gridID + TAB, "");
					}
					
				}
			}
			catch(Exception e) { e.printStackTrace(); }
		}
		return null;
	}
}
