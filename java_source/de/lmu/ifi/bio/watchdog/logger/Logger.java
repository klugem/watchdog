package de.lmu.ifi.bio.watchdog.logger;

import java.io.Serializable;
import java.util.HashMap;

import de.lmu.ifi.bio.watchdog.helper.LogMessageEvent;
import javafx.application.Platform;
import javafx.event.EventHandler;


/**
 * Simple logger class
 * @author Michael Kluge
 *
 */
public class Logger implements Serializable {

	private static final long serialVersionUID = 6336667886532225389L;
	private LogLevel logLevel = LogLevel.INFO;
	
	private static final HashMap<EventHandler<LogMessageEvent>, LogLevel> LISTENER = new HashMap<>();
	
	/**
	 * default constructor
	 */
	public Logger() {}
	
	/**
	 * Constructor
	 * @param initialLogLevel
	 */
	public Logger(LogLevel initialLogLevel) {
		this.setLogLevel(initialLogLevel);
	}
	
	public static void registerListener(EventHandler<LogMessageEvent> e, LogLevel filterLevel) {
		LISTENER.put(e, filterLevel);
	}
	
	public static void unregisterListener(EventHandler<LogMessageEvent> e) {
		LISTENER.remove(e);
	}
	
	private void processListeners(String message, LogLevel l) {
		for(EventHandler<LogMessageEvent> e : LISTENER.keySet()) {
			LogLevel lLevel = LISTENER.get(e);
			if(lLevel.printMessage(l)) 
				Platform.runLater(() -> e.handle(new LogMessageEvent(message, l)));
		}
	}

	/**
	 * prints a debug message
	 * @param message
	 */
	public void debug(String message) {
		if(this.logLevel.printMessage(LogLevel.DEBUG)) {
			System.out.println(LogLevel.DEBUG + message);
		}
		this.processListeners(message, LogLevel.DEBUG);
	}
	
	/**
	 * prints an information message
	 * @param message
	 */
	public void info(String message) {
		if(this.logLevel.printMessage(LogLevel.INFO)) {
			System.out.println(LogLevel.INFO + message);
		}
		this.processListeners(message, LogLevel.INFO);
	}
	
	/**
	 * prints a warning
	 * @param message
	 */
	public void warn(String message) {
		if(this.logLevel.printMessage(LogLevel.WARNING)) {
			System.out.println(LogLevel.WARNING + message);
		}
		this.processListeners(message, LogLevel.WARNING);
	}
	
	/**
	 * prints an error
	 * @param message
	 */
	public void error(String message) {
		if(this.logLevel.printMessage(LogLevel.ERROR)) {
			System.out.println(LogLevel.ERROR + message);
		}
		this.processListeners(message, LogLevel.ERROR);
	}
	
	/**
	 * sets a new log level
	 * @param logLevel
	 */
	public void setLogLevel(LogLevel logLevel) {
		this.logLevel = logLevel;
	}

	/**
	 * prints a stack trace
	 */
	public void printStackTrace() {
		try { throw new Exception(); } catch(Exception e) { e.printStackTrace(); }
	}
}
