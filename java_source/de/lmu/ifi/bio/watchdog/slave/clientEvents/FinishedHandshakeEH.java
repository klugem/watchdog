package de.lmu.ifi.bio.watchdog.slave.clientEvents;

import de.lmu.ifi.bio.network.EventSocket;
import de.lmu.ifi.bio.network.event.Event;
import de.lmu.ifi.bio.network.event.FinishedHandshake;
import de.lmu.ifi.bio.network.eventhandler.Eventhandler;
import de.lmu.ifi.bio.network.exception.ConnectionNotReady;

/**
 * EndOfConnection handler
 * @author Michael Kluge
 * @version 1.0
 */
public final class FinishedHandshakeEH extends Eventhandler {
	
	private final String ID;
	
	public FinishedHandshakeEH(String id) {
		this.ID = id;
	}

	@Override
	public boolean handleEvent(Event event, EventSocket reciever) throws ConnectionNotReady {
		if(event instanceof FinishedHandshake) {
			try {
				reciever.send(new IdentifyEvent(this.ID));
				return true;
			} catch (ConnectionNotReady e1) {
				// should not happen in any case because this method is called once the connection is ready! ;)
				System.out.println("[ERROR] Failed to send id to server because connection was not ready!");
				e1.printStackTrace();
			}
			return false;
		}
		return false;
	}

	@Override
	public Class<?> getType() {
		return FinishedHandshake.class;
	}
}