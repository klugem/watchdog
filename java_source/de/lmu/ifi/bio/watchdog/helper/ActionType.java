package de.lmu.ifi.bio.watchdog.helper;

/**
 * Action types for tasks
 * @author Michael Kluge
 *
 */
public enum ActionType {
	ENABLED("enabled"), SUBTASK("subtask"), DISABLED("disabled"), PERFORMED("performed");
	
	private final String NAME;
	
	/**
	 * Constructor
	 * @param name
	 */
	private ActionType(String name) {
		this.NAME = name;
	}
	
	/**
	 * true, if action should be triggered for each sub task
	 * @return
	 */
	public boolean isSubtaskEnabled() {
		return ActionType.SUBTASK.equals(this);
	}
	
	/**
	 * true, if action should be triggered once all tasks are finished
	 * @return
	 */
	public boolean isEnabled() {
		return ActionType.ENABLED.equals(this);
	}
	
	/**
	 * true, if no action should be triggered in any case
	 * @return
	 */
	public boolean isDisabled() {
		return ActionType.DISABLED.equals(this);
	}
	
	/**
	 * true, if the action was performed once
	 * @return
	 */
	public boolean wasPerformed() {
		return ActionType.PERFORMED.equals(this);
	}
	
	public int getIndex4ToogleGroup() {
		if(this.isEnabled())
			return 0;
		else if (this.isSubtaskEnabled())
			return 1;
		else
			return 2;
	}
	
	@Override
	public String toString() {
		return this.NAME;
	}
	
	/**
	 * returns a enum object based on the value in the string or null if it does not match to any of the allowed onces
	 * @param value
	 * @return
	 */
	public static ActionType getType(String value) {
		if(ActionType.ENABLED.toString().equals(value))
			return ActionType.ENABLED;
		else if(ActionType.SUBTASK.toString().equals(value))
			return ActionType.SUBTASK;
		else if(ActionType.DISABLED.toString().equals(value))
			return ActionType.DISABLED;
		else 
			return null;
	}
}
