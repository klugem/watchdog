package de.lmu.ifi.bio.watchdog.helper;

import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import javafx.event.Event;
import javafx.event.EventType;


public class LogMessageEvent extends Event {

	public static final EventType<LogMessageEvent> LOGMESSAGE_EVENT_TYPE = new EventType<>("LOGMESSAGE_EVENT_TYPE");
	private static final long serialVersionUID = 7150330550769082712L;
	private final String MESSAGE;
	private final LogLevel LEVEL;

	public LogMessageEvent(String message, LogLevel l) {
		super(LOGMESSAGE_EVENT_TYPE);
		this.MESSAGE = message;
		this.LEVEL = l;
	}
	
	public String getMessage() {
		return this.MESSAGE;
	}
	
	public LogLevel getLogLevel() {
		return this.LEVEL;
	}
}
