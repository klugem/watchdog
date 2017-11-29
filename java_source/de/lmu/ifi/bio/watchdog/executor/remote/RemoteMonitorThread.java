package de.lmu.ifi.bio.watchdog.executor.remote;

import java.util.HashMap;
import java.util.HashSet;

import de.lmu.ifi.bio.watchdog.executor.ScheduledMonitorThread;
import de.lmu.ifi.bio.watchdog.helper.DevNullWriter;
import de.lmu.ifi.bio.watchdog.task.Task;

/**
 * Remote monitor thread which monitors the ssh sessions
 * @author Michael Kluge
 *
 */
public class RemoteMonitorThread extends ScheduledMonitorThread<RemoteExecutor> {

	private static final long SLEEP_MILIS = 1000;
	private static String TYPE = "Remote";
	private final DevNullWriter DEV_NULL = new DevNullWriter();
	

	public RemoteMonitorThread() {
		super("RemoteExecutorMonitorThread");
	}
	
	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	public void beforeLoop() {

	}
	
	@Override
	public long getDefaultWaitTime() {
		return SLEEP_MILIS;
	}
		
	@Override
	protected synchronized boolean monitorJobs() {
		super.monitorJobs();
		
		HashSet<Task> typeOfTasks = new HashSet<>();
		// find out what kind of tasks we have here
		HashMap<Integer, RemoteExecutor> monitor = this.getMonitorTasks();
		for(int id : monitor.keySet()) {
			RemoteExecutor e = monitor.get(id);
			typeOfTasks.add(e.getTask());
			
			if(e.isEOFReached()) {
				e.signalEndOfProcessing();
				this.remove(id);
			}
			else {
				// try to read from the stdin of the task
				if(e.getInputStream() != null)
					this.readStream(e.getInputStream(), this.DEV_NULL, e.isEOFReached());
			}
		}
		// try to run a task of this type
		for(Task t : typeOfTasks) {
			try2RunCommands(t);
		}
		return true;
	}
}