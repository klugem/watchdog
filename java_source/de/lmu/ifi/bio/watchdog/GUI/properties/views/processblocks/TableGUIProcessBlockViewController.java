package de.lmu.ifi.bio.watchdog.GUI.properties.views.processblocks;

import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.helper.SuggestPopup;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginPropertyViewController;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginViewController;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessTable;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class TableGUIProcessBlockViewController extends PluginViewController<ProcessTable>  {
	
	@FXML private TextField path;
	@FXML private TextField compare;
	@FXML private CheckBox enforce;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);
	}

	@Override
	public void addPropertyViewControllerToValidate(PluginPropertyViewController<ProcessTable> propertyViewController, String condition) {		
		// add checker
		propertyViewController.addValidateToControl(this.path, "path", f -> propertyViewController.isAbsoluteFile((TextField) f, "A path to a file in csv-format must be given. (f.e *.txt)"), condition);
		
		// add event handler for GUI validation
		this.path.textProperty().addListener(event -> propertyViewController.validate());
		
		// add suggest constants support
		@SuppressWarnings("unused") SuggestPopup p1 = new SuggestPopup(this.path);
	}

	@Override
	public void setHandlerForGUIColoring() {}

	@SuppressWarnings("unused")
	@Override
	public ProcessTable getXMLPluginObject(Object[] data) {
		// cast the data
		String name = (String) data[0];
		boolean append = (boolean) data[1];
		return new ProcessTable(name, this.path.getText(), null, this.compare.getText(), !this.enforce.isSelected());
	}

	@Override
	public void loadData(Object[] data) {
		this.path.setText((String) data[0]);
		this.compare.setText((String) data[1]);
		this.enforce.setSelected((boolean) data[2]);
	}
}
