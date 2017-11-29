package de.lmu.ifi.bio.watchdog.executor;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import de.lmu.ifi.bio.watchdog.task.Task;

/**
 * Monitors tasks, which are internally scheduled
 * @author Michael Kluge
 *
 */
public abstract class ScheduledMonitorThread<E extends ScheduledExecutor> extends MonitorThread<E> {
	
	protected static final int BUFFER_SIZE = 32768;
	protected final byte[] BUFFER = new byte[BUFFER_SIZE];
	protected final int MAX_INTERATIONS = 100;
	protected HashMap<Integer, Integer> nothingFound = new HashMap<>();
	
	public ScheduledMonitorThread(String name) {
		super(name);
	}
	
	/**
	 * Reads a stream and stores the content into a file
	 * @param stream
	 * @param out
	 * @param processFinished
	 */
	protected synchronized void readStream(InputStream stream, BufferedOutputStream out, boolean processFinished) {
		if(stream == null)
			return;
		
		// init nothing found counter
		int code = stream.hashCode();
		if(!this.nothingFound.containsKey(code)) {
			this.nothingFound.put(code, 0);
		}
		
		int bSize = BUFFER_SIZE*2;
		boolean readAll = false;
		
		// if two often nothing was read, lets also accept smaller buffer size
		if(this.nothingFound.get(code) >= 10*this.getDefaultWaitTime()) { // wait at longest 10 times the default wait time
			bSize = 1; // force to read anything
			this.nothingFound.put(code, 0); // reset counter
			readAll = true; // read the complete stream
		}

		int i = 0;
		int read = 0;
		 // we do not want to block the thread
		try {
			while(processFinished || stream.available() >= bSize) {
				 if((read = stream.read(BUFFER)) != -1) {
					 if(out != null) out.write(BUFFER, 0, read);
					 i++;
				 }
				 // end the while loop, when processFinished is true and nothing is left to read
				 if((processFinished && (read < bSize || stream.available() == 0)) || (processFinished == false && i >= MAX_INTERATIONS && !readAll)) 
					 break;
			}
			// flush if anything was written
			if(i > 0 && out != null)
				out.flush();
			else 
				this.nothingFound.put(code, this.nothingFound.get(code)+1);
			
			// flush the content and close the file
			if(processFinished && out != null) {
				out.flush();
				out.close();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			LOGGER.error("Problem during writing into stream.");
		}
	}
		
	/**
	 * tries to run new tasks of the same type
	 * @param t
	 * @return
	 */
	protected synchronized int try2RunCommands(Task t) {
		if(this.isSchedulingPaused())
			return 0;
		
		int run = 0;
		// check, if another job of the same type is on hold
		while((!t.isMaxRunningRestrictionReached() && !t.getExecutor().isMaxRunningRestrictionReached())) {
			Task onHold = Task.getJobsOnHold(t.getTaskID());
			if(onHold != null) {
				boolean found = false;

				// search the executor
				for(E ex : new ArrayList<E>(this.getMonitorTasks().values())) {
					if(ex.getTask().getID().equals(onHold.getID())) {
						onHold.setIsOnHold(false);
						
						// run the actual command
						ex.runCommand();
						found = true;
						run++;
						break;
					}
				}
				// check, if task on hold was found.
				if(!found) {
					LOGGER.error("Task '" + onHold.getID() + "' was not found within the tasks that the " + this.getType() + " monitor thread monitors!");
					System.exit(1);
				}
			}
			else
				break;
		}
		// check if some jobs are on hold that does not consume any resources
		for(E ex : new ArrayList<E>(this.getMonitorTasks().values())) {
			Task task = ex.getTask();
		
			if(!task.doesConsumeResources() && task.isTaskOnHold()) {
				run++;
				task.setIsOnHold(false);
				ex.runCommand();
			}
		}
		return run;
	}
}
