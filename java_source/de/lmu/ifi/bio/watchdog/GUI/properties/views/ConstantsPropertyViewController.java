package de.lmu.ifi.bio.watchdog.GUI.properties.views;


import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.MessageType;
import de.lmu.ifi.bio.watchdog.GUI.css.GUIFormat;
import de.lmu.ifi.bio.watchdog.GUI.helper.TextFilter;
import de.lmu.ifi.bio.watchdog.helper.Constants;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class ConstantsPropertyViewController extends PropertyViewController {

	@FXML private TextField name;
	@FXML private TextField value;
	
	private Constants constantsStore;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);
		// add event handler for GUI validation
		this.name.textProperty().addListener(event -> this.validate());
		this.name.setTextFormatter(TextFilter.getAlphaFollowedByAlphaNumber());
		
		// add checker
		this.addValidateToControl(this.name, "name", f -> this.checkName((TextField) f));

		// call it once to get initial coloring
		this.validate();
	}
		
	private boolean checkName(TextField f) {
		boolean ret = true;
		if(this.isEmpty((TextField) f, "Name for constant is missing."))
			ret = false;
		if(!this.hasUniqueName(((TextField) f).getText()))
			ret = false;
		
		GUIFormat.colorTextField(f, ret); // color the stuff correctly
		return ret;
	}

	public Constants getStoredData() {
		return this.constantsStore;
	}

	@Override
	protected void saveData() {
		// create the constant
		Constants c = new Constants(this.name.getText(), this.value.getText());
		// save the constant
		this.storeXMLData(c);
		super.saveData();
	}
	
	protected PropertyViewType getPropertyTypeName() {
		return PropertyViewType.CONSTANTS;
	}
	
	private void storeXMLData(Constants data) {
		this.constantsStore = data;
		XMLDataStore.registerData(data);
	}

	public void loadData(Constants data) {
		if(data != null) {
			// unregister that data or otherwise name will be blocked!
			XMLDataStore.unregisterData(data);
			this.isDataLoaded = true;

			// set basic settings
			this.name.setText(data.getName());
			this.value.setText(data.getValue());
			
			// load the data
			this.constantsStore = data;
			this.isDataLoaded = false;
		}
	}

	@Override
	protected boolean hasUniqueName(String name) {
		if(!XMLDataStore.hasRegistedData(name, Constants.class))
			return true;
		else {
			this.addMessageToPrivateLog(MessageType.ERROR, "An constant with name '"+name+"' exists already.");
			return false;
		}
	}
}
