package de.lmu.ifi.bio.watchdog.GUI.datastructure;

import javafx.scene.control.Button;

public class Blacklist extends DeleteButton {
	public final String PATTERN;
	
	public Blacklist(String pattern, Button b) {
		super(b);
		this.PATTERN = pattern;
	}
	
	public String getPattern() {
		return this.PATTERN;
	}
	
	@Override
	public String toString() {
		return this.getPattern();
	}
}
