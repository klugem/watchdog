package de.lmu.ifi.bio.watchdog.slave.clientEvents;


import de.lmu.ifi.bio.network.EventSocket;
import de.lmu.ifi.bio.network.event.Event;
import de.lmu.ifi.bio.network.eventhandler.Eventhandler;
import de.lmu.ifi.bio.network.exception.ConnectionNotReady;
import de.lmu.ifi.bio.watchdog.executor.WatchdogThread;
import de.lmu.ifi.bio.watchdog.executor.remote.RemoteJobInfo;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.slave.SlaveStatusHandler;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.task.TaskActionTime;
import de.lmu.ifi.bio.watchdog.task.TaskStatus;
import de.lmu.ifi.bio.watchdog.task.TaskStore;

/**
 * Event handler for a new task event
 * @author Michael Kluge
 *
 */
public class TaskExecutorEventEH extends Eventhandler {
	
	private final WatchdogThread RUNNER;
	private final Logger LOGGER = new Logger(LogLevel.DEBUG);

	/**
	 * Constructor
	 * @param runner
	 */
	public TaskExecutorEventEH(WatchdogThread runner) {
		this.RUNNER = runner;
	}

	@Override
	public Class<?> getType() {
		return TaskExecutorEvent.class;
	}

	@Override
	public boolean handleEvent(Event e, EventSocket reciever) throws ConnectionNotReady {
		if(reciever.isServer())
			return false;
		if(!(e instanceof TaskExecutorEvent))
			return false;
		
		TaskExecutorEvent event = (TaskExecutorEvent) e;
		Task task = event.getTask();
		LOGGER.debug("Recieved new task with id '" + task.getID() + "' from master.");
		
		// mark the task to run on a slave
		task.setIsScheduledOnSlave(false);
		task.setIsRunningOnSlave(true);
		task.performAction(TaskActionTime.BEFORE);	
		
		// add the status update handler
		task.addStatusHandler(SlaveStatusHandler.handler);
		
		// register the task
		TaskStore.taskPut(task.getID(), task);
					
		// reset some stuff
		task.setStatus(TaskStatus.WAITING_DEPENDENCIES);
		task.getExecutor().setClone();
		task.decreaseExecutionCounter();
		
		// add task actions that should be performed on terminate of the slaves
		this.RUNNER.addTerminateCommands(task.getOnKillSlaveActions());
		
		// all actions were performed well
		if(!task.hasErrors()) {
			// add that task to the que
			this.RUNNER.addToQue(event.getTask());
		}
		else {
			// call checker instantly
			task.setJobInfo(new RemoteJobInfo(-1, true, true));
		}
		return true;
	}
}
