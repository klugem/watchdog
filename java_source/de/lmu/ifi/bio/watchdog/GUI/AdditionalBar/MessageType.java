package de.lmu.ifi.bio.watchdog.GUI.AdditionalBar;

import javafx.scene.paint.Color;

public enum MessageType {
	INFO("Info"), WARNING("Warning"), ERROR("Error"), DEBUG("Debug");
	
	private final String NAME;
	
	private MessageType(String name) {
		this.NAME = name;
	}
	
	@Override
	public String toString() {
		return this.NAME + ":";
	}
	
	public Color getColor() {
		if(INFO.equals(this))
			return Color.hsb(306, 0.007, 0.13);
		else if(WARNING.equals(this))
			return Color.DARKORANGE;
		else if(DEBUG.equals(this))
			return Color.DARKMAGENTA;
		else
			return Color.DARKRED;
	}
}
