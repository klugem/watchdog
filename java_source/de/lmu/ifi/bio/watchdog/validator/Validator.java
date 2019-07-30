package de.lmu.ifi.bio.watchdog.validator;

import java.io.File;

import de.lmu.ifi.bio.watchdog.helper.LogMessageEvent;
import de.lmu.ifi.bio.watchdog.interfaces.BasicEventHandler;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;

public abstract class Validator extends Logger {

	private static final long serialVersionUID = -6849130147418180446L;
	private final String NAME;
	private final File WATCHDOG_BASE_DIR;
	public final Logger LOGGER = new Logger();
	
	public Validator(String name, File watchdogBase) {
		this.NAME = name;
		this.WATCHDOG_BASE_DIR = watchdogBase;
	}
	
	public Validator(String name, File watchdogBase, BasicEventHandler<LogMessageEvent> eh) {
		this.NAME = name;
		this.WATCHDOG_BASE_DIR = watchdogBase;
		if(eh != null)
			Logger.registerListener(eh, LogLevel.DEBUG);
	}

	/**
	 * Method that is used to validate something
	 * @return
	 */
	public abstract boolean validate();
	
	/**
	 * name of this validator
	 * @return
	 */
	public String getValidatorName() {
		return this.NAME;
	}
	
	public File getWatchdogBaseDir() {
		return this.WATCHDOG_BASE_DIR;
	}
}
