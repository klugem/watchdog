package de.lmu.ifi.bio.watchdog.task;


/**
 * Internal status a task can have.
 * @author Michael Kluge
 *
 */
public enum TaskStatus {
	WAITING_QUEUE("is waiting on free execution host"), WAITING_RESTRICTIONS("is waiting for resource restrictions"), WAITING_DEPENDENCIES("is waiting on dependencies to be finished"), FAILED_SYNTAX("could not be executed on the executor"), BEFORE_ACTION_FAILED("before action failed"), AFTER_ACTION_FAILED("after action failed"), 
	RUNNING("is currently running"), FAILED("has failed"), FAILED_ERROR_CHECK("error checker found some errors"), FAILED_SUCCESS_CHECK("sucess checker has failed"), FINISHED("has finished"), FINISHED_ON_SLAVE("has finished on slave"), KILLED("was killed"), IGNORE("is ignored"), STATUS_CHECK("waiting for status check to finish"), TERMINATED("was terminated"), RESOLVED("was marked as resolved manually");
	
	private final String MESSAGE;
	
	/**
	 * Private constructor
	 * @param message
	 */
	private TaskStatus(String message) {
		this.MESSAGE = message;
	}
	
	@Override
	public String toString() {
		return this.MESSAGE;
	}
	
	public boolean isWaitingOnDependencies() {
		return this.name().equals(TaskStatus.WAITING_DEPENDENCIES.name());
	}
	
	public boolean isWaitingOnQueue() {
		return this.name().equals(TaskStatus.WAITING_QUEUE.name());
	}

	public boolean isStatusCheck() {
		return this.name().equals(TaskStatus.STATUS_CHECK.name());
	}
	
	public boolean isTerminated() {
		return this.name().equals(TaskStatus.TERMINATED.name());
	}
	
	public boolean isIgnored() {
		return this.name().equals(TaskStatus.IGNORE.name());
	}
	
	public boolean isResolved() {
		return this.name().equals(TaskStatus.RESOLVED.name());
	}
	
	
	//********************* for GUI visu *********************
	public boolean isGUIWaitingQueue() {
		return this.name().equals(TaskStatus.WAITING_QUEUE.name());
	}
	public boolean isGUIWaitingDependencies() {
		return this.name().equals(TaskStatus.WAITING_DEPENDENCIES.name());
	}
	public boolean isGUIWaitingRestrictions() {
		return this.name().equals(TaskStatus.WAITING_RESTRICTIONS.name());
	}
	public boolean isGUIFinished() {
		return this.name().equals(TaskStatus.FINISHED.name());
	}
	public boolean isGUIRunning() {
		return this.name().equals(TaskStatus.RUNNING.name());
	}
	public boolean isGUIFailed() {
		return this.name().equals(TaskStatus.FAILED.name()) || this.name().equals(TaskStatus.FAILED_ERROR_CHECK.name()) || this.name().equals(TaskStatus.FAILED_SUCCESS_CHECK.name())  || this.name().equals(TaskStatus.FAILED_SYNTAX.name()) || this.name().equals(TaskStatus.AFTER_ACTION_FAILED.name()) || this.name().equals(TaskStatus.BEFORE_ACTION_FAILED.name());
	}
}
