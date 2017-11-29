package de.lmu.ifi.bio.watchdog.GUI.properties;

import java.util.HashMap;

import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PropertyViewFactory;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Pair;

public class PropertyLine extends Pane {
	
	private static HashMap<String, PropertyLine> INDEX = new HashMap<>(); 
	private static int COUNTER = 0;
	
	private PropertyLineController controller;
	
	/** hide constructor */
	private PropertyLine() {}

	public static PropertyLine getPropertyLine(PropertyManagerController manager, PropertyViewFactory factory, Integer number, String label, Color color) {
		try {
			FXMLRessourceLoader<PropertyLine, PropertyLineController> l = new FXMLRessourceLoader<>("PropertyLine.fxml", new PropertyLine());
			Pair<PropertyLine, PropertyLineController> p = l.getNodeAndController();
			PropertyLine line = p.getKey();
			line.controller = p.getValue();

			// set properties
			line.controller.setPropertyLine(line);
			line.controller.setManager(manager);
			line.controller.setProperty(factory.getPropertyData(color, number));
			line.controller.setLabel(label);
			line.controller.setViewFactory(factory);
			line.setId("pl_id_" + Integer.toString(COUNTER++));
			
			// save it
			INDEX.put(line.getId(), line);
			
			return line;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null; 
	}
	
	public static PropertyLine getPropertyLine(String nodeID) {
		return INDEX.get(nodeID);
	}
	
	public void config() {
		this.controller.config();
	}
	
	public boolean hasDataStored() {
		return this.controller.hasDataStored();
	}
	
	public XMLDataStore getStoredData() {
		return this.controller.getStoredData();
	}
	
	public PropertyData getPropertyData() {
		return this.controller.getProperty();
	}
	
	public void setStoredData(XMLDataStore data) {
		this.controller.getProperty().setXMLData(data);
	}

	public void setLabel(String label) {
		this.controller.setLabel(label);
	}
 
	public void setDisplayNumber(int number, Color c) {
		this.controller.setDisplayNumber(number, c);
	}

	public void resetCustomLabelAndColor() {
		this.controller.resetCustomLabelAndColor();
	}
}
