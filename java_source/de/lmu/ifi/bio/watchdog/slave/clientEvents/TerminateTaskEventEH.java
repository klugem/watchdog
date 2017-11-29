package de.lmu.ifi.bio.watchdog.slave.clientEvents;

import de.lmu.ifi.bio.network.EventSocket;
import de.lmu.ifi.bio.network.event.Event;
import de.lmu.ifi.bio.network.eventhandler.Eventhandler;
import de.lmu.ifi.bio.network.exception.ConnectionNotReady;
import de.lmu.ifi.bio.watchdog.slave.Master;
import de.lmu.ifi.bio.watchdog.slave.SlaveStatusHandler;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask;

/**
 * Event handler for a termination of a task
 * @author Michael Kluge
 *
 */
public class TerminateTaskEventEH extends Eventhandler {

	@Override
	public Class<?> getType() {
		return TerminateTaskEvent.class;
	}

	@Override
	public boolean handleEvent(Event e, EventSocket reciever) throws ConnectionNotReady {
		if(reciever.isServer())
			return false;
		if(!(e instanceof TerminateTaskEvent))
			return false;
		
		TerminateTaskEvent event = (TerminateTaskEvent) e;
		String id = event.getTaskID();
		Task task = Task.getTask(id);
		
		if(task != null) {
			task.removeStatusHandler(SlaveStatusHandler.class); // do not send any status updates back rum this task
			task.terminateTask();
			XMLTask.deleteSlaveID(task);
			Master.unregisterTask(task);
			return true;
		}
		else {
			System.out.println("[ERROR] Task with ID '"+id+"' can not be terminated because it is not running on this host!");
			return false;
		}
	}
}
