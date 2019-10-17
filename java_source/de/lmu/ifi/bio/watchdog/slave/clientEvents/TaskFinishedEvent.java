package de.lmu.ifi.bio.watchdog.slave.clientEvents;

import java.io.File;
import java.util.ArrayList;

import org.ggf.drmaa.JobInfo;

import de.lmu.ifi.bio.network.event.Event;
import de.lmu.ifi.bio.watchdog.helper.TransferFile;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.task.TaskAction;

public class TaskFinishedEvent extends Event {

	private static final long serialVersionUID = 2979413423190925919L;
	private final String ID;
	private final String SLAVE_ID;
	private final JobInfo INFO;
	private final File ERR;
	private final File OUT;
	private final File VQ;
	private final ArrayList<String> ERRORS = new ArrayList<>();

	public TaskFinishedEvent(Task t, String id, String slaveID, JobInfo info, File err, File out, ArrayList<String> errors, File versionQuery) {
		this.ID = id;
		this.SLAVE_ID = slaveID;
		this.INFO = info;
		
		// copy events that come from actions
		for(String e : errors) {
			if(e.startsWith(TaskAction.AERROR)) {
				this.ERRORS.add(e);
			}
		}
		
		File od = t.getStdOut(false);
		File ed = t.getStdErr(false);
					
		// save the log file where they belong to!
		try {
			// delete the files if it should be not appended
			if(od != null && !t.isOutputAppended())
				od.delete();
			if(ed != null && !t.isErrorAppended())
				ed.delete();
			
			// fill in the new content
			if(od != null && out!= null)
				TransferFile.copyFileToBase(out, od, t.isOutputAppended());
			if(ed != null && err != null)
				TransferFile.copyFileToBase(err, ed, t.isErrorAppended());
		}
		catch(Exception ex) {
			this.ERRORS.add("[ERROR] Failed to copy the stdout and stderr files to the correct location.");
			ex.printStackTrace();
		}
		// set to the new files that are now stored on the correct system
		this.ERR = out;
		this.OUT = err;
		this.VQ = t.getVersionQueryInfoFile();
	}
	
	public String getID() {
		return this.ID;
	}
	
	public String getSlaveID() {
		return this.SLAVE_ID;
	}
	
	public File getErr() {
		return this.ERR;
	}
	
	public File getOut() {
		return this.OUT;
	}
	
	public File getVersionQueryInfoFile() {
		return this.VQ;
	}
	
	public JobInfo getJobInfo() {
		return this.INFO;
	}
	
	public ArrayList<String> getErrors() {
		return this.ERRORS;
	}
}
