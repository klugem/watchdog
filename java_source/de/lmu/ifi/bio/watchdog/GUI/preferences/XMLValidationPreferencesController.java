package de.lmu.ifi.bio.watchdog.GUI.preferences;

import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.helper.PreferencesStore;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

public class XMLValidationPreferencesController extends AbstractPreferencesController {

	@FXML private CheckBox unsafeLoadMode;
	@FXML private CheckBox unsafeSaveMode;
		
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.addValidateToControl(this.unsafeLoadMode, "unsafeLoad", x -> true);
		this.addValidateToControl(this.unsafeSaveMode, "unsafeSave", x -> true);
		this.unsafeLoadMode.selectedProperty().addListener(x -> this.validate());
		this.unsafeSaveMode.selectedProperty().addListener(x -> this.validate());
		super.initialize(location, resources);
	}
		
	@Override
	public void onLoad() {
		this.unsafeLoadMode.setSelected(PreferencesStore.getUnsafeLoadXMLValidationMode());
		this.unsafeSaveMode.setSelected(PreferencesStore.getUnsafeSaveXMLValidationMode());
		super.onLoad();
	}

	@Override
	public void onSave() {
		PreferencesStore.setUnsafeLoadXMLValidationMode(this.unsafeLoadMode.isSelected());
		PreferencesStore.setUnsafeSaveXMLValidationMode(this.unsafeSaveMode.isSelected());
		super.onSave();
	}
}