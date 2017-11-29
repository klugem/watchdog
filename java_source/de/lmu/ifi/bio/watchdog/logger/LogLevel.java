package de.lmu.ifi.bio.watchdog.logger;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Helper class for creating log messages
 * @author Michael Kluge
 *
 */
public enum LogLevel {
	DEBUG(3, "DEBUG"), INFO(2, "INFO "), WARNING(1, "WARN "), ERROR(0, "ERROR"), QUIET(-1, "QUIET");
	
	private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss");
	private final int LEVEL;
	private final String PREFIX;
	
	/**
	 * Constructor
	 * @param level
	 * @param prefix
	 */
	private LogLevel(int level, String prefix) {
		this.LEVEL = level;
		this.PREFIX = prefix;
	}
	
	/**
	 * Tests, if a message should be printed or not
	 * @param currentMessage log level of the message to print
	 * @return
	 */
	public boolean printMessage(LogLevel currentMessage) {
		return this.LEVEL >= currentMessage.LEVEL;
	}
	
	@Override
	public String toString() {
		Date date = new Date();
		return "[" + this.PREFIX + ", " + DATE_FORMAT.format(date) + "] ";
	}
}
