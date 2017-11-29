package de.lmu.ifi.bio.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import de.lmu.ifi.bio.network.event.EndOfConnection;
import de.lmu.ifi.bio.network.event.Event;
import de.lmu.ifi.bio.network.event.Handshake;
import de.lmu.ifi.bio.network.event.ResendLastObjectEvent;
import de.lmu.ifi.bio.network.eventhandler.EndOfConnectionEH;
import de.lmu.ifi.bio.network.eventhandler.Eventhandler;
import de.lmu.ifi.bio.network.eventhandler.HandshakeEH;
import de.lmu.ifi.bio.network.eventhandler.ResendLastObjectEventEH;
import de.lmu.ifi.bio.network.exception.ConnectionNotReady;


public abstract class EventSocket {
    
	public static String GOT = "got";
	public static String SEND = "send";
	private static int WAIT_MILLI = 10;
	private static int WAIT_UNTIL_FORCE_RESEND = 10000; // after 10 seconds, assume that something went wrong --> request resend
	private final HashMap<Class<?>, Eventhandler> HANDLER = new HashMap<>();
	private SocketChannel socket;
    protected boolean ready = false;
    protected final boolean IS_SERVER;
    private boolean connectionEndSignaled = false;
    private Object lastSendObject = null;
    
    /**
     * Constructor
     * @param s
     * @throws IOException
     * @throws ConnectionNotReady 
     */
    public EventSocket(SocketChannel s, boolean threadRunningOnServer) throws IOException, ConnectionNotReady {
    	this.socket = s;
    	this.IS_SERVER = threadRunningOnServer;
    	    	
    	// register handshake eventhandler
    	this.registerEventHandler(new HandshakeEH());
    	this.registerEventHandler(new ResendLastObjectEventEH());
    	
    	// send handshake event to server
    	if(this.isClient()) {
    		this.send(new Handshake(this.getVersion(), InetAddress.getLocalHost().getHostName())); // force sending the header!
    	}
    	
	    // register the event handler that will supress the exception at the end of the connection if it is planed!
	    this.registerEventHandler(new EndOfConnectionEH());
    }
        
    public synchronized boolean isReady() {
    	return this.ready;
    }
    
	/**
	 * Sets the ready status of the connection
	 * @param status
	 */
	public synchronized void setReady(boolean status) {
		this.ready = status;
		System.out.println("status of connection: " + this.ready);
	}

	public boolean isServer() {
    	return this.IS_SERVER;
    }
    
    public boolean isClient() {
    	return !this.IS_SERVER;
    }
    
    /**
     * waits for object sent from the other side
     * @throws IOException 
     * @throws ClassNotFoundException 
     * @throws ConnectionNotReady 
     */
    public synchronized void processEventsWhileBlocking() throws ClassNotFoundException, IOException, ConnectionNotReady {
    	int readBytes = 0;
    	int nothingGot = 0;
    	Object o = null;
        // read the size of the object that will come
    	ByteBuffer lengthBuffer = ByteBuffer.wrap(new byte[4]);
    	 while(lengthBuffer.hasRemaining()) {
    		 readBytes = this.socket.read(lengthBuffer);
    		 if(readBytes == 0) 
    			 nothingGot++;
    		 
    		 if(nothingGot*WAIT_MILLI == WAIT_UNTIL_FORCE_RESEND) {
    			 this.send(new ResendLastObjectEvent());
    			 return;
    		 }
     		 try { Thread.sleep(WAIT_MILLI); } catch(Exception e) {}
    	 }

    	lengthBuffer.flip();
    	int size = lengthBuffer.getInt();
    	ByteBuffer buffer = ByteBuffer.wrap(new byte[size]);

    	// try to fill the complete buffer
        while(buffer.hasRemaining()) {
        	 readBytes = this.socket.read(buffer);
    		 if(readBytes == 0) 
    			 nothingGot++;
    		 
    		 if(nothingGot*WAIT_MILLI == WAIT_UNTIL_FORCE_RESEND) {
    			 this.send(new ResendLastObjectEvent());
    			 return;
    		 }
        	try { Thread.sleep(WAIT_MILLI); } catch(Exception e) { e.printStackTrace(); }
        }

        // parse the object
       if(buffer.hasRemaining() == false) {
            buffer.flip();

            InputStream bais = new ByteArrayInputStream(buffer.array(), 0, buffer.limit());
            ObjectInputStream ois = new ObjectInputStream(bais); 
            try { o = ois.readObject(); } 
            catch(OptionalDataException e) {
            	e.printStackTrace();
            	System.out.println("length: " + e.length);
            }
            ois.close();
            System.out.println("received object of size " + size + ": " + o.getClass().getSimpleName());
        }
    	
    	// decide what action should be performed. 
    	// handle object can be overwritten
       if(o != null) {
			if(o instanceof Event) {
				this.handle((Event) o);
			}
			else {
				this.handleObject(o);
			}
       }
    }
    
