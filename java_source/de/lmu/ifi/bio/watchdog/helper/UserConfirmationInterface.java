package de.lmu.ifi.bio.watchdog.helper;

import de.lmu.ifi.bio.watchdog.logger.Logger;

/**
 * Should be used to get a confirmation from the user
 * @author kluge
 *
 */
public interface UserConfirmationInterface {
	
	/**
	 * requests a confirmation by the user
	 * @param question
	 * @param log
	 * @return
	 */
	public boolean confirm(String question, Logger log);
}
