package de.lmu.ifi.bio.watchdog.executor.cluster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.ExitTimeoutException;
import org.ggf.drmaa.InvalidJobException;
import org.ggf.drmaa.JobInfo;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;

import de.lmu.ifi.bio.watchdog.executor.MonitorThread;
import de.lmu.ifi.bio.watchdog.helper.AbortedJobInfo;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.task.TaskStatus;

/**
 * Monitors the grid jobs in a separate thread
 * @author Michael Kluge
 *
 */
public class ClusterMonitorThread extends MonitorThread<ClusterExecutor> {
	
	private static final long SLEEP_MILIS = 1000;
	private static String TYPE = "Cluster";
	private Session session = SessionFactory.getFactory().getSession();
	private static ClusterMonitorThread instance = null;
	
	private ClusterMonitorThread() {
		super("ClusterExecutorMonitorThread");
		
		// init the DRM with the default system
        try {
			this.session.init(null);
		} catch (DrmaaException e) {
			e.printStackTrace();
			LOGGER.error("Can not communicate with the default grid system.");
			System.exit(1);
		}
	}
	
	@Override
	protected synchronized boolean monitorJobs() {
		int finished = 0;
		boolean noRelease = true;
		for(Integer gridIDInteger : new ArrayList<Integer>(this.getMonitorTasks().keySet())) {
			String gridID = Integer.toString(gridIDInteger);

			Task t = this.getMonitorTasks().get(gridIDInteger).getTask();
			// check, if job was run and has exited
			if(t.getExecutionCounter() > 0 && !t.hasJobInfo()) {
				try {
					// check, if the task should be terminated
					if(t.isTerminationPending()) {
						this.session.control(gridID, Session.TERMINATE);
						this.remove(gridIDInteger);
						// remove it from the running job list
						t.getExecutor().removeIDofRunningJob(t);
						t.setStatus(TaskStatus.TERMINATED);
						
						// try to release a job of the same type
						if(!t.isMaxRunningRestrictionReached() && !t.getExecutor().isMaxRunningRestrictionReached()) {
							Task onHold = Task.getJobsOnHold(t.getTaskID());
							if(onHold != null) {
								this.releaseHold(onHold);
								noRelease = false;
							}
						}
						continue;
					}
					if(!t.isTaskOnHold()) {
						if(t.getStatus().isWaitingOnQueue()) {
							if(Session.RUNNING == this.session.getJobProgramStatus(gridID)) {
								t.setStatus(TaskStatus.RUNNING);
								t.setHostname(ClusterExecutor.getHostname(gridID, t.getExecutor().getWatchdogBaseDir()));
							}
						}
						if(t.getHost() == null) 
							t.setHostname(ClusterExecutor.getHostname(gridID, t.getExecutor().getWatchdogBaseDir())); // try to get ID from the file written by the watchdog scripts on the disk 
						
						JobInfo info = this.session.wait(gridID, Session.TIMEOUT_NO_WAIT);
						// while task is running this will result in a timeout exception!
						t.setJobInfo(info);

						this.remove(gridIDInteger);
						LOGGER.info("Task with internal ID " + t.getID() + " has been finished on the grid system.");
						finished++;
	
						// check, if another job of the same type is on hold
						if(!t.isMaxRunningRestrictionReached() && !t.getExecutor().isMaxRunningRestrictionReached()) {
							Task onHold = Task.getJobsOnHold(t.getTaskID());
							if(onHold != null) {
								this.releaseHold(onHold);
								noRelease = false;
							}
						}
						
					}
					else { 
						// check, if hold state of that task should be released because it "does not consume any" resources
						if(!t.doesConsumeResources())
							this.releaseHold(t);
						
						// check, if task is still in the queue!!!
						this.session.wait(gridID, Session.TIMEOUT_NO_WAIT);
					}
				} 
				catch(ExitTimeoutException e) { }
				catch(InvalidJobException e) {
					if(t.getHost() == null) 
						t.setHostname(ClusterExecutor.getHostname(gridID, t.getExecutor().getWatchdogBaseDir())); // try to get ID from the file written by the watchdog scripts on the disk
					t.setJobInfo(new AbortedJobInfo());
					this.remove(gridIDInteger);
					LOGGER.error("Task with internal ID " + t.getID() + " was removed from the grid system.");
					finished++;
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		// release the holds of jobs if needed!
		if(noRelease) {
			// run through all jobs
			for(Integer gridIDInteger : new ArrayList<Integer>(this.getMonitorTasks().keySet())) {
				Task t = this.getMonitorTasks().get(gridIDInteger).getTask();
				// check, if task is ok
				if(t.isTaskOnHold() && t.getExecutionCounter() > 0 && !t.hasJobInfo() && !t.isTerminationPending()) {
					// check, if needed resources are there
					if((!t.isMaxRunningRestrictionReached() && !t.getExecutor().isMaxRunningRestrictionReached()) || !t.doesConsumeResources()) {
						try {
							this.releaseHold(t);
						}
						catch(Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		return finished > 0;
	}
	
	@Override
	public void setPauseScheduling(boolean pause) {
		super.setPauseScheduling(pause);
		LinkedHashMap<Integer, ClusterExecutor> tasks = this.getMonitorTasks();
		// do not schedule any more tasks --> set all on hold
		for(int gridID : tasks.keySet()) {
			ClusterExecutor ex = tasks.get(gridID);
			Task t = ex.getTask();
			// if it is in waiting queue --> hold it!
			if(t.isTaskWaitingInQue())
				try { 
					this.session.control(Integer.toString(gridID), Session.HOLD);
					t.setStatus(TaskStatus.WAITING_RESTRICTIONS);
				}
				catch(Exception e) {}
		}
	}

	/**
	 * releases a task out of its holding state
	 * @param t
	 * @return
	 * @throws DrmaaException 
	 */
	protected boolean releaseHold(Task t) throws DrmaaException {
		if(this.isSchedulingPaused())
			return false;
		
		// check, if task is really on hold!
		if(!t.isTaskOnHold())
			return false;
		
		String onHoldGridID = Integer.toString(this.getGridID(t));
		// release the job
		if(onHoldGridID != null) {
			t.setIsOnHold(false);
			t.setStatus(TaskStatus.WAITING_QUEUE);
			this.session.control(onHoldGridID, Session.RELEASE);
			LOGGER.info("Releasing job with ID '" + t.getID() + "'.");
			return true;
		}
		else {
			LOGGER.error("Task '" + t.getID() + "' was not found within the tasks that the grid monitor thread monitors!");
			System.exit(1);
		}
		return false;
	}
	
	/**
	 * returns the gridID of that task or null if no grid ID is found
	 * @param t
	 * @return
	 */
	private synchronized Integer getGridID(Task t) {
		for(int gridID : this.getMonitorTasks().keySet()) {
			Task test = this.getMonitorTasks().get(gridID).getTask();
			if(t.equals(test))
				return gridID;
		}
		return null;
	}
	
	@Override
	protected void remove(int id) {
		super.remove(id);	
		// do clean-up!
		try {
			this.session.synchronize(Arrays.asList(Integer.toString(id)), 15, true);
		}
		catch(Exception e) { e.printStackTrace(); }
	}
	
	@Override
	public void afterLoop() {
		super.afterLoop();
		// do clean-up!
		try {
			this.session.synchronize(Arrays.asList(Session.JOB_IDS_SESSION_ALL), 15, true);
			// and some more!
			this.session.exit();
			this.session = null;
			ClusterMonitorThread.instance = null;
		}
		catch(Exception e) { e.printStackTrace(); }
	}

	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	public long getDefaultWaitTime() {
		return SLEEP_MILIS;
	}
	
	/************************** STATIC METHODS which are meant to be called from outside *************************/
	
	private static synchronized void ensureInstanceIsRunning() {
		if(ClusterMonitorThread.instance == null) {
			ClusterMonitorThread.instance = new ClusterMonitorThread();
			ClusterMonitorThread.instance.start();
		}
	}
	
	public static synchronized String runJob(JobTemplate job) throws DrmaaException {
		ClusterMonitorThread.ensureInstanceIsRunning();
		return ClusterMonitorThread.instance.session.runJob(job);
	}

	public static synchronized JobTemplate createJobTemplate() throws DrmaaException {
		ClusterMonitorThread.ensureInstanceIsRunning();
		return ClusterMonitorThread.instance.session.createJobTemplate();
	}

	public static synchronized void deleteJobTemplate(JobTemplate job) throws DrmaaException {
		ClusterMonitorThread.ensureInstanceIsRunning();
		ClusterMonitorThread.instance.session.deleteJobTemplate(job);
	}
	
	public static synchronized void addTaskToMonitor(ClusterExecutor executor, int id) {
		ClusterMonitorThread.ensureInstanceIsRunning();
		ClusterMonitorThread.instance.addTaskToMonitor(id, executor);
	}

	public static void stopExecution(String id) throws DrmaaException {
		if(id == null)
			return;
		
		ClusterMonitorThread.ensureInstanceIsRunning();
		ClusterMonitorThread.instance.session.control(id, Session.TERMINATE);
	}
	
	@Override
	public void beforeLoop() {

	}
	
}
