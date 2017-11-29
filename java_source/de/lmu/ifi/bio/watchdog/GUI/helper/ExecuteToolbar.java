package de.lmu.ifi.bio.watchdog.GUI.helper;

import de.lmu.ifi.bio.watchdog.GUI.WorkflowDesignController;
import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import javafx.scene.layout.Pane;
import javafx.util.Pair;

/**
 * should be constructed using the static method defined in ExecuteToolbar
 * @author kluge
 *
 */
public class ExecuteToolbar extends Pane {

	private ExecuteToolbarController controller;
	
	/** hide constructor */ 
	protected ExecuteToolbar() {}
	
	public static ExecuteToolbar getExecuteToolbarer(WorkflowDesignController controller) {
		try {
			FXMLRessourceLoader<ExecuteToolbar, ExecuteToolbarController> l = new FXMLRessourceLoader<>("ExecuteToolbar.fxml", new ExecuteToolbar());
			Pair<ExecuteToolbar, ExecuteToolbarController> pair = l.getNodeAndController();
			ExecuteToolbar p = pair.getKey();
			p.controller = pair.getValue();
				
			// set properties on GUI
			p.controller.setDesignControlller(controller);
			return p;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void stopWorkflow() {
		this.controller.stopWorkflow();
	}

	public void setIsFinished() {
		this.controller.setIsFinished();
	}
}


