package de.lmu.ifi.bio.watchdog.slave.clientEvents;

import de.lmu.ifi.bio.network.event.Event;
import de.lmu.ifi.bio.watchdog.task.Task;

public class TaskExecutorEvent extends Event {

	private final Task TASK;
	private static final long serialVersionUID = 4317430108994879399L;

	public TaskExecutorEvent(Task t) {
		this.TASK = t;
	}
	
	public Task getTask() {
		return this.TASK;
	}

}
