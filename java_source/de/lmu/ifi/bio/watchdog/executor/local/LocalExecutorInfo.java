package de.lmu.ifi.bio.watchdog.executor.local;

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
	public LocalExecutorInfo(String type, String name, boolean isDefault, boolean isStick2Host, String path2java, int maxRunning, String watchdogBaseDir, Environment environment, String workingDir) {
		super(type, name, isDefault, isStick2Host, null, path2java, maxRunning, watchdogBaseDir, environment, workingDir);
	}

	
	@Override
	public String toXML() {
		XMLBuilder x = new XMLBuilder();
		// start with basic tag
		x.startTag(XMLParser.LOCAL, false);
		x.addQuotedAttribute(XMLParser.NAME, this.getName());
		
		// add optional attributes
		if(this.hasDefaultEnv())
			x.addQuotedAttribute(XMLParser.ENVIRONMENT, this.getEnv().getName());
		if(this.isDefaultExecutor())
			x.addQuotedAttribute(XMLParser.DEFAULT, true);
		if(this.getMaxSimRunning() >= 1)
			x.addQuotedAttribute(XMLParser.MAX_RUNNING, this.getMaxSimRunning());
		if(this.isStick2Host())
			x.addQuotedAttribute(XMLParser.STICK2HOST, true);
		if(this.hasColor())
			x.addQuotedAttribute(XMLParser.COLOR, this.getColor());
		
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
}
