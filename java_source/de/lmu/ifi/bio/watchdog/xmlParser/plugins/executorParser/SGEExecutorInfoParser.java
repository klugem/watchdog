package de.lmu.ifi.bio.watchdog.xmlParser.plugins.executorParser;

import java.io.File;

import org.w3c.dom.Element;

import de.lmu.ifi.bio.watchdog.GUI.properties.views.executor.ExecutorPropertyViewController;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.executor.SGEGUIExecutorView;
import de.lmu.ifi.bio.watchdog.executor.external.sge.SGEExecutorInfo;
import de.lmu.ifi.bio.watchdog.executor.external.sge.SGEMonitorThread;
import de.lmu.ifi.bio.watchdog.executor.external.sge.SGEWorkloadManagerConnector;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

public class SGEExecutorInfoParser extends XMLExecutorInfoParser<SGEExecutorInfo> {
	
	private static final String XSD_DEF = "plugins" + File.separator + "executor.sge.xsd";
	private boolean firstSge = false;
	
	static {	
		// set monitor thread on Executor
		SGEMonitorThread.updateMonitorThread();
		
		// register the executor plugins shipped with watchdog on GUI
		if(Functions.hasJavaFXInstalled()) {
			ExecutorPropertyViewController.registerWatchdogPluginOnGUI(SGEExecutorInfo.class, SGEGUIExecutorView.class);
		}
	}
	
	public SGEExecutorInfoParser(Logger l) {
		super(l);
	}

	@Override
	public String getNameOfParseableTag() {
		return SGEWorkloadManagerConnector.EXECUTOR_NAME;
	}
	
	@Override
	public SGEExecutorInfo parseElement(Element el, String watchdogBaseDir, Object[] additionalData) {		
		DefaultExecutorInfo di = this.parseMandatoryParameter(el, watchdogBaseDir, additionalData);
		
		// get additional grid attributes
		int slots = Integer.parseInt(XMLParser.getAttribute(el, XMLParser.SLOTS));
		String memory = XMLParser.getAttribute(el, XMLParser.MEMORY);
		String queue = XMLParser.getAttribute(el, XMLParser.QUEUE);
		String customParams = XMLParser.getAttribute(el, XMLParser.CUSTOM_PARAMETERS); 
		boolean disableDefault = Boolean.parseBoolean(XMLParser.getAttribute(el, XMLParser.DISABLE_DEFAULT));
		
		// bound slots to 1
		if(slots <= 0) 
			slots = 1; 
	
		SGEExecutorInfo info = new SGEExecutorInfo(this.getNameOfParseableTag(), di.getName(), di.isDefaultExecutor(), di.isStick2Host(), di.getMaxSlaveRunningTasks(), di.getPath2Java(), di.getMaxSimRunning(), di.getWatchdogBaseDir(), di.getEnv(), di.getShebang(), slots, memory, queue, di.getWorkingDir(), customParams, disableDefault, di.getBeforeScriptNames(), di.getAfterScriptNames());
		if(di.getColor() != null)
			info.setColor(di.getColor());
		return info;
	}

	@Override
	public String getXSDDefinition() {
		return XSD_DEF;
	}

	@Override
	public void runAdditionalTestsOnElement(String name) {}
}