package de.lmu.ifi.bio.watchdog.GUI.helper;

import de.lmu.ifi.bio.utils.interfaces.EventDistributor;
import de.lmu.ifi.bio.watchdog.GUI.preferences.AbstractPreferencesController;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

public class SaveablePane extends Pane {

	private AbstractPreferencesController controller;
	
	public void setSaveController(AbstractPreferencesController value) {
		this.controller = value;
	}
	
	/**
	 * call the load method of the controller
	 */
	public void load() {
		if(this.controller != null)
			this.controller.onLoad();
	}
	
	/**
	 * call the save method of the controller
	 */
	public void save() {
		if(this.controller != null)
			this.controller.onSave();
	}
	
	public SimpleBooleanProperty getValidatedDataBoolean() {
		return this.controller.getValidatedDataBoolean();
	}
	
	public SimpleBooleanProperty getUnsavedDataBoolean() {
		return this.controller.getUnsavedDataBoolean();
	}
	
	public BorderPane getParentToSendEvents() {
		return this.controller.getParentToSendEvents();
	}

	public void setParentPaneForEvents(BorderPane parent) {
		this.controller.setParentPaneForEvents(parent);
	}

	public void setEventDistributorForEvents(EventDistributor ed) {
		this.controller.setEventDistributorForEvents(ed);
	}
	
	public void recieveEventFromSiblingPages(Event e) {
		this.controller.recieveEventFromSiblingPages(e);
	}
}
