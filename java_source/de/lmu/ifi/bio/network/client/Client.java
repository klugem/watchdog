package de.lmu.ifi.bio.network.client;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import de.lmu.ifi.bio.network.EventSocket;
import de.lmu.ifi.bio.network.exception.ConnectionNotReady;

/**
 * Socket client
 * @author Michael Kluge
 *
 */
public class Client extends Thread {
	
	private final ClientConnectionHandler CONNECTION_HANDLER;

	/**
	 * Factory
	 * @param addr
	 * @param port
	 * @return
	 * @throws IOException
	 * @throws IllegalArgumentException
	 * @throws ConnectionNotReady 
	 */
	public static Client getClient(String addr, int port) throws IOException, IllegalArgumentException, ConnectionNotReady {
		SocketChannel s;
		try {
			s = SocketChannel.open(new InetSocketAddress(addr, port));
			System.out.println("Established connection to '" + addr + "' on port '"+port+"'.");
		}
		catch(ConnectException e) {
			throw new IllegalArgumentException("Couldn't connect to the server. Probably no server running under "+addr+":"+port+": " + e.getMessage());
        }
		
		return new Client(s);
	}
	
	/**
	 * Constructor
	 * @param Socket
	 * @throws IOException 
	 * @throws ConnectionNotReady 
	 */
	public Client(SocketChannel s) throws IOException, ConnectionNotReady {
		this.CONNECTION_HANDLER = new ClientConnectionHandler(s);
    }
	
	@Override
	public void run() {
		try {
            while(true) {          
            	int num = this.CONNECTION_HANDLER.getSelector().selectNow();
            	if(num == 0) continue;
            	for(Iterator<SelectionKey> it = this.CONNECTION_HANDLER.getSelector().selectedKeys().iterator(); it.hasNext(); ) {
            		SelectionKey key = (SelectionKey) it.next();
            		it.remove();
            		if((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
            			this.CONNECTION_HANDLER.processEventsWhileBlocking();
            		}
            	}
            }
		}
        catch(Exception e) { 
        	// omit this error message if the end of the connection was signaled before
        	if(!this.CONNECTION_HANDLER.isConnectionEndSignaled())
        		e.printStackTrace();
        }
        finally {
            System.out.print(this.CONNECTION_HANDLER + ": ending connection...");
            
            this.CONNECTION_HANDLER.setReady(false);
            this.CONNECTION_HANDLER.disconnect();
            
            // end client
            System.exit(0);
        }
	}

	public EventSocket getEventSocket() {
		return this.CONNECTION_HANDLER;
	}
}
