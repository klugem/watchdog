package de.lmu.ifi.bio.watchdog.slave.clientEvents;

import de.lmu.ifi.bio.network.event.Event;
import de.lmu.ifi.bio.watchdog.task.TaskStatus;

/**
 * Event that is sent when the status of a task is changed.
 * @author Michael Kluge
 *
 */
public class StatusUpdateEvent extends Event {
	private static final long serialVersionUID = 5492824218671183450L;
	private final String TASK_ID;
	private final String HOST;
	private final TaskStatus STATUS;
	
	/**
	 * Constructor
	 * @param taskID
	 * @param status
	 */
	public StatusUpdateEvent(String taskID, TaskStatus status, String hostname) {
		this.TASK_ID = taskID;
		this.STATUS = status;
		this.HOST = hostname;
	}
	
	public String getTaskID() {
		return this.TASK_ID;
	}
	
	public TaskStatus getTaskStatus() {
		return this.STATUS;
	}
	
	public String getHostname() {
		return this.HOST;
	}
}
