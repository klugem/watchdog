package de.lmu.ifi.bio.watchdog.executor.local;

import java.util.ArrayList;
import java.util.HashMap;

import de.lmu.ifi.bio.watchdog.executor.Executor;
import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.SyncronizedLineWriter;
import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Local executor info
 * @author Michael Kluge
 *
 */
public class LocalExecutorInfo extends ExecutorInfo {

	private static final long serialVersionUID = 1985752316536563754L;

	/**
	 * Constructor
	 * @param name
	 * @param isDefault
	 * @param maxRunning
	 * @param watchdogBaseDir
	 * @param environment
	 */
	public LocalExecutorInfo(String type, String name, boolean isDefault, boolean isStick2Host, String path2java, int maxRunning, String watchdogBaseDir, Environment environment, String workingDir, String shebang, ArrayList<String> beforeScripts, ArrayList<String> afterScripts, ArrayList<String> packageManagers, String container) {
		super(type, name, isDefault, isStick2Host, null, path2java, maxRunning, watchdogBaseDir, environment, workingDir, shebang, beforeScripts, afterScripts, packageManagers, container);
	}

	
	@Override
	public String toXML() {
		XMLBuilder x = new XMLBuilder();
		// start with basic tag
		x.startTag(XMLParser.LOCAL, false);
		x.addQuotedAttribute(XMLParser.NAME, this.getName());
		// add default optional attributes
		this.addDefaultExecutorAttributes(x);
		
		// end the tag
		x.endCurrentTag();
		return x.toString();
	}

	@Override
	public Executor<LocalExecutorInfo> getExecutorForTask(Task t, SyncronizedLineWriter logFile) {
		return new LocalExecutor(t, logFile, this);
	}


	@Override
	public Object[] getDataToLoadOnGUI() { return new Object[0]; }
	
	@Override
	public boolean isWatchdogDetachSupported() {
		return false;
	}

	@Override
	public HashMap<String, String> getExecutorSpecificEnvironmentVariables() { return new HashMap<String, String>(); }
}
