package de.lmu.ifi.bio.watchdog.executor.external.slurm;

import java.util.HashMap;
import java.util.Map;

import org.ggf.drmaa.DrmaaException;

import de.lmu.ifi.bio.watchdog.executor.remote.RemoteJobInfo;

public class SlurmJobInfo extends RemoteJobInfo {
	
	private final int SIGNAL;
	private final String ID;

	public SlurmJobInfo(String id, int exitCode, int signal, boolean hasExited) {
		super(exitCode, hasExited, signal != 0);
		this.ID = id;
		this.SIGNAL = signal;
	}

	@Override
	public String getJobId() throws DrmaaException {
		return this.ID;
	}

	@Override
	public Map<String, String> getResourceUsage() throws DrmaaException {
		return new HashMap<String, String>();
	}

	@Override
	public boolean hasSignaled() throws DrmaaException {
		return this.SIGNAL != 0;
	}

	@Override
	public String getTerminatingSignal() throws DrmaaException {
		return Integer.toString(this.SIGNAL);
	}
}
