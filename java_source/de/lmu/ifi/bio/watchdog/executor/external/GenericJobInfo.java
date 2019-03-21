package de.lmu.ifi.bio.watchdog.executor.external;

import java.util.HashMap;
import java.util.Map;

import org.ggf.drmaa.DrmaaException;

import de.lmu.ifi.bio.watchdog.executor.remote.RemoteJobInfo;

public class GenericJobInfo extends RemoteJobInfo {
	
	private final int SIGNAL;
	private final String ID;
	private final HashMap<String, String> RES;

	public GenericJobInfo(String id, int exitCode, int signal, boolean hasExited, HashMap<String, String> resources) {
		super(exitCode, hasExited, signal != 0);
		this.ID = id;
		this.SIGNAL = signal;
		this.RES = resources;
	}

	@Override
	public String getJobId() throws DrmaaException {
		return this.ID;
	}

	@Override
	public Map<String, String> getResourceUsage() throws DrmaaException {
		return this.RES;
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
