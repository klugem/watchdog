package de.lmu.ifi.bio.watchdog.GUI.properties.views;

import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.executor.local.LocalExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;

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
	public void executorPropertyViewController(ExecutorPropertyViewController executorPropertyViewController, String condition) {}

	@Override
	public void setHandlerForGUIColoring() {}

	@Override
	public ExecutorInfo getExecutor(String name, boolean isDefault, boolean isStick2Host, Integer maxSlaveRunning, String path2java, int maxRunning,String watchdogBaseDir, Environment environment, String workingDir ) {
		return new LocalExecutorInfo(name, isDefault, isStick2Host, path2java, maxRunning, watchdogBaseDir, environment, workingDir);
	}

	@Override
	public void loadData(Object[] data) {}
}
