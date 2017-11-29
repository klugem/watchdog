package de.lmu.ifi.bio.watchdog.helper;

import de.lmu.ifi.bio.network.event.Event;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;


public class LogMessageEvent extends Event {

	private static final long serialVersionUID = 7150330550769082713L;
	private final String MESSAGE;
	private final LogLevel LEVEL;

	public LogMessageEvent(String message, LogLevel l) {
		super();
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
