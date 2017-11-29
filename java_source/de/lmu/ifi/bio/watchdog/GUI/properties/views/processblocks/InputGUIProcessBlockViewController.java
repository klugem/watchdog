package de.lmu.ifi.bio.watchdog.GUI.properties.views.processblocks;

import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginPropertyViewController;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginViewController;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessInput;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;


public class InputGUIProcessBlockViewController extends PluginViewController<ProcessInput>  {
	
	@FXML private TextField seperator;
	@FXML private TextField compare;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);
	}

	@Override
	public void addPropertyViewControllerToValidate(PluginPropertyViewController<ProcessInput> propertyViewController, String condition) {
		// add event handler for GUI validation
		this.seperator.textProperty().addListener(event -> propertyViewController.validate());
	}

	@Override
	public void setHandlerForGUIColoring() {}

	@SuppressWarnings("unused")
	@Override
	public ProcessInput getXMLPluginObject(Object[] data) {
		// cast the data
		String name = (String) data[0];
		boolean append = (boolean) data[1];
		return new ProcessInput(name, this.seperator.getText(), this.compare.getText());
	}

	@Override
	public void loadData(Object[] data) {
		this.seperator.setText((String) data[0]);
		this.compare.setText((String) data[1]);
	}
}
