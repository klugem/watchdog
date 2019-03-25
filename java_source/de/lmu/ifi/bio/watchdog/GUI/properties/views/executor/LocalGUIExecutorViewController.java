package de.lmu.ifi.bio.watchdog.GUI.properties.views.executor;

import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginPropertyViewController;
import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.executor.local.LocalExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Local executor has no additional settings --> nothing to do here
 * @author kluge
 *
 */
public class LocalGUIExecutorViewController extends GUIExecutorViewController {

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);
	}

	@Override
	public void addPropertyViewControllerToValidate(PluginPropertyViewController<ExecutorInfo> executorPropertyViewController, String condition) {}

	@Override
	public void setHandlerForGUIColoring() {}

	@Override
	public void loadData(Object[] data) {}

	@SuppressWarnings("unused")
	@Override
	public ExecutorInfo getXMLPluginObject(Object[] data) {
		// cast the data
		String name = (String) data[0];
		boolean isDefault = (boolean) data[1];
		boolean isStick2Host = (boolean) data[2];
		Integer maxSlaveRunning = (Integer) data[3];
		String path2java = (String) data[4];
		int maxRunning = (int) data[5];
		String watchdogBaseDir = (String) data[6];
		Environment environment = (Environment) data[7];
		String workingDir = (String) data[8];
		String shebang = (String) data[9];
		
		return new LocalExecutorInfo(XMLParser.LOCAL, name, isDefault, isStick2Host, path2java, maxRunning, watchdogBaseDir, environment, workingDir, shebang);
	}
}
