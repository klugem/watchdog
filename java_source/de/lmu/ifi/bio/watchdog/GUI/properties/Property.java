package de.lmu.ifi.bio.watchdog.GUI.properties;

import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Pair;

public class Property extends Pane {
	
	private PropertyController controller;
	private PropertyData data;
	private boolean isInPropertyToolbar;

	/** hide constructor */
	private Property() {}

	public static Property getProperty(PropertyLine parent, Color c, Integer n, boolean isInPropertyToolbar) {
		try {
			FXMLRessourceLoader<Property, PropertyController> l = new FXMLRessourceLoader<>("Property.fxml", new Property());
			Pair<Property, PropertyController> pair = l.getNodeAndController();
			Property p = pair.getKey();
			p.controller = pair.getValue();
			p.isInPropertyToolbar = isInPropertyToolbar;
			
			if(c != null && !c.equals(Color.TRANSPARENT))
				p.data = new PropertyData(c, n);
				
			// set properties on GUI
			p.setPropertyData(p.data, parent);
			return p;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void setParentLine(PropertyLine parent) {
		this.controller.setParentLine(parent);
	}
	
	public PropertyLine getParentLine() {
		return this.controller.getParentLine();
	}

	public void setPropertyData(PropertyData data, PropertyLine parent) {
		this.setParentLine(parent);
		this.setPropertyData(data, false);
	}
	
	public void setPropertyData(PropertyData data, boolean internalUpdateCall) {
		boolean newElementregister = data != this.data;
		// unregister the element at the old data binding
		if(newElementregister && this.data != null) this.data.unregisterGUIElement(this);
		
		this.data = data;
		if(newElementregister || data == null || !internalUpdateCall) // update only if required
			this.controller.setProperty(this.data, internalUpdateCall);
		
		// create only tooltips for properties displayed in workflow
		if(!this.isInPropertyToolbar) {
			// remove old tooltip
			if(this.getUserData() != null && this.getUserData() instanceof Tooltip)
				Tooltip.uninstall(this, (Tooltip) this.getUserData());
			
			// install Tooltip
			if(this.hasPropertyData() && this.data.hasXMLData()) {
				Tooltip t = new Tooltip(this.data.getXMLData().getName() + " (" + this.data.getXMLData().getClass().getSimpleName().replaceAll("([A-Z])", " $1").toLowerCase().replaceFirst("^ ", "") + ")");
				this.setUserData(t);
				Tooltip.install(this, t);
			}
		}
		
		if(this.data != null) this.controller.hide(this.data.isHidden());
		
		// register it with new data binding
		if(newElementregister && this.data != null) this.data.registerGUIElement(this);
	}

	public PropertyData getPropertyData() {
		return this.data;
	}
	
	public boolean hasPropertyData() {
		return this.data != null;
	}

	public void setDisplayNumber(Integer number, Color c) {
		this.controller.setDisplayNumber(number, c);
	}
}
