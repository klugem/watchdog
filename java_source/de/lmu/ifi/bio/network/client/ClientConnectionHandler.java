package de.lmu.ifi.bio.network.client;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import de.lmu.ifi.bio.network.EventSocket;
import de.lmu.ifi.bio.network.event.Event;
import de.lmu.ifi.bio.network.exception.ConnectionNotReady;

public class ClientConnectionHandler extends EventSocket {

	public static String VERSION = "2.0";
	private final Selector SELECTOR = Selector.open();
	
	public ClientConnectionHandler(SocketChannel s) throws IOException, ConnectionNotReady {
		super(s, false);
		
		// register listener for incoming data
		s.configureBlocking(false);
		s.register(this.SELECTOR, SelectionKey.OP_READ);
	}
	
	public Selector getSelector() {
		return this.SELECTOR;
	}
	
	@Override
	public void handle(Event e) {
		super.handle(e);
	}

	@Override
	public String getVersion() {
		return VERSION;
	}
}
