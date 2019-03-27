package de.lmu.ifi.bio.watchdog.executor.remote;

import java.util.ArrayList;
import java.util.HashMap;

import de.lmu.ifi.bio.watchdog.executor.Executor;
import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.SSHPassphraseAuth;
import de.lmu.ifi.bio.watchdog.helper.SyncronizedLineWriter;
import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.task.Task;
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
	private SSHPassphraseAuth AUTH;

	public RemoteExecutorInfo(String type, String name, boolean isDefault, boolean isStick2Host, Integer maxSlaveRunning, String path2java, int maxRunning, String watchdogBaseDir, Environment environment, String shebang, String host, String user, int port, boolean strictHostChecking, String workingDir, SSHPassphraseAuth auth) {
		super(type, name, isDefault, isStick2Host, maxSlaveRunning, path2java, maxRunning, watchdogBaseDir, environment, workingDir, shebang);
		
		// save the additional stuff
		this.USER = user;
		this.ORIGINAL_HOST_LIST = host;
		for(String h : host.split(XMLParser.HOST_SEP)) {
			this.HOSTS.put(h, 0);
		}
		this.PORT = port;
		this.STRICT_HOST_CHECKING = strictHostChecking;
		this.AUTH = auth;
	}
	
	@Override
	public Executor<RemoteExecutorInfo> getExecutorForTask(Task t, SyncronizedLineWriter logFile) {
		return new RemoteExecutor(t, logFile, this);
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
	
	public SSHPassphraseAuth getAuth() {
		return this.AUTH;
	}
	
	public void testRemoteCredentials(boolean guiLoadAttempt, boolean noExit, Logger logger) {
		if(guiLoadAttempt || noExit)
			noExit = true;		
		if(!guiLoadAttempt) {
			int succ = 0;
			this.AUTH.testAuthFile(noExit);
			ArrayList<String> hosts = new ArrayList<>();
			hosts.addAll(this.HOSTS.keySet());
			for(String h : hosts) {
				if(this.AUTH.testCredentials(this.USER, h, this.PORT, this.STRICT_HOST_CHECKING)) {
					succ++;
					if(logger != null)
						logger.info("Testing of remote connection to host '" + h + "' with user '" + this.USER + "' on port '" + this.PORT + "' and the given private auth key succeeded.");
				}
				// remove host, if connection can not established
				else {
					this.HOSTS.remove(h);
				}
			}
			// test, if at least one host was reachable
			if(succ == 0) {
				if(logger != null)
					logger.error("No remote host accepted a connection for executor with name '"+this.getName()+"'!");
				if(!noExit) System.exit(1);
			}
		}
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
	 * returns the path to the private key
	 * @return
	 */
	public String getPrivateKey() {
		return this.AUTH.getAuthFile();
	}
	

	@Override
	public String toXML() {
		XMLBuilder x = new XMLBuilder();
		// start with basic tag
		x.startTag(XMLParser.REMOTE, false);
		x.addQuotedAttribute(XMLParser.NAME, this.getName());
		x.addQuotedAttribute(XMLParser.USER, this.getUser());
		x.addQuotedAttribute(XMLParser.HOST, this.getOriginalHostList());
		String authFile = this.getPrivateKey();
		x.addQuotedAttribute(XMLParser.PRIVATE_KEY, authFile);
		authFile = null;
		System.gc();
				
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
		if(this.hasCustomShebang()) 
			x.addQuotedAttribute(XMLParser.SHEBANG, this.getShebang());
		
		if(this.hasColor())
			x.addQuotedAttribute(XMLParser.COLOR, this.getColor());
		
		// end the tag
		x.endCurrentTag();
		return x.toString();
	}

	@Override
	public Object[] getDataToLoadOnGUI() {
		return new Object[] { this.getOriginalHostList(), this.getUser(), this.getPrivateKey(), this.getPort(), !this.isStrictHostCheckingEnabled() };
	}
	
	@Override
	public boolean isWatchdogDetachSupported() {
		return false;
	}
	
	@Override
	public HashMap<String, String> getExecutorSpecificEnvironmentVariables() { return new HashMap<String, String>(); }
}