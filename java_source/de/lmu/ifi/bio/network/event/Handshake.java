package de.lmu.ifi.bio.network.event;


/**
 * Handshake event
 * @author Michael Kluge
 *
 */
public final class Handshake extends Event {

	private static final long serialVersionUID = -4807189484641757929L;
	private short received = 0;
	private boolean wrong = false;
	private String wrongVersion = "";
	private final String VERSION;
	private String hostname;

	/**
	 * Handshake constructor
	 * @param version
	 * @param hostname
	 */
	public Handshake(String version, String hostname) {
		this.VERSION = version;
		this.hostname = hostname;
	}
	
	/**
	 * Returns the hostname of the host who sent this handshake, of this Handshake
	 * @return
	 */
	public String getHostname() {
		return this.hostname;
	}
	
	/**
	 * Returns the received status
	 * @return
	 */
	public boolean isBackClient() {
		return this.received == 2;
	}
	
	public boolean isBackServer() {
		return this.received == 1;
	}

	/**
	 * Sets the received option true
	 */
	public void setReceived() {
		this.received++;
	}
	
	/**
	 * Sets the wrong option true
	 */
	public void setWrongVersion(String version) {
		this.wrong = true;
		this.wrongVersion = version;
	}
	
	/**
	 * Returns the wrong version status
	 * @return
	 */
	public boolean isWrongVersion() {
		return this.wrong;
	}
	
	/**
	 * Returns the Version of the System, which tries to connect.
	 */
	public String getVersion() {
		return this.VERSION;
	}
	
	
	/**
	 * Returns the Version of the other System, if the Version is different.
	 * @return
	 */
	public String getWrongVersion() {
		return this.wrongVersion;
	}
}
