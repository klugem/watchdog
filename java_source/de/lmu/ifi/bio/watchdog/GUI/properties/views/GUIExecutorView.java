package de.lmu.ifi.bio.watchdog.GUI.properties.views;

import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import javafx.scene.layout.Pane;
import javafx.util.Pair;

public abstract class GUIExecutorView extends Pane {

	private GUIExecutorViewController controller;
	
	/** hide constructor */ 
	protected GUIExecutorView() {}
	
	public static GUIExecutorView getExecutorPropertyView(String fxmlFile, GUIExecutorView pane) {
		try {
			FXMLRessourceLoader<GUIExecutorView, GUIExecutorViewController> l = new FXMLRessourceLoader<>(fxmlFile, pane);
			Pair<GUIExecutorView, GUIExecutorViewController> pair = l.getNodeAndController();
			GUIExecutorView p = pair.getKey();
			p.controller = pair.getValue();
				
			// set properties on GUI

			return p;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public String toString() {
		return this.getName();
	}
	
	/**
	 * name of that executor
	 * @return
	 */
	public abstract String getName();

	/**
	 * name of the fxml resource file
	 * @return
	 */
	public abstract String getFXMLResourceFilename();

	/**
	 * adds the validate commands to the control using name of executor as condition
	 * @param executorPropertyViewController
	 */
	public void addValidateToControl(ExecutorPropertyViewController executorPropertyViewController) {
		this.controller.executorPropertyViewController(executorPropertyViewController, this.getName());
	}
	
	/**
	 * returns the executor
	 * @return
	 */
	public ExecutorInfo getExecutor(String name, boolean isDefault, boolean isStick2Host, Integer maxSlaveRunning, String path2java, int maxRunning,String watchdogBaseDir, Environment environment, String workingDir) {
		return this.controller.getExecutor(name, isDefault, isStick2Host, maxSlaveRunning, path2java, maxRunning, watchdogBaseDir, environment, workingDir);
	}

	/**
	 * loads executor specific data
	 * @param data
	 */
	public void loadData(Object[] data) {
		this.controller.loadData(data);
	}
}


