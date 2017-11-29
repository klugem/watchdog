package de.lmu.ifi.bio.watchdog.helper;

import de.lmu.ifi.bio.watchdog.logger.Logger;

public interface GetPassword {

	/**
	 * requests the user to enter an password
	 * @param name
	 * @param log
	 * @return
	 */
	public byte[] requestPasswortInputFromUser(String name, Logger log) throws Exception;
}
