package de.lmu.ifi.bio.watchdog.executor.local;

import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.util.HashMap;

import org.ggf.drmaa.JobInfo;

import de.lmu.ifi.bio.watchdog.executor.ScheduledMonitorThread;
import de.lmu.ifi.bio.watchdog.task.Task;

/**
 * Monitors locally running jobs in a separate thread and accounts for stderr, stdinn and stdout streams
 * @author Michael Kluge
 *
 */
public class LocalMonitorThread extends ScheduledMonitorThread<LocalExecutor> {
	
	private static final long SLEEP_MILIS = 25; 
	private static String TYPE = "Local";
	
	public LocalMonitorThread() {
		super("LocalExecutorMonitorThread");
	}
			
	/**
	 * Reads from a inputstream and writes to STDIN of a process
	 * @param stream
	 * @param in
	 * @param 
	 */
	private synchronized boolean writeStdinStream(OutputStream stream, BufferedInputStream in) {
		if(stream == null || in == null)
			return false;
		
		int i = 0;
		int read = 0;
		try {
			while((read = in.read(BUFFER)) != -1) {
				stream.write(BUFFER, 0, read);
				i++;

				// give other streams the chance to be processed
				if(i >= MAX_INTERATIONS)
					break;
			}
			
			// flush, if something was written
			if(i > 0) 
				stream.flush();
			
			// close the stream and mark the stuff as finished
			if(read == -1) 
				stream.close();
				return true;
		}
		catch(Exception e) {
			e.printStackTrace();
			LOGGER.error("Problem during writing into stdin.");
		}
		return false;
	}


	@Override
	protected synchronized boolean monitorJobs() {
		super.monitorJobs();
		int finished = 0;
		HashMap<String, LocalExecutor> monitor = this.getMonitorTasks();
		for(String id : monitor.keySet()) {
			LocalExecutor e = monitor.get(id);
			Task t = e.getTask();
			// check, if job was run and has exited
			if(t.getExecutionCounter() > 0 && !t.isTaskOnHold() && !t.hasJobInfo()) {
				try {
					if(e.isProcessStillRuning()) {
						// collect the stderr and stdout streams
						this.readStream(e.getStdoutStream(), e.getStdoutFile(), false);
						this.readStream(e.getStderrStream(), e.getStderrFile(), false);
									
						// write to stdin if needed
						if(!e.wasStdinCompletelyWritten()) {
							// set it to completely written, if it is the case!
							if(this.writeStdinStream(e.getStdinStream(), e.getStdinFile()))
								e.setStdinWasCompletelyWritten();
						}
					}
					// create the job info object
					else {
						// finish the stderr and stdout streams
						this.readStream(e.getStdoutStream(), e.getStdoutFile(), true);
						this.readStream(e.getStderrStream(), e.getStderrFile(), true);
						e.sync(); // write files physicall to disk!
						
						JobInfo info = e.getJobInfo();
						// destroy the process
						e.stopExecution();
						t.setJobInfo(info);
						this.remove(id);
						LOGGER.info("Task with internal ID " + t.getID() + " has been finished on the local system.");
						finished++;
					}
				} 
				catch(Exception ex) {
					ex.printStackTrace();
				}
			}
			// try to run a new jobs of this type
			this.try2RunCommands(t);
		}
		return finished > 0;
	}
	
	@Override
	public void beforeLoop() {

	}
	
	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	public long getDefaultWaitTime() {
		return SLEEP_MILIS;
	}
}
