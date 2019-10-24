package de.lmu.ifi.bio.watchdog.helper;

import de.lmu.ifi.bio.watchdog.logger.Logger;

/**
 * 
 * @author kluge
 *
 */
public class UserConfirmation {
	
	private static UserConfirmationInterface INST = new CMDConfirmation();
	
	/**
	 * requests confirmation from the user
	 * @param question
	 * @param log
	 * @return
	 */
	public static boolean confirm(String question, Logger log) {
		return UserConfirmation.INST.confirm(question, log);
	}
	
	/**
	 * allows to set a new user confirmation requester
	 * @param cr
	 */
	public static void setUserConfirmationRequester(UserConfirmationInterface cr) {
		UserConfirmation.INST = cr;
	}
}
