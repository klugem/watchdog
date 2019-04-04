package de.lmu.ifi.bio.watchdog.executor.external;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobInfo;

import de.lmu.ifi.bio.watchdog.executor.MonitorThread;
import de.lmu.ifi.bio.watchdog.helper.AbortedJobInfo;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.task.TaskStatus;

/**
 * Monitors jobs submitted to an external workflow manager in a separate thread
 * @author Michael Kluge
 *
 */
public abstract class ExternalScheduledMonitorThread<A extends ExternalScheduledExecutor<?>> extends MonitorThread<A> {
	
	protected ExternalWorkloadManagerConnector connector;
	/**
	 * Constructor
	 * @param name
	 */
	protected ExternalScheduledMonitorThread(String name) {
		super(name);
	}
	
	/**
	 * must return variable connector
	 */
	protected static ExternalWorkloadManagerConnector getExternalWorkloadManagerConnector() {
		throw new IllegalStateException("getExternalWorkloadManagerConnector() hasn't been set up in the subclass");
	}
	
	public static ExternalScheduledMonitorThread<?> getMonitorThreadInstance() {
		throw new IllegalStateException("getMonitorThreadInstance() hasn't been set up in the subclass");
	}
	
	@Override
	protected synchronized void remove(String id) {
		super.remove(id);
		this.connector.remove(id);
	}
	
	@Override
	protected synchronized boolean monitorJobs() {
		int finished = 0;
		try {
			// update the cache first
			this.connector.ensureThatJobStatusIsUpToDate(false);
			
			boolean noRelease = true;
			for(String id : new ArrayList<>(this.getMonitorTasks().keySet())) {
				Task t = this.getMonitorTasks().get(id).getTask();
				// check, if job was run and has exited
				if(t.getExecutionCounter() > 0 && !t.hasJobInfo()) {
				
					// check, if the task should be terminated
					if(t.isTerminationPending()) {
						this.connector.cancelJob(id);
						this.remove(id);
						// remove it from the running job list
						t.getExecutor().removeIDofRunningJob(t);
						t.setStatus(TaskStatus.TERMINATED);
						t.setJobInfo(new AbortedJobInfo());
						
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
						if(t.getStatus().isWaitingOnQueue() && this.connector.isJobRunning(id))
							t.setStatus(TaskStatus.RUNNING);
						if(t.getHost() == null) 
							t.setHostname(this.connector.getNameOfExecutionNode(id, t.getExecutor().getWatchdogBaseDir())); // try to get hostname until some name is set 
	
						// test if job is still running --> do noting
						if(t.isTaskRunning() && this.connector.isJobRunning(id)) {
							continue;
						}
						else {
							JobInfo status = this.connector.getJobInfo(id);
							if(status == null)
								continue;
							
							// while task is running this will result in a timeout exception!
							t.setJobInfo(status);
	
							this.remove(id);
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
					}
					else { 
						// check, if hold state of that task should be released because it "does not consume any" resources
						if(!t.doesConsumeResources())
							this.releaseHold(t);
						
						// check, if task is still in the queue!!!
						if(!this.connector.isJobKnownInGridSystem(id) && !this.connector.isInInitialSubmissionState(id)) {
							// try to get node name one last time
							if(t.getHost() == null) 
								t.setHostname(this.connector.getNameOfExecutionNode(id, t.getExecutor().getWatchdogBaseDir()));
							t.setJobInfo(new AbortedJobInfo());
							this.remove(id);
							LOGGER.error("Task with internal ID " + t.getID() + " was removed from the grid system.");
							finished++;
						}
					}
				}
			}

			// release the holds of jobs if needed!
			if(noRelease) {
				// run through all jobs
				for(String id : new ArrayList<>(this.getMonitorTasks().keySet())) {
					Task t = this.getMonitorTasks().get(id).getTask();
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
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		return finished > 0;
	}
	
	@Override
	public void setPauseScheduling(boolean pause) {
		super.setPauseScheduling(pause);
		// if not in start&stop mode --> set all tasks on hold
		if(!this.isInDetachMode()) {
			LinkedHashMap<String, A> tasks = this.getMonitorTasks();
			// do not schedule any more tasks --> set all on hold
			for(String id : tasks.keySet()) {
				A ex = tasks.get(id);
				Task t = ex.getTask();
				// if it is in waiting queue --> hold it!
				if(t.isTaskWaitingInQue())
					try { 
						this.connector.holdJob(id);
						t.setStatus(TaskStatus.WAITING_RESTRICTIONS);
					}
					catch(Exception e) {}
			}
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
		
		String onHoldGridID = this.getGridID(t);
		// release the job
		if(onHoldGridID != null) {
			t.setIsOnHold(false);
			t.setStatus(TaskStatus.WAITING_QUEUE);
			this.connector.releaseJob(onHoldGridID);
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
	 * returns the id of that task or null if no grid id is found
	 * @param t
	 * @return
	 */
	private synchronized String getGridID(Task t) {
		for(String id : this.getMonitorTasks().keySet()) {
			Task test = this.getMonitorTasks().get(id).getTask();
			if(t.equals(test))
				return id;
		}
		return null;
	}
	
	@Override
	public void beforeLoop() {
		super.beforeLoop();
		this.connector.init();

	}
	
	@Override
	public void afterLoop() {
		HashMap<String, A> ids = new HashMap<>();
		ids.putAll(this.getMonitorTasks());
		super.afterLoop();
		this.connector.clean(ids, this.isInDetachMode());
	}
	
	public boolean isInitComplete() {
		return this.connector.isInitComplete();
	}

	@Override
	public String getType() {
		return this.connector.getExecutorType();
	}
	
	@Override
	public long getDefaultWaitTime() {
		return this.connector.getDefaultWaitTime();
	}
			
	/**
	 * submit the task and on success add it to monitor list 
	 * @param task
	 * @param executor
	 * @return
	 * @throws Exception
	 */
	public synchronized String submit(Task task, A executor) throws Exception {
		String id = null;
		if(task.isTaskAlreadyRunning()) {
			id = task.getExternalExecutorID();
			task.setTaskIsAlreadyRunning(false);
		}
		else {
			id = this.connector.submitJob(task, executor);
		}
		if(id != null) {
			this.addTaskToMonitor(id, executor);
		}
		return id;
	}
	
	/**
	 * stops execution of a job with that id
	 * @param id
	 * @throws Exception
	 */
	public synchronized void stopExecution(String id) throws DrmaaException {
		this.connector.cancelJob(id);
	}	
}
