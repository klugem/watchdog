package de.lmu.ifi.bio.watchdog.slave.clientEvents;

import de.lmu.ifi.bio.network.EventSocket;
import de.lmu.ifi.bio.network.event.Event;
import de.lmu.ifi.bio.network.eventhandler.Eventhandler;
import de.lmu.ifi.bio.network.exception.ConnectionNotReady;
import de.lmu.ifi.bio.watchdog.task.Task;

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
		
		// do not update this 
		if(event.getTaskStatus().isWaitingOnDependencies())
			return true;
		
		// try to get the task that is meant
		Task t = Task.getTask(event.getTaskID());
		if(t == null)
			System.out.println("[ERROR] Task with ID '"+event.getTaskID()+"' does not exist!");
		// update the task status on the master
		else {
			t.setStatus(event.getTaskStatus());
			if(t.getHost() == null && event.getHostname() != null)
				t.setHostname(event.getHostname());
		}
		
		return true;
	}

}