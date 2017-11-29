package de.lmu.ifi.bio.watchdog.executor;

import java.io.FileOutputStream;

import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.helper.SyncronizedLineWriter;
import de.lmu.ifi.bio.watchdog.task.Task;

/**
 * Executor with internal scheduling
 * @author Michael Kluge
 *
 */
public abstract class ScheduledExecutor extends Executor {

	protected FileOutputStream fos;
	protected FileOutputStream fer;
	
	/**
	 * Constructor
	 * @param task
	 * @param log
	 * @param execInfo
	 */
	public ScheduledExecutor(Task task, SyncronizedLineWriter log, ExecutorInfo execInfo) {
		super(task, log, execInfo);
	}
	
	/**
	 * runs the actual command
	 * @return 
	 */
	public void runCommand() {
		try {
			if(this.fos == null) this.fos = new FileOutputStream(Functions.generateRandomLogFile(false, false), false);
			if(this.fer == null) this.fer = new FileOutputStream(Functions.generateRandomLogFile(true, false), false);
		}
		catch(Exception e) { e.printStackTrace(); }
	}
	
	/**
	 * Ensure that log files are written physically to disk!
	 */
	public void sync() {
		if(this.fer != null)
			try { this.fer.getFD().sync(); } catch(Exception e) {}
		if(this.fos != null)
			try { this.fos.getFD().sync(); } catch(Exception e) {}
	}
}
