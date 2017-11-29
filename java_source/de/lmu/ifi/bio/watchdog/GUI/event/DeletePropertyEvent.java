package de.lmu.ifi.bio.watchdog.GUI.event;

import javafx.event.Event;
import javafx.event.EventType;

/** Event which is sent when a property should be removed */
public class DeletePropertyEvent extends Event {
	
	private static final long serialVersionUID = -4994533003722375362L;
	public static final EventType<DeletePropertyEvent> DELETE_EVENT_TYPE = new EventType<>("DELETE_EVENT_TYPE");
	
	private final int NUMBER;
	
	public DeletePropertyEvent(int number) {
		super(DeletePropertyEvent.DELETE_EVENT_TYPE);
		this.NUMBER = number;
	}
	
	public int getNumber() {
		return this.NUMBER;
	}
}
