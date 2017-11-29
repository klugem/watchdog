package de.lmu.ifi.bio.network.server;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.lmu.ifi.bio.multithreading.StopableLoopRunnable;
import de.lmu.ifi.bio.network.exception.ConnectionNotReady;


/**
 * Socket server that can handle multiple connections at once.
 * @author Michael Kluge
 *
 */
public class Server extends StopableLoopRunnable {

	private final int PORT;
	private final ServerSocketChannel CHANNEL;
	private final HashMap<String, ServerConnectionHandler> CONNECTION = new HashMap<>();
	private final HashMap<String, Future<?>> RUNNING = new HashMap<>();
	private final Selector SELECTOR = Selector.open();
	private final ExecutorService PROCESS_THREADS;
	
	/**
	 * Factory
	 * @param port
	 * @return
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public static Server getServer(int port, int processThreads) throws IOException, IllegalArgumentException {
		return new Server(port, processThreads);
	}
	
	/**
	 * Constructor
	 * @param port
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public Server(int port, int processThreads) throws IOException, IllegalArgumentException {
		super("Server");
		this.PORT = port;
		this.PROCESS_THREADS = Executors.newFixedThreadPool(processThreads);
		ServerSocketChannel sc = ServerSocketChannel.open();
		try {
			sc.bind(new InetSocketAddress(this.PORT));
			sc.configureBlocking(false);
			sc.register(this.SELECTOR, SelectionKey.OP_ACCEPT);
		}
		catch(BindException b) {
			throw new IllegalArgumentException("Some other server is runnning on port "+this.PORT+".");
        }
		this.CHANNEL = sc;
		System.out.println("Started server on port "+this.CHANNEL.socket().getLocalPort()+"...");
	}
	
	@Override
	public void afterLoop() {
		this.PROCESS_THREADS.shutdownNow();
		
		// showdown connections
		for(ServerConnectionHandler s : this.CONNECTION.values()) {
			s.disconnect();
		}
	}
	
	@Override
	public int executeLoop() {
    	int num = 0;
    	try { num = this.SELECTOR.selectNow(); }
    	catch(Exception e) {
    		System.err.println("Exception occurred while checking if server needs to respond to anything.");
    		e.printStackTrace();
    	}
    	if(num == 0) return 1;

    	for(Iterator<SelectionKey> it = this.SELECTOR.selectedKeys().iterator(); it.hasNext(); ) {
    		SelectionKey key = (SelectionKey) it.next();
    		it.remove();
    		
    		if((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
    			try {
    				this.add(this.CHANNEL.accept());
    			}
    			catch(Exception e) {
    	        	System.err.println("Exception occurred while trying to connect to a new client.");
    	        	e.printStackTrace();
    			}
    		}
    		else if((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
    			try {
        			SocketChannel sc = (SocketChannel) key.channel();
        			String id = getID(sc.socket());
        			// let a worker thread process the data
        			if(this.CONNECTION.containsKey(id) && (this.RUNNING.get(id) == null || this.RUNNING.get(id).isDone())) {
            			System.out.println("new data is there from client: '"+id+"'");
        				ServerConnectionHandler h = this.CONNECTION.get(id);
        				Future<?> f = this.PROCESS_THREADS.submit(() -> { 
        					try { h.processEventsWhileBlocking(); } 
        					catch (Exception e) { if(!h.isConnectionEndSignaled()) e.printStackTrace();}});
        				// update running stuff
        				this.RUNNING.put(id, f);
        			}
    			}
    			catch(Exception e) {
    	        	System.err.println("Exception occurred while reading data from client.");
    	        	e.printStackTrace();
    			}
    		}
    	}
    	return 1;
	}
	
	public static String getID(Socket s) {
		return s.getInetAddress().toString().replaceFirst("^/", "") + ":" + s.getPort();
	}
	
	/**
	 * add a new connection that was accepted
	 * @param accept
	 * @throws IOException 
	 * @throws ConnectionNotReady 
	 */
	private void add(SocketChannel accept) throws IOException, ConnectionNotReady {
		ServerConnectionHandler c = new ServerConnectionHandler(accept);
		accept.configureBlocking(false);
		accept.register(this.SELECTOR, SelectionKey.OP_READ);
		this.CONNECTION.put(Server.getID(accept.socket()), c);
		System.out.println("New client connected to server. Now "+this.CONNECTION.size()+" connections are open.");
		
		this.register(c);
	}

	/**
	 * Can be overwritten in order to perform some additional tasks after the client is connected 
	 * @param c
	 */
	public void register(ServerConnectionHandler c) {

	}
	
	/**
	 * returns the port the server is running on.
	 * @return
	 */
	public int getPort() {
		return this.CHANNEL.socket().getLocalPort();
	}
	@Override
	public long getDefaultWaitTime() {
		return 1;
	}

	@Override
	public void beforeLoop() {

	}

}
