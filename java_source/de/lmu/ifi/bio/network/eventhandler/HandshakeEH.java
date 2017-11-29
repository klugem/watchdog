package de.lmu.ifi.bio.network.eventhandler;

import de.lmu.ifi.bio.network.EventSocket;
import de.lmu.ifi.bio.network.event.Event;
import de.lmu.ifi.bio.network.event.FinishedHandshake;
import de.lmu.ifi.bio.network.event.Handshake;
import de.lmu.ifi.bio.network.exception.ConnectionNotReady;

/**
 * Handshake handler
 * @author Michael Kluge
 * @version 1.0
 */
public final class HandshakeEH extends Eventhandler {

	@Override
	public boolean handleEvent(Event event, EventSocket reciever) throws ConnectionNotReady {
		Handshake h = null;
		if(event instanceof Handshake) {
			h = (Handshake) event;
		}
		else
			return false;
		
		h.setReceived();
		
		// test for correct version
		if(!h.getVersion().equals(reciever.getVersion())) {
			h.setWrongVersion(reciever.getVersion());
			reciever.send(event);
			
			if(reciever.isServer()) {
				System.out.println("System, which tries to connect has the wrong version: "+ h.getVersion());
				reciever.disconnect();
			}
			else {
				if(reciever.isClient()) {
					System.out.println("Server has incompatible version: "+h.getWrongVersion());
				}
				reciever.disconnect();
			}
		}
		// correct version
		else {
			if(reciever.isServer() && h.isBackServer()) {
					reciever.setReady(true);
					System.out.println("Handshake was successfull");
					reciever.send(event);
			}
			else if(reciever.isClient() && h.isBackClient()) {
				reciever.setReady(true);
				System.out.println("Handshake was successfull");
				
				// handshake is finished
				reciever.send(new FinishedHandshake());
				reciever.handle(new FinishedHandshake());
			}
		}
		return true;
	}

	@Override
	public Class<?> getType() {
		return Handshake.class;
	}
}