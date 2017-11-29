package de.lmu.ifi.bio.watchdog.GUI.properties.views;

import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import javafx.application.Platform;

public abstract class PropertyViewController extends ValidateViewController {
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);
		Platform.runLater(() -> this.saveButton.getScene().getWindow().setOnHiding(event -> this.registerData()));
	}
	
	private void registerData() {
		if(this.getStoredData() != null)
			XMLDataStore.registerData(this.getStoredData());
	}

	/**
	 * Checks, if a property with that name is already registered
	 * @param text
	 * @return
	 */
	protected abstract boolean hasUniqueName(String text);
	
	protected abstract PropertyViewType getPropertyTypeName();
	
	@Override
	public String getSaveName() {
		return this.getPropertyTypeName().getPropertyName();
	}

}
