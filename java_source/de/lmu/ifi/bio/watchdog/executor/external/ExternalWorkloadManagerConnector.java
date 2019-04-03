package de.lmu.ifi.bio.watchdog.executor.external;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobInfo;
import org.ggf.drmaa.TryLaterException;

import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.task.Task;

/**
 * Interface that must be implemented in order to support an external workfload manager
 * @author kluge
 *
 */
public abstract class ExternalWorkloadManagerConnector<A extends ExternalScheduledExecutor<?>> {
	
	private static final long DEFAULT_WAIT_TIME = 1000;
	public static final String DEV_NULL = "/dev/null";
	public final Logger LOGGER;
	
	protected final HashMap<String, String> CACHED_JOB_STATUS = new HashMap<>(); // stores information about running jobs
	protected final HashMap<String, Integer> SUBMITTED_JOB_STATUS = new HashMap<>(); // stores information about jobs that were submitted but not visible
	protected long lastCacheUpdate = 0;
	public static final int MAX_CACHE_AGE = 2 * 1000;
	
	public ExternalWorkloadManagerConnector(Logger l) {
		this.LOGGER = l;
	}
	
	/**
	 * is called internally to ensure that submitted tasks are getting "visible to the external executor system"
	 * after a few cycles the id must be visible in the CACHED_JOB_STATUS or an exception will be thrown
	 * @throws TryLaterException 
	 */
	private void increaseSubmitCounterAndCheckIfTaskIsVisible() throws TryLaterException {
		for(Iterator<String> it = this.SUBMITTED_JOB_STATUS.keySet().iterator(); it.hasNext(); ) {
			String key = it.next();
			int v = this.SUBMITTED_JOB_STATUS.get(key);
			
			if(this.CACHED_JOB_STATUS.containsKey(key)) {
				it.remove();
			}
			else {
				// ensure that count is not tooooo high
				if(v > this.getMaxWaitCyclesUntilJobsIsVisible()) {
					throw new TryLaterException("ID '"+key+"' could not be queried after a wait time of " + v + " cycles.");
				}
			
				// update v
				v = v + 1;
				this.SUBMITTED_JOB_STATUS.put(key, v);
			}
		}
	}
	
	/**
	 * must return the maximum number of wait cycles until a job must be visible in the executor system
	 * @return
	 */
	public abstract int getMaxWaitCyclesUntilJobsIsVisible();

	/**
	 * submits a task to the grid system
	 * @param jobtask
	 * @return grid id if it was submitted successfully; null if not
	 * @throws DrmaaException 
	 */
	public abstract String submitJob(Task task, A executor) throws DrmaaException;
	
	/**
	 * releases a job on the grid system
	 * @param id
	 */
	public abstract void releaseJob(String id) throws DrmaaException;
	
	/**
	 * sets a job on hold on the grid system
	 * @param id
	 */
	public abstract void holdJob(String id) throws DrmaaException;
	
	/**
	 * cancels a job on the grid system that is scheduled or running
	 * @param id
	 */
	public abstract void cancelJob(String id) throws DrmaaException;
	
	/**
	 * tests, if a job is currently in RUNNING state on the grid system
	 * @param id
	 * @return
	 * @throws DrmaaException 
	 */
	public abstract boolean isJobRunning(String id) throws DrmaaException;
	
	/**
	 * retuns the jobinfo for a finished job
	 * @param id
	 * @return
	 * @throws DrmaaException 
	 */
	public abstract JobInfo getJobInfo(String id) throws DrmaaException;
	
	/**
	 * inits the connection to the grid system
	 * should terminate the program if connection can not be established
	 */
	public abstract void init();
	
	/**
	 * ends the connection to grid system
	 * and performs clean up if required
	 * @param ids 
	 * @param isInDetachMode
	 */
	public abstract void clean(HashMap<String, A> ids, boolean isInDetachMode);

	/**
	 * returns the host name of the execution node
	 * @param id
	 * @param watchdogBaseDir
	 * @return
	 */
	public abstract String getNameOfExecutionNode(String id, String watchdogBaseDir);

	/**
	 * true, if job is in queue and not RUNNING
	 * @param id
	 * @return
	 */
	public abstract boolean isJobKnownInGridSystem(String id);

	/**
	 * type of the executor 
	 * @return
	 */
	public abstract String getExecutorType();

	/**
	 * wait time between monitor checks
	 * the shorter the time, the more load is put on the grid system
	 * 1000 (ms) might be a good choice
	 * @return
	 */
	public long getDefaultWaitTime() {
		return DEFAULT_WAIT_TIME;
	}
	
	/**
	 * should return true, when init() was called but clean() was not
	 * @return
	 */
	public abstract boolean isInitComplete();
	
	/**
	 * should be overwritten in order to update the running job status
	 */
	protected abstract void updateJobStatusCache();
	
	/**
	 * is called periodically to ensure that the cache is updated regulary
	 * @throws TryLaterException 
	 */
	public synchronized boolean ensureThatJobStatusIsUpToDate(boolean enforceUpdate) throws TryLaterException {
		if(enforceUpdate || this.mustCacheBeUpdated()) {
			this.CACHED_JOB_STATUS.clear();
			this.updateJobStatusCache();
			this.increaseSubmitCounterAndCheckIfTaskIsVisible();
			lastCacheUpdate = System.currentTimeMillis();
			return true;
		}
		return false;
	}
		
	/**
	 * true, if the cache should be updated now
	 * @return
	 */
	protected boolean mustCacheBeUpdated() {
		return System.currentTimeMillis()-lastCacheUpdate > this.getMaxCacheAgeInMs();
	}
	
	/**
	 * maximal allowed age of the cache in milli seconds
	 * @return
	 */
	protected int getMaxCacheAgeInMs() {
		return BinaryCallBasedExternalWorkflowManagerConnector.MAX_CACHE_AGE;
	}

	/**
	 * removes a task from being monitored
	 * @param id
	 */
	protected void remove(String id) {
		this.CACHED_JOB_STATUS.remove(id);
		this.SUBMITTED_JOB_STATUS.remove(id);
	}

	/**
	 * some jobs might take a few seconds until the are visible in the grids system
	 * @param id
	 * @return
	 */
	protected boolean isInInitialSubmissionState(String id) {
		return this.SUBMITTED_JOB_STATUS.containsKey(id);
	}
}