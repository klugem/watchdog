package de.lmu.ifi.bio.watchdog.helper;

/**
 * Helper class for actions which can be performed using the web interface
 * @author Michael Kluge
 *
 */
public enum ControlAction {
	RELEASE("release checkpoint", "was released out of this checkpoint state"), RESTART("restart task", "was added to the scheduler again"), MODIFY("modify parameters", "use modified parameters"), DISPLAY("display parameters", "parameters were displayed"), LIST("show information", "show information"), TERMINATE("terminate all", "terminate watchdog and all running tasks"), IGNORE("ignore task", "ignore"), TERMINATE_TASK("terminate task", "terminate task"), RELEASE_RESOURCE_RESTRICTIONS("release resource restrictions", "resource restrictions were released"), USERINTERFACE_ACTION("GO!", "action from user interface"), RESOLVE("mark as resolved", "was marked as resolved"), RESOLVE_RETURN_USERINTERFACE("mark as resolved", "was marked as resolved"), DETACH("detach Watchdog", "detach request was sent");

	private final String ACTION_NAME;
	private final String DESCRIPTION;
	
	/**
	 * Constructor
	 * @param actionName
	 * @param description
	 */
	private ControlAction(String actionName, String description) {
		this.ACTION_NAME = actionName;
		this.DESCRIPTION = description;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getActionName() {
		return this.ACTION_NAME;
	}
	
	/**
	 * true, if a release action
	 * @return
	 */
	public boolean isReleaseAction() {
		return ControlAction.RELEASE.name().equals(this.name());
	}
	
	/**
	 * true, if a restart action
	 * @return
	 */
	public boolean isRestartAction() {
		return ControlAction.RESTART.name().equals(this.name());
	}
	
	/**
	 * true, if a parameter display action
	 * @return
	 */
	public boolean isDisplayAction() {
		return ControlAction.DISPLAY.name().equals(this.name());
	}
	
	/**
	 * true, if a modify action
	 * @return
	 */
	public boolean isModifyAction() {
		return ControlAction.MODIFY.name().equals(this.name());
	}
	
	
	/**
	 * true, if a list action
	 * @return
	 */
	public boolean isListAction() {
		return ControlAction.LIST.name().equals(this.name());
	}
	
	/**
	 * true, if a terminate action
	 * @return
	 */
	public boolean isTerminateAction() {
		return ControlAction.TERMINATE.name().equals(this.name());
	}
	
	/**
	 * true, if a detach action
	 * @return
	 */
	public boolean isDetachAction() {
		return ControlAction.DETACH.name().equals(this.name());
	}
	
	/**
	 * true, if a resolve action
	 * @return
	 */
	public boolean isResolveAction() {
		return ControlAction.RESOLVE.name().equals(this.name());
	}
	
	/**
	 * true, if a ignore action
	 * @return
	 */
	public boolean isIgnoreAction() {
		return ControlAction.IGNORE.name().equals(this.name());
	}
	
	public boolean isUserinteraceAction() {
		return ControlAction.USERINTERFACE_ACTION.name().equals(this.name());
	}
	
	public boolean isResolveParameterEnterAction() {
		return ControlAction.RESOLVE_RETURN_USERINTERFACE.name().equals((this.name()));
	}
	
	/**
	 * a description of that ControlAction
	 * @return
	 */
	public String getDescription() {
		return this.DESCRIPTION;
	}
	
	/**
	 * true, if the action requires neither a task id nor a xml id
	 * @return
	 */
	public boolean requiresNoID() {
		if(this.isListAction() || this.isTerminateAction() || this.isDetachAction())
			return true;
		
		return false;
	}
	
	/**
	 * returns a enum object based on the value in the string or null if it does not match to any of the allowed onces
	 * @param value
	 * @return
	 */
	public static ControlAction getType(String value) {
		if(ControlAction.RELEASE.name().equals(value))
			return ControlAction.RELEASE;
		else if(ControlAction.RESTART.name().equals(value))
			return ControlAction.RESTART;
		else if(ControlAction.MODIFY.name().equals(value))
			return ControlAction.MODIFY;
		else if(ControlAction.DISPLAY.name().equals(value))
			return ControlAction.DISPLAY;
		else if(ControlAction.LIST.name().equals(value))
			return ControlAction.LIST;
		else if(ControlAction.TERMINATE.name().equals(value))
			return ControlAction.TERMINATE;
		else if(ControlAction.IGNORE.name().equals(value))
			return ControlAction.IGNORE;
		else if(ControlAction.TERMINATE_TASK.name().equals(value))
			return ControlAction.TERMINATE_TASK;
		else if(ControlAction.RELEASE_RESOURCE_RESTRICTIONS.name().equals(value))
			return ControlAction.RELEASE_RESOURCE_RESTRICTIONS;
		else if(ControlAction.USERINTERFACE_ACTION.name().equals(value))
			return ControlAction.USERINTERFACE_ACTION;
		else if(ControlAction.RESOLVE.name().equals(value))
			return ControlAction.RESOLVE;
		else if(ControlAction.DETACH.name().equals(value))
			return ControlAction.DETACH;
		else
			return null;
	}
}
