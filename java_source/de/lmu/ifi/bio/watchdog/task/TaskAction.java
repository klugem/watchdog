package de.lmu.ifi.bio.watchdog.task;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;

import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;

/**
 * An action that is performed before or after after and task or onSuccess or onFailure
 * @author kluge
 *
 */

public abstract class TaskAction implements Serializable, XMLDataStore {
	
	private static final long serialVersionUID = 7557518524537639261L;
	private final TaskActionTime TIME;
	private final boolean UNCOUPLE_FROM_EXECUTOR;
	private final ArrayList<String> ERRORS = new ArrayList<>();
	protected static final String NEWLINE = System.lineSeparator();
	public static final String AERROR = "Action reported an error:";
	private boolean wasExecuted = false;
	
	/**
	 * Constructor
	 * @param time
	 */
	public TaskAction(TaskActionTime time, boolean uncoupleFromExecutor) {
		this.TIME = time;
		this.UNCOUPLE_FROM_EXECUTOR = uncoupleFromExecutor;
	}

	/**
	 * performs the action of this task
	 * @return
	 */
	protected boolean performAction() {
		this.wasExecuted = true;
		return false;
	}
	
	/**
	 * return the action time for this event
	 * @return
	 */
	public TaskActionTime getActionTime() {
		return this.TIME;
	}
	
	/**
	 * true, if action was performed without problems.
	 * @return
	 */
	public boolean wasSuccessfull() {
		return this.ERRORS.size() == 0;
	}
	
	/**
	 * returns the errors, if some are there
	 * @return
	 */
	public ArrayList<String> getErrors() {
		return this.ERRORS;
	}
	
	/**
	 * adds an error
	 * @param error
	 * @return
	 */
	public void addError(String error) {
		this.ERRORS.add(AERROR + NEWLINE + this.toString() + NEWLINE + error);
	}
	
	@Override
	public String getName() {
		return this.getClass().getSimpleName().replace("TaskAction", "").toLowerCase();
	}
	
	/**
	 * returns a target path for diplay on the GUI
	 * @return
	 */
	public abstract String getTarget();
	
	@Override
	public String toString() {
		// ChoiseBoX mode
		if(this.TIME == null)
			return this.getName();
		else 
			return "action: " + this.getClass().getSimpleName() + ", time: " + this.TIME.toString();
	}
	
	/**
	 * tests, if the action should be performed on watchdog itself and not on the specific executor
	 * @return
	 */
	public boolean isUncoupledFromExecutor() {
		return this.UNCOUPLE_FROM_EXECUTOR;
	}
	
	public boolean wasExecuted() {
		return this.wasExecuted;
	}
}
