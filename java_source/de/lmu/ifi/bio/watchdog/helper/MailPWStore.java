package de.lmu.ifi.bio.watchdog.helper;

import javax.mail.PasswordAuthentication;

/**
 * Stores mail credentials. Because it is not very secure it should not be used with passwords that are used at other places.
 * 
 * @author Michael Kluge
 *
 */
public class MailPWStore extends javax.mail.Authenticator {
	
	private final String USER;
	private final String PW;
	
	public MailPWStore(String u, String p) {
		this.USER = u;
		this.PW = p;
	}
	
	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication(this.USER, this.PW);
	}
}
