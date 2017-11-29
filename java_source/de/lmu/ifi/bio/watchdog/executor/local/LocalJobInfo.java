package de.lmu.ifi.bio.watchdog.executor.local;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.InvalidAttributeValueException;
import org.ggf.drmaa.JobInfo;

/**
 * Implements the JobInfo for a local task
 * Does only return very rudimentary info
 * @author Michael Kluge
 *
 */
public class LocalJobInfo implements JobInfo, Serializable {
	
	private static final long serialVersionUID = -140363548113324813L;
	private final Process P;
	
	/**
	 * Constructor
	 * @param p
	 */
	public LocalJobInfo(Process p) {
		this.P = p;
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
		return !this.P.isAlive();
	}

	@Override
	public int getExitStatus() throws DrmaaException {
		if(!this.hasExited())
			throw(new InvalidAttributeValueException("Process is not finished yet!"));
		return this.P.exitValue();
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
		return this.P == null;
	}
}
