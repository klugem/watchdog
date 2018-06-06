package de.lmu.ifi.bio.watchdog.GUI.preferences;

import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.utils.interfaces.EventDistributor;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.ValidateViewController;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import javafx.event.Event;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

public abstract class AbstractPreferencesController extends ValidateViewController {
	
	private BorderPane parentPane;
	private EventDistributor eventDistributor;
	
	public AbstractPreferencesController() {
		this.saveButton = new Button(); // dummy save button
	}
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.onLoad();
	}
	
	/**
	 * is called when the user chooses to save all the stuff
	 */
	public void onSave() {
		this.saveLastSavedData4Validate();
	}
	
	/**
	 * is called when GUI is loaded or the user resets all changes
	 */
	public void onLoad() {
		this.saveLastSavedData4Validate();
	}
	
	@Override
	public String getSaveName() {
		return null;
	}

	@Override
	public XMLDataStore getStoredData() {
		return null;
	}
	
	public BorderPane getParentToSendEvents() {
		return this.parentPane;
	}
	
	public void setParentPaneForEvents(BorderPane parent) {
		this.parentPane = parent;
	}

	public void checkSettingsWhenNoDataIsThere() {
		this.onLoad();
	}

	public void setEventDistributorForEvents(EventDistributor ed) {
		this.eventDistributor = ed;
	}
		
	public boolean sendEventToSiblingPages(Event e) {
		if(this.eventDistributor == null)
			return false;
		this.eventDistributor.distribute(e);
		return true;
	}

	/**
	 * can be overwritten
	 * @param e
	 */
	public void recieveEventFromSiblingPages(Event e) {}
}
