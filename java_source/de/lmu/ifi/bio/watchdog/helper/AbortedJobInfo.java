package de.lmu.ifi.bio.watchdog.helper;

import java.util.HashMap;
import java.util.Map;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobInfo;

/**
 * Implements the JobInfo for a local task
 * Does only return very rudimentary info
 * @author Michael Kluge
 *
 */
public class AbortedJobInfo implements JobInfo {
	
	/**
	 * Constructor
	 */
	public AbortedJobInfo() {
	}

	@Override
	public String getJobId() throws DrmaaException {
		return null;
	}

	@Override
	public Map<String, String> getResourceUsage() throws DrmaaException {
		return new HashMap<String, String>();
	}

	@Override
	public boolean hasExited() throws DrmaaException {
		return true;
	}

	@Override
	public int getExitStatus() throws DrmaaException {
		return -1;
	}

	@Override
	public boolean hasSignaled() throws DrmaaException {
		return false;
	}

	@Override
	public String getTerminatingSignal() throws DrmaaException {
		return null;
	}

	@Override
	public boolean hasCoreDump() throws DrmaaException {
		return false;
	}

	@Override
	public boolean wasAborted() throws DrmaaException {
		return true;
	}
}
