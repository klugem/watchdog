package de.lmu.ifi.bio.watchdog.GUI.event;

import javafx.event.Event;
import javafx.event.EventType;

public class WatchdogBaseDirUpdateEvent extends Event {

	private static final long serialVersionUID = 3491838595835103933L;
	public static final EventType<WatchdogBaseDirUpdateEvent> WATHCODG_BASE_DIR_UPDATE_EVENT_TYPE = new EventType<>("WATHCODG_BASE_DIR_UPDATE_EVENT_TYPE");
	
	private final String BASE;
	
	public WatchdogBaseDirUpdateEvent(String baseDir) {
		super(WATHCODG_BASE_DIR_UPDATE_EVENT_TYPE);
		this.BASE = baseDir;
	}

	public String getNewBaseDir() {
		return this.BASE;
	}
}
