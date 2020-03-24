package de.lmu.ifi.bio.watchdog.executor.external.drmaa;

import java.util.ArrayList;
import java.util.HashMap;

import de.lmu.ifi.bio.watchdog.executor.Executor;
import de.lmu.ifi.bio.watchdog.executor.external.ExternalExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.SyncronizedLineWriter;
import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * generic drmaa executor info
 * @author Michael Kluge
 *
 */
public class DRMAAExecutorInfo extends ExternalExecutorInfo {
	
	private static final long serialVersionUID = -8553496121935804071L;
	private static final int RETRY_SUBMIT_COUNT = 10; // try to context the DRM system x times before waiting for new round
		
	private final String CUSTOM_PARAMS;
	
	/**
	 * Constructor
	 * @param name
	 * @param isDefault
	 * @param maxRunning
	 * @param watchdogBaseDir
	 * @param environment
	 */
	public DRMAAExecutorInfo(String type, String name, boolean isDefault, boolean isStick2Host, Integer maxSlaveRunning, String path2java, int maxRunning, String watchdogBaseDir, Environment environment, String shebang, String workingDir, String customParams, ArrayList<String> beforeScripts, ArrayList<String> afterScripts, ArrayList<String> packageManagers, String container) {
		super(type, name, isDefault, isStick2Host, maxSlaveRunning, path2java, maxRunning, watchdogBaseDir, environment, workingDir, shebang, beforeScripts, afterScripts, packageManagers, container);
		this.CUSTOM_PARAMS = customParams;
	}
	
	/**
	 * custom parameters that can be added by the user to be passed to the SGE
	 * @return
	 */
	public String getCustomParameters() {
		return this.CUSTOM_PARAMS;
	}
	
	public boolean hasCustomParametersSet() {
		return this.getCustomParameters() != null && this.getCustomParameters().length() > 0;
	}
	
	/**
	 * gets the command which is set on the grid
	 * @return
	 */
	public String getCommandsForGrid() {
		if(this.hasCustomParametersSet()) {
			return this.getCustomParameters();
		}
		return new String();
	}

	
	@Override
	public String toXML() {
		XMLBuilder x = new XMLBuilder();
		// start with basic tag
		x.startTag(XMLParser.CLUSTER, false);
		x.addQuotedAttribute(XMLParser.NAME, this.getName());

		// add optional attributes
		this.addDefaultExecutorAttributes(x);
		
		// add custom parameters
		if(this.hasCustomParametersSet())
			x.addQuotedAttribute(XMLParser.CUSTOM_PARAMETERS, this.getCustomParameters());
	
		// end the tag
		x.endCurrentTag();
		return x.toString();
	}

	@Override
	public Executor<DRMAAExecutorInfo> getExecutorForTask(Task t, SyncronizedLineWriter logFile) {
		return new DRMAAExecutor(t, logFile, RETRY_SUBMIT_COUNT, this);
	}
	
	@Override
	public Object[] getDataToLoadOnGUI() {
		return new Object[] { this.getCustomParameters() };		
	}
	
	@Override
	public boolean isWatchdogDetachSupported() {
		return true;
	}
	
	@Override
	public HashMap<String, String> getExecutorSpecificEnvironmentVariables() { return new HashMap<String, String>(); }
}
