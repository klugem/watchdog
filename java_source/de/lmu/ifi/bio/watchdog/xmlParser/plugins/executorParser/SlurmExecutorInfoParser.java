package de.lmu.ifi.bio.watchdog.xmlParser.plugins.executorParser;

import java.io.File;

import org.w3c.dom.Element;

import de.lmu.ifi.bio.watchdog.GUI.properties.views.executor.ExecutorPropertyViewController;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.executor.SlurmGUIExecutorView;
import de.lmu.ifi.bio.watchdog.executor.external.slurm.SlurmExecutorInfo;
import de.lmu.ifi.bio.watchdog.executor.external.slurm.SlurmMonitorThread;
import de.lmu.ifi.bio.watchdog.executor.external.slurm.SlurmWorkloadManagerConnector;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

public class SlurmExecutorInfoParser extends XMLExecutorInfoParser<SlurmExecutorInfo> {
	
	private static final String XSD_DEF = "plugins" + File.separator + "executor.slurm.xsd";
	
	static {		
		// set monitor thread on Executor
		SlurmMonitorThread.updateMonitorThread();
		
		// register the executor plugins shipped with watchdog on GUI
		if(Functions.hasJavaFXInstalled()) {
			ExecutorPropertyViewController.registerWatchdogPluginOnGUI(SlurmExecutorInfo.class, SlurmGUIExecutorView.class);
		}
	}
	
	public SlurmExecutorInfoParser(Logger l) {
		super(l);
	}

	@Override
	public String getNameOfParseableTag() {
		return SlurmWorkloadManagerConnector.EXECUTOR_NAME;
	}
	
	@Override
	public SlurmExecutorInfo parseElement(Element el, String watchdogBaseDir, Object[] additionalData) {		
		DefaultExecutorInfo di = this.parseMandatoryParameter(el, watchdogBaseDir, additionalData);
		
		// get additional slurm attributes
		int cpu = Integer.parseInt(XMLParser.getAttribute(el, XMLParser.CPU));
		String memory = XMLParser.getAttribute(el, XMLParser.MEMORY);
		String cluster = XMLParser.getAttribute(el, XMLParser.CLUSTER);
		String partition = XMLParser.getAttribute(el, XMLParser.PARTITION);
		String timelimit = XMLParser.getAttribute(el, XMLParser.TIMELIMIT);
		String customParams = XMLParser.getAttribute(el, XMLParser.CUSTOM_PARAMETERS); 
		boolean disableDefault = Boolean.parseBoolean(XMLParser.getAttribute(el, XMLParser.DISABLE_DEFAULT));
		
		// bound CPU to 1
		if(cpu <= 0) 
			cpu = 1; 
	
		SlurmExecutorInfo info = new SlurmExecutorInfo(this.getNameOfParseableTag(), di.getName(), di.isDefaultExecutor(), di.isStick2Host(), di.getMaxSlaveRunningTasks(), di.getPath2Java(), di.getMaxSimRunning(), di.getWatchdogBaseDir(), di.getEnv(), di.getShebang(), cpu, memory, cluster, partition, timelimit, di.getWorkingDir(), customParams, disableDefault, di.getBeforeScriptNames(), di.getAfterScriptNames());
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