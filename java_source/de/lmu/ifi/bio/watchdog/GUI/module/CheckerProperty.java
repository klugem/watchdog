package de.lmu.ifi.bio.watchdog.GUI.module;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.StatusConsole;
import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import de.lmu.ifi.bio.watchdog.helper.ErrorCheckerStore;
import javafx.scene.layout.Pane;

public class CheckerProperty extends Pane {
	
	private CheckerPropertyController controller;
	
	/** hide constructor */
	private CheckerProperty() {}
	
	public ErrorCheckerStore getStoredData() {
		return (ErrorCheckerStore) this.controller.getStoredData();
	}
	
	public void loadData(ErrorCheckerStore data) {
		this.controller.loadData(data);
	}

	public static CheckerProperty getNewChecker(WorkflowModuleController module, StatusConsole console) {
		try {
			FXMLRessourceLoader<CheckerProperty, CheckerPropertyController> l = new FXMLRessourceLoader<>("CheckerProperty.fxml", new CheckerProperty());
			Pair<CheckerProperty, CheckerPropertyController> p = l.getNodeAndController();
			CheckerProperty m = p.getKey();
			m.controller = p.getValue();

			// set properties
			m.controller.setStatusConsole(console);
			m.controller.setWorkflowModule(module);
			return m;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
