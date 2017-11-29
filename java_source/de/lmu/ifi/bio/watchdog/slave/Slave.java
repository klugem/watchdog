package de.lmu.ifi.bio.watchdog.slave;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import de.lmu.ifi.bio.network.client.Client;
import de.lmu.ifi.bio.network.exception.ConnectionNotReady;
import de.lmu.ifi.bio.watchdog.slave.clientEvents.FinishedHandshakeEH;

public class Slave extends Client {
	
	private final String ID;
	
	/**
	 * Factory
	 * @param addr
	 * @param port
	 * @return
	 * @throws IOException
	 * @throws IllegalArgumentException
	 * @throws ConnectionNotReady 
	 */
	public static Slave getSlave(String addr, int port, String id) throws IOException, IllegalArgumentException, ConnectionNotReady {
		SocketChannel s;
		try {
			s = SocketChannel.open(new InetSocketAddress(addr, port));
			System.out.println("[STATUS] Established connection to '" + addr + "' on port '"+port+"' (id="+id+").");
		}
		catch(ConnectException e) {
			throw new IllegalArgumentException("Couldn't connect to the server. Probably no server running under "+addr+":"+port+": " + e.getMessage());
        }
		
		return new Slave(s, id);
	}

	/**
	 * Constructor
	 * @param s
	 * @param id
	 * @throws IOException
	 * @throws ConnectionNotReady
	 */
	public Slave(SocketChannel s, String id) throws IOException, ConnectionNotReady {
		super(s);
		this.ID = id;
		if(id == null) {
			try { throw new IllegalArgumentException(); }
			catch (Exception e) { e.printStackTrace(); }
			System.exit(1);
		}
		this.getEventSocket().registerEventHandler(new FinishedHandshakeEH(this.ID));
		this.start();
	}
}
