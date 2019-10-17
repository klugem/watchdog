package de.lmu.ifi.bio.watchdog.slave.clientEvents;

import de.lmu.ifi.bio.network.EventSocket;
import de.lmu.ifi.bio.network.event.Event;
import de.lmu.ifi.bio.network.eventhandler.Eventhandler;
import de.lmu.ifi.bio.network.exception.ConnectionNotReady;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.task.TaskStore;

/**
 * Event handler for a new task event
 * @author Michael Kluge
 *
 */
public class TaskFinishedEventEH extends Eventhandler {
	
	private final Logger LOGGER = new Logger(LogLevel.DEBUG);

	@Override
	public Class<?> getType() {
		return TaskFinishedEvent.class;
	}

	@Override
	public boolean handleEvent(Event e, EventSocket reciever) throws ConnectionNotReady {
		if(reciever.isClient())
			return false;
		if(!(e instanceof TaskFinishedEvent))
			return false;
		// update the task
		TaskFinishedEvent event = (TaskFinishedEvent) e;
		String id = event.getID();
		Task t = TaskStore.taskGet(id);
		
		if(t != null) {
			LOGGER.debug("Recieved finished task with id '" + t.getID() + "' from slave '"+event.getSlaveID()+"'.");
			// task is not scheduled any more
			t.setIsScheduledOnSlave(false);
			
			// delete the action tasks as it was running on a slave
			t.deleteActions();
			
			// set the slave ID
			t.setSlaveTaskID(event.getSlaveID());
			
			// set the errors the task handler detected from actions
			for(String err : event.getErrors())
				t.addError(err);
			
			// mark the files to be deleted on exit
			// set the values to get the error checker to check the stuff
			if(event.getErr() != null) {
				event.getErr().deleteOnExit();
				t.setStderr(event.getErr());
			}
			if(event.getOut() != null) { 
				event.getOut().deleteOnExit();
				t.setStdout(event.getOut());
			}
			
			// set the version query file
			if(event.getVersionQueryInfoFile() != null) {
				t.setVersionQueryInfoFile(event.getVersionQueryInfoFile());
			}

			// let the checker do his work!
			t.setJobInfo(event.getJobInfo());
		}
		else {
			LOGGER.error("Finished slave task with id '" + id + "' is not valid on master!");
		}
		return true;
	}

}
