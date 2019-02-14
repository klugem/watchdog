package de.lmu.ifi.bio.watchdog.resume;

import java.util.Map;

import org.ggf.drmaa.JobInfo;

public class ResumeJobInfo implements JobInfo {

	public ResumeJobInfo(){}

    public String getJobId() {
    	return "-1";
    }
    
    public Map<Object, Object> getResourceUsage() {
    	return null;
    }
   
    public boolean hasExited() {
    	return true;
    }
   
    public int getExitStatus() {
    	return 0;
    }
    
    public boolean hasSignaled() {
    	return false;
    }
    public String getTerminatingSignal() {
    	return null;
    }
    
    public boolean hasCoreDump() {
    	return false;
    }

    public boolean wasAborted() {
    	return false;
    }
}
