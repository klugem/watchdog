package de.lmu.ifi.bio.watchdog.executor.cluster;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.HashMap;
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
 * Executes tasks on the grid using the DRMAA libraries
 * @author Michael Kluge
 *
 */
public class ClusterExecutor extends Executor {

	private static final String EXEC_NAME = "cluster";
	public static final String WATCHGOD_CORES = "WATCHDOG_CORES";
	public static final String WATCHGOD_MEMORY = "WATCHDOG_MEMORY";
	private static final String DEV_NULL = "/dev/null";
	public static final String JOBID2HOSTNAME_FILE = "/tmp/.jobID2hostname.txt";
	private static final String TAB = "\t";
	private static final int NUMBER_OF_TRIES = 10;
	private static final String NO_QUE="no suitable queues";
	private static final String WRONG_QUE="Job was rejected because job requests unknown queue";
	private static final Logger LOGGER = new Logger(LogLevel.DEBUG);
	
	private final int RETRY_COUNT;
	private String gridJobID;
		
	/**
	 * Constructor
	 * @param task
	 * @param log
	 * @param retryCount
	 * @param execInfo
	 */
	public ClusterExecutor(Task task, SyncronizedLineWriter log, int retryCount, ClusterExecutorInfo execInfo) {
		super(task, log, execInfo);
		this.RETRY_COUNT = retryCount;
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void execute() {
		// ignore tasks that should be executed on a slave
		if(this.TASK.willRunOnSlave())
			return;
		int i = 0;
		while(i < NUMBER_OF_TRIES) {
			try {
				JobTemplate job = this.getPlainJobTemplate();
				// set arguments, if not a summary script is called
				if(this.isSingleCommand())
					job.setArgs(this.TASK.getArguments());
				
				i++;
				if(!this.submitJob(job, this.RETRY_COUNT)) {
					throw new DrmCommunicationException("Could not submit job to grid system.");
				}
				else {
					this.LOG.writeLog(this.TASK.getBinaryCall() + " " + StringUtils.join(this.TASK.getArguments()), this.getType(), this.TASK.getID(), EXECUTE);
					// add grid job to monitor
					ClusterMonitorThread.addTaskToMonitor(this, Integer.parseInt(this.getGridJobID()));
					ClusterMonitorThread.deleteJobTemplate(job);
					break;
				}
			}
			catch(Exception e) {
				if(e.getMessage() != null && e.getMessage().endsWith(NO_QUE)) {
					LOGGER.error("No valid queue is set.");
					LOGGER.error("Error message: " + e.getMessage());
					System.exit(1);
				}
				else if(e.getMessage() != null && e.getMessage().contains(WRONG_QUE)) {
					LOGGER.error(e.getMessage());
					System.exit(1);
				}
				else {
					e.printStackTrace();
					LOGGER.error("Can not communicate with the default grid system.");
					try { Thread.sleep(2500); } catch(Exception e2) {}
				}
			}
		}
	}
	
	/**
	 * Returns a JobTemplate without set arguments
	 * @return
	 * @throws DrmaaException
	 */
	private JobTemplate getPlainJobTemplate() throws DrmaaException {
			JobTemplate jt = ClusterMonitorThread.createJobTemplate();
			// set the command to call
			jt.setRemoteCommand(this.getFinalCommand(false)[0]); 

			// set the name of the job
			jt.setJobName(this.TASK.getProjectShortCut() + " " + this.TASK.getName() + " " + this.TASK.getID());

			// set the working directory
			jt.setWorkingDirectory(this.getWorkingDir(false));
			
			// set the environment variables
			HashMap<String, String> env = this.getEnvironmentVariables();

			// set infos about the number of used cores and the total memory
			env.put(WATCHGOD_CORES, Integer.toString(((ClusterExecutorInfo) this.EXEC_INFO).getSlots()));
			env.put(WATCHGOD_MEMORY, Integer.toString(((ClusterExecutorInfo) this.EXEC_INFO).getTotalMemorsInMB()));
			jt.setJobEnvironment(env);

			// set stdout
			if(this.TASK.getStdOut(false) != null) {
				File out = this.TASK.getStdOut(true);
				jt.setOutputPath(":" + out.getAbsolutePath());
				if(!this.TASK.isOutputAppended())
					out.delete();
			}
			else
				jt.setOutputPath(":" + DEV_NULL);
			// set stderr
			if(this.TASK.getStdErr(false) != null) {
				File err = this.TASK.getStdErr(true);
				jt.setErrorPath(":" + err.getAbsolutePath());
				if(!this.TASK.isErrorAppended())
					err.delete();
			}
			else
				jt.setErrorPath(":" + DEV_NULL);
			// set stdin
			if(this.TASK.getStdIn() != null)
				jt.setInputPath(":" + this.TASK.getStdIn().getAbsolutePath());
			
			// add memory, slot and queue specification
			String additionalInfo = ((ClusterExecutorInfo) this.EXEC_INFO).getCommandsForGrid();
			if(additionalInfo != null && additionalInfo.length() > 0) {
				jt.setNativeSpecification(additionalInfo);
			}
		return jt;
	}
	
	/**
	 * Submits a job to the DRM system
	 * @param task task to submit
	 * @param retryCount number of tries to contact the DRM system
	 * @return true, if job was submitted successfully
	 * @throws DrmaaException
	 * @throws InterruptedException
	 */
	private boolean submitJob(JobTemplate job, int retryCount) throws DrmaaException, InterruptedException {
		int i = 0;
		while(retryCount-i > 0) {
			try {
				// hold the job if the maximal number of jobs of that type are currently running
				if(this.TASK.doesConsumeResources() && (this.TASK.isMaxRunningRestrictionReached() || this.EXEC_INFO.isMaxRunningRestrictionReached())) {
					job.setJobSubmissionState(JobTemplate.HOLD_STATE);
					this.TASK.setIsOnHold(true);
					this.TASK.setStatus(TaskStatus.WAITING_RESTRICTIONS);
				}
				else {
					this.TASK.setIsOnHold(false);
					this.EXEC_INFO.addIDofRunningJob(this.TASK);
				}
				
				String gridJobID = ClusterMonitorThread.runJob(job);
				this.setGridJobID(gridJobID);
				i++;
				LOGGER.info("Task of type '" + this.TASK.getName() + "' was submitted with grid ID '" + gridJobID + "' and internal ID '" + this.TASK.getID() + "'.");
				return true;
			} catch (DrmCommunicationException e) {
				LOGGER.warn("Retry to submit task on DRM the '"+ i +"'. time..." + e.getMessage());
				Thread.sleep(WatchdogThread.RETRY_WAIT_TIME);
			}
		}
		return false;
	}
	
	/**
	 * Casting of the executor info
	 * @return
	 */
	@SuppressWarnings("unused")
	private ClusterExecutorInfo getExecInfo() {
		return (ClusterExecutorInfo) this.EXEC_INFO;
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

	@Override
	public String getType() {
		return EXEC_NAME;
	}

	@Override
	public String getID() {
		return this.getType() + "(" + ((ClusterExecutorInfo) this.EXEC_INFO).getQueue() + ")";
	}	
	
	@Override
	public void stopExecution() {	
		try { ClusterMonitorThread.stopExecution(this.gridJobID); }
		catch(Exception e) { e.printStackTrace(); }
		super.stopExecution();
	}
}
