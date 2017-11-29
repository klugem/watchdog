package de.lmu.ifi.bio.watchdog.interfaces;

import de.lmu.ifi.bio.watchdog.task.Task;

public abstract class SuccessChecker {

	public abstract boolean hasTaskSucceeded();
	public final Task T;
	
	/**
	 * Constructor
	 * @param t
	 */
	public SuccessChecker(Task t) {
		this.T = t;
	}
}
