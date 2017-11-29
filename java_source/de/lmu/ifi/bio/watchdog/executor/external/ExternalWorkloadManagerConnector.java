package de.lmu.ifi.bio.watchdog.executor.external;

import java.util.HashSet;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobInfo;

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
	
	public ExternalWorkloadManagerConnector(Logger l) {
		this.LOGGER = l;
	}

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
	 */
	public abstract void clean(HashSet<String> ids);

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
}