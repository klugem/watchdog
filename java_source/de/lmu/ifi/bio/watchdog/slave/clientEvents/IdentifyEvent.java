package de.lmu.ifi.bio.watchdog.slave.clientEvents;

import de.lmu.ifi.bio.network.event.Event;

/**
 * Event that is sent when the slave connects to the server in order to sumit the ID of the slave
 * @author Michael Kluge
 *
 */
public class IdentifyEvent extends Event {

	private static final long serialVersionUID = 7692860449191772799L;
	private final String ID;
	
	/**
	 * Constructor
	 * @param id
	 */
	public IdentifyEvent(String id) {
		this.ID = id;
	}
	
	public String getID() {
		return this.ID;
	}
}