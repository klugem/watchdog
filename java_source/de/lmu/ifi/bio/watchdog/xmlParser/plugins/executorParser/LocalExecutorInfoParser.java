package de.lmu.ifi.bio.watchdog.xmlParser.plugins.executorParser;

import java.io.File;

import org.w3c.dom.Element;

import de.lmu.ifi.bio.watchdog.GUI.properties.views.executor.ExecutorPropertyViewController;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.executor.LocalGUIExecutorView;
import de.lmu.ifi.bio.watchdog.executor.local.LocalExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

public class LocalExecutorInfoParser extends XMLExecutorInfoParser<LocalExecutorInfo> {
	private static final String XSD_DEF = "plugins" + File.separator + "executor.local.xsd";
	
	static {
		// register the executor plugins shipped with watchdog on GUI
		if(Functions.hasJavaFXInstalled()) {
			ExecutorPropertyViewController.registerWatchdogPluginOnGUI(LocalExecutorInfo.class, LocalGUIExecutorView.class);
		}
	}

	public LocalExecutorInfoParser(Logger l) {
		super(l);
	}

	@Override
	public String getNameOfParseableTag() {
		return XMLParser.LOCAL;
	}

	@Override
	public LocalExecutorInfo parseElement(Element el, String watchdogBaseDir, Object[] additionalData) {
		DefaultExecutorInfo di = this.parseMandatoryParameter(el, watchdogBaseDir, additionalData);
		// disable stick2host on local executor by default
		LocalExecutorInfo info =  new LocalExecutorInfo(this.getNameOfParseableTag(), di.getName(), di.isDefaultExecutor(), false, null, di.getMaxSimRunning(), di.getWatchdogBaseDir(), di.getEnv(), di.getWorkingDir(), di.getShebang());
		
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
