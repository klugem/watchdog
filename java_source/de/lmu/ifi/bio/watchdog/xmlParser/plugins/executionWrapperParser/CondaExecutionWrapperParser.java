package de.lmu.ifi.bio.watchdog.xmlParser.plugins.executionWrapperParser;

import java.io.File;

import org.w3c.dom.Element;

import de.lmu.ifi.bio.watchdog.GUI.properties.views.executor.ExecutorPropertyViewController;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.wrapper.GUICondaExecutionWrapperView;
import de.lmu.ifi.bio.watchdog.executionWrapper.packageManager.CondaExecutionWrapper;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Execution wrapper that is based on the package manager Conda
 * @author kluge
 *
 */
public class CondaExecutionWrapperParser extends XMLExecutionWrapperParser<CondaExecutionWrapper> {
	private static final String XSD_DEF = "plugins" + File.separator + "wrapper.conda.xsd";
	public static final String CONDA = "conda";
	
	public static final String PATH2CONDA = "path2conda";
	public static final String PATH2ENV = "path2environments";
	public static final String DEFAULT_CONDA_ENV_NAME = "conda_watchdog_env";
	
	static {
		// register the execution wrapper plugins shipped with watchdog on GUI
		if(Functions.hasJavaFXInstalled()) {
			ExecutorPropertyViewController.registerWatchdogPluginOnGUI(CondaExecutionWrapper.class, GUICondaExecutionWrapperView.class);
		}
	}

	public CondaExecutionWrapperParser(Logger l) {
		super(l);
	}

	@Override
	public String getNameOfParseableTag() {
		return CONDA;
	}

	@Override
	public CondaExecutionWrapper parseElement(Element el, String watchdogBaseDir, Object[] additionalData) {
		String name = XMLParser.getAttribute(el, XMLParser.NAME);
		String path = XMLParser.getAttribute(el, PATH2CONDA);
		String evnPath = XMLParser.getAttribute(el, PATH2ENV);

		// set default conda env path
		if(evnPath == null || evnPath.length() == 0)
			evnPath = watchdogBaseDir + File.separator + XMLParser.TMP_FOLDER + File.separator + DEFAULT_CONDA_ENV_NAME;
		// relative paths are relative to watchdo's install dir
		else if(!evnPath.startsWith(File.separator))
			evnPath = watchdogBaseDir + File.separator + evnPath;
		
		CondaExecutionWrapper w = new CondaExecutionWrapper(name, watchdogBaseDir, path, evnPath);		
		return w;
	}

	@Override
	public String getXSDDefinition() {
		return XSD_DEF;
	}

	@Override
	public void runAdditionalTestsOnElement(String name) {}
}
