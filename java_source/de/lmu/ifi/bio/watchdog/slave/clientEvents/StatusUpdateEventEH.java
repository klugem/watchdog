package de.lmu.ifi.bio.watchdog.slave.clientEvents;

import de.lmu.ifi.bio.network.EventSocket;
import de.lmu.ifi.bio.network.event.Event;
import de.lmu.ifi.bio.network.eventhandler.Eventhandler;
import de.lmu.ifi.bio.network.exception.ConnectionNotReady;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.task.TaskStatus;

/**
 * Event handler for task status update events
 * @author Michael Kluge
 *
 */
public class StatusUpdateEventEH extends Eventhandler {

	@Override
	public Class<?> getType() {
		return StatusUpdateEvent.class;
	}

	@Override
	public boolean handleEvent(Event e, EventSocket reciever) throws ConnectionNotReady {
		if(reciever.isClient())
			return false;
		if(!(e instanceof StatusUpdateEvent))
			return false;
		
		StatusUpdateEvent event = (StatusUpdateEvent) e;
		TaskStatus status = event.getTaskStatus();
		// do not update this 
		if(status.isWaitingOnDependencies())
			return true;
		
		// do not set the finished status until the master confirmed that (or depending tasks might not be scheduled)
		if(TaskStatus.FINISHED.equals(status))
			status = TaskStatus.FINISHED_ON_SLAVE;
		
		// try to get the task that is meant
		Task t = Task.getTask(event.getTaskID());
		if(t == null)
			System.out.println("[ERROR] Task with ID '"+event.getTaskID()+"' does not exist!");
		// update the task status on the master
		else {
			t.setStatus(status);
			if(t.getHost() == null && event.getHostname() != null)
				t.setHostname(event.getHostname());
		}
		
		return true;
	}

}