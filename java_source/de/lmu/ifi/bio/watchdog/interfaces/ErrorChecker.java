package de.lmu.ifi.bio.watchdog.interfaces;

import java.io.Serializable;
import java.util.ArrayList;

import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.task.Task;

public abstract class ErrorChecker implements Serializable {

	private static final long serialVersionUID = -1644751162931143032L;
	protected final ArrayList<String> ERRORS = new ArrayList<>(); 	
	protected final Logger LOGGER = new Logger();
	private boolean wasCheckPerformed = false;
	public final Task T;
	
	/**
	 * Constructor
	 * @param t
	 */
	public ErrorChecker(Task t) {
		this.T = t;
	}

	public abstract boolean hasTaskFailed();
	
	/**
	 * returns the error messages, collected by the error checker
	 * @return
	 */
	public ArrayList<String> getErrorMessages() {
		return new ArrayList<>(this.ERRORS);
	}
	
	/**
	 * true, if check, was performed
	 * @return
	 */
	public boolean wasCheckPerformed() {
		return this.wasCheckPerformed;
	}
	
	/**
	 * can be use to signal that checking was performed
	 */
	public void checkWasPerformed() {
		this.wasCheckPerformed = true;
	}
	
	public void reset() {
		this.wasCheckPerformed = false;
		this.ERRORS.clear();
	}
}