    public void resendLastObject() {
    	if(this.lastSendObject != null) {
			System.out.println("[WARNING] Resend of last object was requested!");    	
	    	try { this.send(this.lastSendObject); }
	    	catch(Exception e) {
	    		System.err.println("Failed to resend last object:");
	    		e.printStackTrace();
	    	}
	    }
    }
    
    /**
     * handle objects that are send from the other side
     * @param o
     */
    protected synchronized void handleObject(Object o) {
		System.out.println("Received unhandeled object of type '"+o.getClass()+"'!");
	}

	/**
     * sends a object to the other side
     * @param o
     * @return
     * @throws ConnectionNotReady
     */
    public synchronized boolean send(Object o) throws ConnectionNotReady {
        if (this.socket != null && (this.ready == true || o instanceof Handshake)) {				
        	try {
        		// mark the end of the connection if a object of this type is sent to the partner
        		if(o instanceof EndOfConnection) {
        			this.setExpectedEndOfConnection();
        		}
        		// prepare the stream
        		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        	    ObjectOutputStream oos = new ObjectOutputStream(baos);
        	    oos.writeObject(o);
        	    
        	    // send number of bytes to receive
        	    byte[] data = baos.toByteArray();
        	    int size = data.length;
        	    System.out.println("writing object of " + size + " bytes...");
        	    ByteBuffer bl = ByteBuffer.wrap(intToByteArray(size));
        	    while(bl.hasRemaining()) 
        	    	this.socket.write(bl); // send until all is gone through the connection

        	    // send object itself
        	    ByteBuffer bo = ByteBuffer.wrap(data);
        	    System.out.print("transfer of " + o.getClass().getSimpleName() + " was started...");
        	    while(bo.hasRemaining()) 
        	    	this.socket.write(bo); // send until all is gone through the connection
        	    System.out.println("done!");
        	    
        	    // store that for later use
        	    this.lastSendObject = o; 
				return true;
			 
			} catch (IOException e) { e.printStackTrace();  throw new ConnectionNotReady();  }        
        }
        else if(this.ready == false){
        	System.out.println("Wait with sending objects until connection is ready!");
        	throw new ConnectionNotReady();
        }
        return false;
    }
    
    public byte[] intToByteArray(int value) {
        return new byte[] {(byte)(value >>> 24), (byte)(value >>> 16), (byte)(value >>> 8), (byte)value};
    }
    
    /**
     * end the connection
     */
    public void disconnect() {
		 try { 
			 // send event to the other side that connection will end now if termination comes from this side
			 if(!this.connectionEndSignaled)
				 this.send(new EndOfConnection());
			 // close the socket, if still open
			 this.socket.close();
		 }
		 catch(IOException e) { e.printStackTrace(); } catch (ConnectionNotReady e) { e.printStackTrace();};
    }
    
    /**
     * Handles events send from the other side
     * @param e
     */
    public synchronized void handle(Event e) {
    	// check, if a eventhandler is registered for that event
    	Class<?> c = e.getClass();
    	if(this.HANDLER.containsKey(c)) {
    		try {
    			this.HANDLER.get(c).handleEvent(e, this);
    		}
    		catch(Exception ex) {
    			if(!this.connectionEndSignaled)
    				System.out.println("Reciever connection was not ready!");
    			ex.printStackTrace();
    		}
    	}
    	else {
    		System.out.println("No eventhandler was registered for event of type '"+c.getSimpleName()+"'!");
    		System.out.println("Registered handlers: " + StringUtils.join(this.HANDLER.keySet(), ", "));
    	}
    }
    
    public boolean isConnectionEndSignaled() {
    	return this.connectionEndSignaled;
    }
    
    /**
     * ensure that client and server have the same version
     */
    public abstract String getVersion();
    
    /**
     * registers a new event handler
     * @param handler
     */
    public synchronized void registerEventHandler(Eventhandler handler) {
    	this.HANDLER.put(handler.getType(), handler);
    }

    /**
     * the other side will terminate the connection 
     * do not print any errors
     */
	public synchronized void setExpectedEndOfConnection() {
		this.connectionEndSignaled = true;
	}
}
