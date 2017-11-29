package de.lmu.ifi.bio.watchdog.GUI.properties;

import java.util.ArrayList;
import java.util.HashMap;

import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PropertyViewType;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Pair;

public class PropertyManager extends Pane {
	
	private static final HashMap<PropertyViewType, PropertyManager> INSTANCES = new HashMap<>(); 
	private PropertyManagerController controller;
	
	/** hide constructor */
	private PropertyManager() {}

	public static PropertyManager getPropertyManager(PropertyViewType type) {
		if(INSTANCES.containsKey(type))
			return INSTANCES.get(type);
		try {
			FXMLRessourceLoader<PropertyManager, PropertyManagerController> l = new FXMLRessourceLoader<>("PropertyManager.fxml", new PropertyManager());
			Pair<PropertyManager, PropertyManagerController> p = l.getNodeAndController();
			PropertyManager manager = p.getKey();
			manager.controller = p.getValue();

			// set properties
			manager.controller.setPropertyViewType(type);
			
			// save the manager for further use
			INSTANCES.put(type, manager);
			return manager;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void clear() {
		this.controller.clear();
	}

	public ArrayList<XMLDataStore> getXMLData() {
		return this.controller.getXMLData();
	}
	
	public PropertyLine loadProperty(Color c, int number, XMLDataStore data2load) {
		return this.controller.loadProperty(c, number, data2load);
	}
	
	public Color getColor(int n) {
		return this.controller.getColor(n);
	}

	public void updateAppendColor(String name, Color p) {
		this.controller.updateAppendColor(name, p);
	}

	public void updateUsedColor(PropertyLine p, int number, Color c) {
		this.controller.updateUsedColor(p, number, c);
	}
	
	public ArrayList<PropertyLine> getDataWithSameName4Append(String name) {
		return this.controller.getDataWithSameName4Append(name);
	}
}
