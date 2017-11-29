package de.lmu.ifi.bio.watchdog.GUI.properties;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import javafx.scene.paint.Color;

public class PropertyData implements Serializable {
	
	private static final long serialVersionUID = 2979285522256119744L;
	private String colorCode = Color.TRANSPARENT.toString();
	private Integer number = null;
	private final int ID;
	private XMLDataStore data;
	private boolean hide = false;
	private HashSet<Property> GUI_PROPERTIES = new HashSet<>();
	private static HashMap<Integer, PropertyData> INSTANCES = new HashMap<>();
	private static int internalID = 0;
	
	/**
	 * property data rectangle
	 */
	public PropertyData(Integer number) {
		this.hide = true;
		this.number = number;
		this.ID = internalID++;
		
		INSTANCES.put(this.ID, this);
	}
	
	public PropertyData(Color color, Integer number) {
		this.colorCode = color.toString();
		this.number = number;
		this.ID = internalID++;
		
		INSTANCES.put(this.ID, this);
	}
	
	public int getID() {
		return this.ID;
	}
	
	public static boolean hasPropertyData(int id) {
		return INSTANCES.containsKey(id);
	}
	
	public static PropertyData getPropertyData(int id) {
		return INSTANCES.get(id);
	}
	
	public Color getColor() {
		return Color.valueOf(this.colorCode);
	}

	public Integer getNumber() {
		return this.number;
	}
	
	public XMLDataStore getXMLData() {
		return this.data;
	}

	public void setColor(Color color, boolean internalUpdateCall) {
		this.colorCode = color.toString();
		if(this.hasXMLData())
			this.getXMLData().setColor(this.colorCode);
		if(!internalUpdateCall)
			this.updateGUI();
	}

	public void setNumber(Integer number, boolean internalUpdateCall) {
		this.number = number;
		if(!internalUpdateCall) {
			this.updateGUI();
		}
	}
	
	public boolean hasXMLData() {
		return this.data != null;
	}
	
	public void setXMLData(XMLDataStore data) {
		this.data = data;
		if(this.hasXMLData())
			this.setColor(this.getColor(), true);
		this.updateGUI();
	}
	
	public void registerGUIElement(Property p) {
		this.GUI_PROPERTIES.add(p);
	}
	
	public void unregisterGUIElement(Property p) {
		this.GUI_PROPERTIES.remove(p);
	}
	
	private void updateGUI() {
		for(Property p : this.GUI_PROPERTIES)
			p.setPropertyData(this, true);
	}

	public boolean isHidden() {
		return this.hide;
	}

	public void setDisplayNumber(Integer number, Color c) {
		for(Property p : this.GUI_PROPERTIES)
			p.setDisplayNumber(number, c);
	}
}
