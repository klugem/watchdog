package de.lmu.ifi.bio.network.eventhandler;

import de.lmu.ifi.bio.network.EventSocket;
import de.lmu.ifi.bio.network.event.Event;
import de.lmu.ifi.bio.network.event.ResendLastObjectEvent;
import de.lmu.ifi.bio.network.exception.ConnectionNotReady;

/**
 * EndOfConnection handler
 * @author Michael Kluge
 * @version 1.0
 */
public final class ResendLastObjectEventEH extends Eventhandler {

	@Override
	public boolean handleEvent(Event event, EventSocket reciever) throws ConnectionNotReady {
		if(event instanceof ResendLastObjectEvent) {
			reciever.resendLastObject();
			return true;
		}
		return false;
	}

	@Override
	public Class<?> getType() {
		return ResendLastObjectEvent.class;
	}
}