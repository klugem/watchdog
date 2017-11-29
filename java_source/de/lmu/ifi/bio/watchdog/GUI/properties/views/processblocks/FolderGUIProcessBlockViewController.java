package de.lmu.ifi.bio.watchdog.GUI.properties.views.processblocks;

import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.helper.SuggestPopup;
import de.lmu.ifi.bio.watchdog.GUI.helper.TextFilter;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginPropertyViewController;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginViewController;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessFolder;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class FolderGUIProcessBlockViewController extends PluginViewController<ProcessFolder>  {
	
	@FXML private TextField folder;
	@FXML private TextField pattern;
	@FXML private TextField ignore;
	@FXML private CheckBox append;
	@FXML private CheckBox enforce;
	@FXML private TextField maxDepth;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);
	}

	@Override
	public void addPropertyViewControllerToValidate(PluginPropertyViewController<ProcessFolder> propertyViewController, String condition) {
		// add double or integer enforcer
		this.maxDepth.setTextFormatter(TextFilter.getPositiveIntFormater());
		
		// add checker
		propertyViewController.addValidateToControl(this.pattern, "pattern", f -> !propertyViewController.isEmpty((TextField) f, "A file pattern must be given. (f.e *.txt)"), condition);
		propertyViewController.addValidateToControl(this.folder, "folder", f -> propertyViewController.isAbsoluteFolder((TextField) f, "A folder to search for files matching the pattern must be given. (f.e. /tmp/)"), condition);
		
		// add event handler for GUI validation
		this.folder.textProperty().addListener(event -> propertyViewController.validate());
		this.pattern.textProperty().addListener(event -> propertyViewController.validate());
		
		// add suggest constants support
		@SuppressWarnings("unused") SuggestPopup p2 = new SuggestPopup(this.folder);
	}

	@Override
	public void setHandlerForGUIColoring() {}

	@Override
	public ProcessFolder getXMLPluginObject(Object[] data) {
		// cast the data
		String name = (String) data[0];
		boolean append = (boolean) data[1];
		String rootPath = this.folder.getText();
		String pattern = this.pattern.getText();
		String ignore = this.ignore.getText();
		int maxDepth = Integer.parseInt(this.maxDepth.getText());
		boolean disableExistanceCheck = !this.enforce.isSelected();
		return new ProcessFolder(name, rootPath, null, pattern, ignore, maxDepth, append, disableExistanceCheck);
	}

	@Override
	public void loadData(Object[] data) {
		this.folder.setText((String) data[0]);
		this.pattern.setText((String) data[1]);
		this.ignore.setText((String) data[2]);
		this.enforce.setSelected((boolean) data[3]);
		this.maxDepth.setText(Integer.toString((int) data[4]));
	}
}
