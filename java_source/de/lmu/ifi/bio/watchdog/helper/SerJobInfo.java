package de.lmu.ifi.bio.watchdog.helper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobInfo;

public class SerJobInfo implements JobInfo, Serializable {

	private static final long serialVersionUID = -2021290862613234149L;
	private final String ID;
	private final HashMap<Object, Object> RES = new HashMap<Object, Object>();
	private final boolean HAS_EXITED;
	private final int EXIT_STATUS;
	private final boolean HAS_SIGNALED;
	private final String TERMINATION_SIGNAL;
	private final boolean HAS_CORE_DUMP;
	private final boolean WAS_ABORTED;
	
	@SuppressWarnings("unchecked")
	public SerJobInfo(JobInfo info) throws DrmaaException {
		this.ID = info.getJobId();
		this.RES.putAll(info.getResourceUsage());
		this.HAS_EXITED = info.hasExited();
		this.EXIT_STATUS = info.getExitStatus();
		this.HAS_SIGNALED = info.hasSignaled();
		this.TERMINATION_SIGNAL = info.getTerminatingSignal();
		this.HAS_CORE_DUMP = info.hasCoreDump();
		this.WAS_ABORTED = info.wasAborted();
	}

    public String getJobId() throws DrmaaException {
    	return this.ID;
    }
    
    public Map<Object, Object> getResourceUsage() throws DrmaaException {
    	return this.RES;
    }
   
    public boolean hasExited() throws DrmaaException {
    	return this.HAS_EXITED;
    }
   
    public int getExitStatus() throws DrmaaException {
    	return this.EXIT_STATUS;
    }
    
    public boolean hasSignaled() throws DrmaaException {
    	return this.HAS_SIGNALED;
    }
    public String getTerminatingSignal() throws DrmaaException {
    	return this.TERMINATION_SIGNAL;
    }
    
    public boolean hasCoreDump() throws DrmaaException {
    	return this.HAS_CORE_DUMP;
    }

    public boolean wasAborted() throws DrmaaException {
    	return this.WAS_ABORTED;
    }
}
