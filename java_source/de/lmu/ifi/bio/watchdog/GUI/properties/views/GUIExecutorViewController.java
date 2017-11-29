package de.lmu.ifi.bio.watchdog.GUI.properties.views;

import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TitledPane;

public abstract class GUIExecutorViewController implements Initializable {
	
	@FXML private TitledPane advancedPane;

	@Override
	public void initialize(URL location, ResourceBundle resources) {} 

	/**
	 * adds the checker to the parent controller using condition as condition
	 * @param executorPropertyViewController
	 * @param condition
	 */
	public abstract void executorPropertyViewController(ExecutorPropertyViewController executorPropertyViewController, String condition);
	
	/**
	 * must set be handlers that are required to color the GUI
	 */
	public abstract void setHandlerForGUIColoring();
	
	/**
	 * returns the executor that was saved
	 * @return
	 */
	public abstract ExecutorInfo getExecutor(String name, boolean isDefault, boolean isStick2Host, Integer maxSlaveRunning, String path2java, int maxRunning,String watchdogBaseDir, Environment environment, String workingDir);

	/**
	 * loads executor specific data
	 * @param data
	 */
	public abstract void loadData(Object[] data);
}
