package de.lmu.ifi.bio.watchdog.task;

import de.lmu.ifi.bio.network.exception.ConnectionNotReady;

/**
 * Interface that handles task status updates
 * @author Michael Kluge
 *
 */
public interface StatusHandler {
	
	/**
	 * handles an update change of a task
	 * @param task
	 * @throws ConnectionNotReady 
	 */
	public void handle(Task task);
}