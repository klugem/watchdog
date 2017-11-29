package de.lmu.ifi.bio.watchdog.slave.clientEvents;

import de.lmu.ifi.bio.network.event.Event;

public class TerminateTaskEvent extends Event {

	private final String ID;
	private static final long serialVersionUID = -116394650525279430L;
	
	public TerminateTaskEvent(String id) {
		this.ID = id;
	}
	
	public String getTaskID() {
		return this.ID;
	}
}
