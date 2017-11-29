package de.lmu.ifi.bio.watchdog.GUI.layout;

import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import javafx.scene.layout.Pane;
import javafx.util.Pair;

public class DependencyProperty extends Pane {
	
	private DependencyPropertyController controller;

	/** hide constructor */
	private DependencyProperty() {}
	
	public static DependencyProperty getModule(Dependency d) {
		try {
			FXMLRessourceLoader<DependencyProperty, DependencyPropertyController> l = new FXMLRessourceLoader<>("DependencyProperty.fxml", new DependencyProperty());
			Pair<DependencyProperty, DependencyPropertyController> p = l.getNodeAndController();
			DependencyProperty m = p.getKey();
			m.controller = p.getValue();
			
			// set properties
			m.controller.setDependency(d);
			return m;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
