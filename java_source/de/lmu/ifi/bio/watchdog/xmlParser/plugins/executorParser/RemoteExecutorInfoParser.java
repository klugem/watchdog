package de.lmu.ifi.bio.watchdog.xmlParser.plugins.executorParser;

import java.io.File;

import org.w3c.dom.Element;

import de.lmu.ifi.bio.watchdog.GUI.properties.views.executor.ExecutorPropertyViewController;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.executor.RemoteGUIExecutorView;
import de.lmu.ifi.bio.watchdog.executor.remote.RemoteExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.helper.SSHPassphraseAuth;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

public class RemoteExecutorInfoParser extends XMLExecutorInfoParser<RemoteExecutorInfo> {
	private static final String XSD_DEF = "plugins" + File.separator + "executor.remote.xsd";
	
	static {
		// register the executor plugins shipped with watchdog on GUI
		if(Functions.hasJavaFXInstalled()) {
			ExecutorPropertyViewController.registerWatchdogPluginOnGUI(RemoteExecutorInfo.class, RemoteGUIExecutorView.class);
		}
	}
	
	public RemoteExecutorInfoParser(Logger l) {
		super(l);
	}

	@Override
	public String getNameOfParseableTag() {
		return XMLParser.REMOTE;
	}

	@Override
	public RemoteExecutorInfo parseElement(Element el, String watchdogBaseDir, Object[] additionalData) {
		DefaultExecutorInfo di = this.parseMandatoryParameter(el, watchdogBaseDir, additionalData);
		
		// get additional ssh attributes
		final int port = Integer.parseInt(XMLParser.getAttribute(el, XMLParser.PORT));
		final String host = XMLParser.getAttribute(el, XMLParser.HOST);
		final String user = XMLParser.getAttribute(el, XMLParser.USER);
		final String privKey = XMLParser.getAttribute(el, XMLParser.PRIVATE_KEY);
		final boolean disableStrictHostCheck = Boolean.parseBoolean(XMLParser.getAttribute(el, XMLParser.DISABLE_STRICT_HOST_CHECK));
		SSHPassphraseAuth authStore = new SSHPassphraseAuth(di.getName(), privKey, this.no_exit);
		RemoteExecutorInfo info = new RemoteExecutorInfo(this.getNameOfParseableTag(), di.getName(), di.isDefaultExecutor(), di.isStick2Host(), di.getMaxSlaveRunningTasks(), di.getPath2Java(), di.getMaxSimRunning(), di.getWatchdogBaseDir(), di.getEnv(), di.getShebang(), host, user, port, !disableStrictHostCheck, di.getWorkingDir(), authStore, di.getBeforeScriptNames(), di.getAfterScriptNames());
				
		if(di.getColor() != null)
			info.setColor(di.getColor());
		return info;
	}

	@Override
	public String getXSDDefinition() {
		return XSD_DEF;
	}

	@Override
	public void runAdditionalTestsOnElement(String name) {
		this.PARSED_INFO.get(name).testRemoteCredentials(this.GUI_load_attempt, this.no_exit, this.LOGGER);
	}
}
