package de.lmu.ifi.bio.watchdog.GUI.properties.views.wrapper;

import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.helper.SuggestPopup;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginPropertyViewController;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginViewController;
import de.lmu.ifi.bio.watchdog.executionWrapper.packageManager.CondaExecutionWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class GUICondaExecutionWrapperViewController extends PluginViewController<CondaExecutionWrapper> {
	
	@FXML private TextField path2conda;
	@FXML private TextField path2environments;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);
	}

	@Override
	public void addPropertyViewControllerToValidate(PluginPropertyViewController<CondaExecutionWrapper> propertyViewController, String condition) {
		// add checker
		propertyViewController.addValidateToControl(this.path2conda, "conda binary path", f -> propertyViewController.isAbsoluteFile((TextField) f, "An absolute path to a conda binary must be given."), condition);
		propertyViewController.addValidateToControl(this.path2environments, "conda environment path", f -> propertyViewController.isAbsoluteOrRelativeFolder((TextField) f, "An absolute or relative path where conda environments should be installed must be given.", true), condition);

		// add event handler for GUI validation
		this.path2conda.textProperty().addListener(event -> propertyViewController.validate());
		this.path2environments.textProperty().addListener(event -> propertyViewController.validate());
		
		// add suggest constants support
		@SuppressWarnings("unused") SuggestPopup p1 = new SuggestPopup(this.path2conda);
		@SuppressWarnings("unused") SuggestPopup p2 = new SuggestPopup(this.path2environments);
	}

	@Override
	public void setHandlerForGUIColoring() {}

	@Override
	public CondaExecutionWrapper getXMLPluginObject(Object[] data) {
		// cast the data
		String name = (String) data[0];
		return new CondaExecutionWrapper(name, null, this.path2conda.getText(), this.path2environments.getText());
	}

	@Override
	public void loadData(Object[] data) {
		this.path2conda.setText((String) data[0]);
		this.path2environments.setText((String) data[1]);
	}
}
