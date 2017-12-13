package de.lmu.ifi.bio.watchdog.slave.clientEvents;

import java.util.LinkedHashSet;

import de.lmu.ifi.bio.network.event.Event;
import de.lmu.ifi.bio.watchdog.task.Task;

public class TaskExecutorEvent extends Event {

	private final Task TASK;
	private final LinkedHashSet<Integer> DEPS;
	private static final long serialVersionUID = 4317430108994879399L;

	public TaskExecutorEvent(Task t, LinkedHashSet<Integer> depToKeep) {
		this.TASK = t;
		this.DEPS = depToKeep;
	}
	
	public Task getTask() {
		return this.TASK;
	}
	
	public LinkedHashSet<Integer> getDependencies() {
		return this.DEPS;
	}
}
