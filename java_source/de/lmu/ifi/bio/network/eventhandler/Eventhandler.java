package de.lmu.ifi.bio.network.eventhandler;

import de.lmu.ifi.bio.network.EventSocket;
import de.lmu.ifi.bio.network.event.Event;
import de.lmu.ifi.bio.network.exception.ConnectionNotReady;

public abstract class Eventhandler {

	/**
	 * Handles a event which was send from the other side to reciever
	 * @param event
	 * @param reciever
	 * @return
	 */
	public abstract boolean handleEvent(Event event, EventSocket reciever) throws ConnectionNotReady;
	
	public abstract Class<?> getType();
}
