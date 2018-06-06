package de.lmu.ifi.bio.watchdog.slave.clientEvents;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentHashMap;

import de.lmu.ifi.bio.network.EventSocket;
import de.lmu.ifi.bio.network.event.Event;
import de.lmu.ifi.bio.network.eventhandler.Eventhandler;
import de.lmu.ifi.bio.network.exception.ConnectionNotReady;
import de.lmu.ifi.bio.network.server.ServerConnectionHandler;
import de.lmu.ifi.bio.watchdog.slave.Master;
import de.lmu.ifi.bio.watchdog.task.Task;

/**
 * Event handler for task status update events
 * @author Michael Kluge
 *
 */
public class IdentifyEventEH extends Eventhandler {
	private final ConcurrentHashMap<String, ServerConnectionHandler> CONS;
	private final ConcurrentHashMap<String, LinkedHashMap<Task, LinkedHashSet<Integer>>> WAITING;
	private final ConcurrentHashMap<String, Boolean> PENDING;
	
	/**
	 * Constructor
	 * @param cons
	 */
	public IdentifyEventEH(ConcurrentHashMap<String, ServerConnectionHandler> cons, ConcurrentHashMap<String, LinkedHashMap<Task, LinkedHashSet<Integer>>> waiting, ConcurrentHashMap<String, Boolean> pending) {
		this.CONS = cons;
		this.WAITING = waiting;
		this.PENDING = pending;
	}

	@Override
	public Class<?> getType() {
		return IdentifyEvent.class;
	}

	@Override
	public boolean handleEvent(Event e, EventSocket reciever) throws ConnectionNotReady {
		if(reciever.isClient())
			return false;
		if(!(e instanceof IdentifyEvent))
			return false;
		if(!(reciever instanceof ServerConnectionHandler)) // because this is a server event handler that should be the case!
			return true;
		
		IdentifyEvent event = (IdentifyEvent) e;
		String id = event.getID();
		System.out.println("identity was recieved from " + id);
		this.CONS.put(id, (ServerConnectionHandler) reciever);
		this.PENDING.remove(id);
		
		// send tasks that are waiting
		if(this.WAITING.containsKey(id)) {
			for(Task t : this.WAITING.get(id).keySet()) {
				LinkedHashSet<Integer> depToKeep = this.WAITING.get(id).get(t);
				Master.registerTask(t.getID(), id);
				reciever.send(new TaskExecutorEvent(t, depToKeep));
				System.out.println("task was sent to host " + t.getID());
			}
			// delete all that tasks
			this.WAITING.remove(id);
		}
		return true;
	}

}