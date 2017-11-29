package de.lmu.ifi.bio.utils.interfaces;

import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;

public interface Logable {
	final Logger LOGGER = new Logger(LogLevel.INFO);
	
	default public void setLogLevel(LogLevel l) {
		LOGGER.setLogLevel(l);
	}
}
