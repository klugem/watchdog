package de.lmu.ifi.bio.watchdog.GUI.datastructure;

import de.lmu.ifi.bio.watchdog.GUI.interfaces.ListLibraryView;

/**
 * Holds data about the available categorys for the module lib
 * @author kluge
 *
 */
public class Category implements ListLibraryView {
	
	private final String NAME;
	
	public Category(String name) {
		this.NAME = name;
	}

	@Override
	public String getNameForDisplay() {
		return this.NAME;
	}
}
