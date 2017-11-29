package de.lmu.ifi.bio.watchdog.GUI.event;

import javafx.event.Event;
import javafx.event.EventType;

public abstract class TabEvent extends Event {

	private static final long serialVersionUID = -206954389394000448L;
	private final String TARGET_TAB_NAME;

	public TabEvent(String tabname, EventType<? extends Event> eventType) {
		super(eventType);
		this.TARGET_TAB_NAME = tabname;
	}
	
	public String getTargetTabName() {
		return this.TARGET_TAB_NAME;
	}
}
