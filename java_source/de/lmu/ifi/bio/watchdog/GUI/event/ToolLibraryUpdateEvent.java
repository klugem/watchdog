package de.lmu.ifi.bio.watchdog.GUI.event;

import javafx.event.Event;
import javafx.event.EventType;

public class ToolLibraryUpdateEvent extends Event {

	private static final long serialVersionUID = 3491838595835103933L;
	public static final EventType<ToolLibraryUpdateEvent> TOOL_LIB_EVENT_TYPE = new EventType<>("TOOL_LIB_EVENT_TYPE");
	
	public ToolLibraryUpdateEvent() {
		super(TOOL_LIB_EVENT_TYPE);
	}
}
