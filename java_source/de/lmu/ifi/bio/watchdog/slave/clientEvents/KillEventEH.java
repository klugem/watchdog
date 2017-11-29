package de.lmu.ifi.bio.watchdog.slave.clientEvents;

import java.util.ArrayList;

import de.lmu.ifi.bio.network.EventSocket;
import de.lmu.ifi.bio.network.event.Event;
import de.lmu.ifi.bio.network.eventhandler.Eventhandler;
import de.lmu.ifi.bio.network.exception.ConnectionNotReady;
import de.lmu.ifi.bio.watchdog.executor.WatchdogThread;
import de.lmu.ifi.bio.watchdog.task.TaskAction;

/**
 * Termination event handler.
 * @author Michael Kluge
 *
 */
public class KillEventEH extends Eventhandler {

	private final WatchdogThread RUNNER;
	
	/**
	 * Constructor
	 * @param runner
	 */
	public KillEventEH(WatchdogThread runner) {
		this.RUNNER = runner;
	}
	
	@Override
	public Class<?> getType() {
		return KillEvent.class;
	}

	@Override
	public boolean handleEvent(Event e, EventSocket reciever) throws ConnectionNotReady {
		if(reciever.isServer())
			return false;
		if(!(e instanceof KillEvent))
			return false;
		
		ArrayList<TaskAction> shutdownActions = this.RUNNER.getShutdownEvents();
		
		System.out.println("[STATUS] Got kill event from server!");
		System.out.println("[STATUS] performing "+ shutdownActions.size() +" before termination task actions.");
		this.RUNNER.shutdown();
		System.out.println("[STATUS] shutting down...");
		reciever.setExpectedEndOfConnection();
		reciever.disconnect();
		System.exit(0);
		return true;
	}
}