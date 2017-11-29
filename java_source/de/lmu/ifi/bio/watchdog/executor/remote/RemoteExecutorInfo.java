package de.lmu.ifi.bio.watchdog.executor.remote;

import java.util.HashMap;

import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Executor info for ssh executor
 * @author Michael Kluge
 *
 */
public class RemoteExecutorInfo extends ExecutorInfo {
	
	private static final long serialVersionUID = 2487019833065796681L;
	private final String USER;
	private final HashMap<String, Integer> HOSTS = new HashMap<>(); // name of the host --> running jobs on it
	private final int PORT;
	private final boolean STRICT_HOST_CHECKING;
	private final String ORIGINAL_HOST_LIST;
	private String privKeyGUI;

	public RemoteExecutorInfo(String name, boolean isDefault, boolean isStick2Host, Integer maxSlaveRunning, String path2java, int maxRunning, String watchdogBaseDir, Environment environment, String host, String user, int port, boolean strictHostChecking, String workingDir) {
		super(name, isDefault, isStick2Host, maxSlaveRunning, path2java, maxRunning, watchdogBaseDir, environment, workingDir);
		
		// save the additional stuff
		this.USER = user;
		this.ORIGINAL_HOST_LIST = host;
		for(String h : host.split(XMLParser.HOST_SEP)) {
			this.HOSTS.put(h, 0);
		}
		this.PORT = port;
		this.STRICT_HOST_CHECKING = strictHostChecking;
	}
	
	public String getOriginalHostList() {
		return this.ORIGINAL_HOST_LIST;
	}
	
	public String getUser() {
		return this.USER;
	}
	
	public void increaseRunningCounter(String host) {
		if(this.HOSTS.containsKey(host))
			this.HOSTS.put(host, this.HOSTS.get(host) + 1);
	}
	
	public void decreaseRunningCounter(String host) {
		if(this.HOSTS.containsKey(host))
			this.HOSTS.put(host, Math.max(this.HOSTS.get(host) - 1, 0));
	}

	public String getFreeHost() {
		for(String h : this.HOSTS.keySet()) {
			if(this.getMaxSimRunning() < 0 || (this.HOSTS.get(h) < this.getMaxSimRunning()))
				return h;
		}
		return null;
	}
	
	public boolean hasFreeHost() {
		return this.getFreeHost() != null;
	}

	public int getPort() {
		return this.PORT;
	}

	public boolean isStrictHostCheckingEnabled() {
		return this.STRICT_HOST_CHECKING;
	}
	
	@Override
	public boolean isMaxRunningRestrictionReached() {
		return !this.hasFreeHost();
	}

	public void removeHost(String host) {
		this.HOSTS.remove(host);
	}
	
	/**
	 * sets a private key for the GUI
	 * @param privKeyGUI
	 */
	public void setPrivKey(String privKeyGUI) {
		this.privKeyGUI = privKeyGUI;
	}

	/**
	 * returns the path to the private key
	 * @return
	 */
	public String getPrivateKey() {
		return this.privKeyGUI;
	}
	

	@Override
	public String toXML() {
		XMLBuilder x = new XMLBuilder();
		// start with basic tag
		x.startTag(XMLParser.REMOTE, false);
		x.addQuotedAttribute(XMLParser.NAME, this.getName());
		x.addQuotedAttribute(XMLParser.USER, this.getUser());
		x.addQuotedAttribute(XMLParser.HOST, this.getOriginalHostList());
		x.addQuotedAttribute(XMLParser.PRIVATE_KEY, this.getPrivateKey());
				
		// add optional attributes
		if(this.hasDefaultEnv())
			x.addQuotedAttribute(XMLParser.ENVIRONMENT, this.getEnv().getName());
		if(this.isDefaultExecutor())
			x.addQuotedAttribute(XMLParser.DEFAULT, true);
		if(this.getMaxSimRunning() >= 1)
			x.addQuotedAttribute(XMLParser.MAX_RUNNING, this.getMaxSimRunning());
		if(this.getPort() != 22)
			x.addQuotedAttribute(XMLParser.PORT, this.getPort());
		if(!this.isStrictHostCheckingEnabled())
			x.addQuotedAttribute(XMLParser.DISABLE_STRICT_HOST_CHECK, true);
		if(this.isStick2Host())
			x.addQuotedAttribute(XMLParser.STICK2HOST, true);
		
		if(this.hasColor())
			x.addQuotedAttribute(XMLParser.COLOR, this.getColor());
		
		// end the tag
		x.endCurrentTag();
		return x.toString();
	}

}
