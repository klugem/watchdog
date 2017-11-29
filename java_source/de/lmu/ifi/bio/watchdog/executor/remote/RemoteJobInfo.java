package de.lmu.ifi.bio.watchdog.executor.remote;

import java.util.HashMap;
import java.util.Map;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.InvalidAttributeValueException;
import org.ggf.drmaa.JobInfo;

/**
 * Implements the JobInfo for a remote task
 * Does only return very rudimentary info
 * @author Michael Kluge
 *
 */
public class RemoteJobInfo implements JobInfo {
	
	private final int EXIT_CODE;
	private final boolean HAS_EXITED;
	private final boolean WAS_ABORTED;
	
	/**
	 * Constructor
	 * @param exitCode
	 * @param hasExited
	 * @param wasAborted
	 */
	public RemoteJobInfo(int exitCode, boolean hasExited, boolean wasAborted) {
		this.EXIT_CODE = exitCode;
		this.HAS_EXITED = hasExited;
		this.WAS_ABORTED = wasAborted;
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
		return HAS_EXITED;
	}

	@Override
	public int getExitStatus() throws DrmaaException {
		if(!this.hasExited())
			throw(new InvalidAttributeValueException("Process is not finished yet!"));
		return this.EXIT_CODE;
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
		return this.WAS_ABORTED;
	}
}
