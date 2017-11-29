package de.lmu.ifi.bio.network.server;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import de.lmu.ifi.bio.network.EventSocket;
import de.lmu.ifi.bio.network.event.Event;
import de.lmu.ifi.bio.network.exception.ConnectionNotReady;

public class ServerConnectionHandler extends EventSocket {

	public static String VERSION = "2.0";
	
	public ServerConnectionHandler(SocketChannel s) throws IOException, ConnectionNotReady {
		super(s, true);
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
