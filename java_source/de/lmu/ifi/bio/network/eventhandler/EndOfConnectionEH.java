package de.lmu.ifi.bio.network.eventhandler;

import de.lmu.ifi.bio.network.EventSocket;
import de.lmu.ifi.bio.network.event.EndOfConnection;
import de.lmu.ifi.bio.network.event.Event;
import de.lmu.ifi.bio.network.exception.ConnectionNotReady;

/**
 * EndOfConnection handler
 * @author Michael Kluge
 * @version 1.0
 */
public final class EndOfConnectionEH extends Eventhandler {

	@Override
	public boolean handleEvent(Event event, EventSocket reciever) throws ConnectionNotReady {
		if(event instanceof EndOfConnection) {
			reciever.setExpectedEndOfConnection();
			return true;
		}
		return false;
	}

	@Override
	public Class<?> getType() {
		return EndOfConnection.class;
	}
}