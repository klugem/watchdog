package de.lmu.ifi.bio.watchdog.GUI.properties.views.processblocks;

import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.helper.TextFilter;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginPropertyViewController;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginViewController;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessSequence;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;


public class SequenceGUIProcessBlockViewController extends PluginViewController<ProcessSequence>  {
	
	@FXML private TextField start;
	@FXML private TextField end;
	@FXML private TextField step;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);
	}

	@Override
	public void addPropertyViewControllerToValidate(PluginPropertyViewController<ProcessSequence> propertyViewController, String condition) {
		// add double or integer enforcer
		this.start.setTextFormatter(TextFilter.getDoubleFormater());
		this.end.setTextFormatter(TextFilter.getDoubleFormater());
		this.step.setTextFormatter(TextFilter.getDoubleFormater());
		
		// add checker
		propertyViewController.addValidateToControl(this.start, "start", f -> propertyViewController.isDouble((TextField) f, "Start of process sequence must be a valid double value. (f.e. 5.0)"), condition);
		propertyViewController.addValidateToControl(this.end,  "end",f -> propertyViewController.isDouble((TextField) f, "End of process sequence must be a valid double value. (f.e. 9.0)"), condition);
		
		// add event handler for GUI validation
		this.start.textProperty().addListener(event -> propertyViewController.validate());
		this.end.textProperty().addListener(event -> propertyViewController.validate());
	}

	@Override
	public void setHandlerForGUIColoring() {}

	@Override
	public ProcessSequence getXMLPluginObject(Object[] data) {
		// cast the data
		String name = (String) data[0];
		boolean append = (boolean) data[1];
		double s = Double.parseDouble(this.start.getText());
		double e = Double.parseDouble(this.end.getText());
		double st = Double.parseDouble(this.step.getText());
		return new ProcessSequence(name, s, e, st, append);
	}

	@Override
	public void loadData(Object[] data) {
		this.start.setText(Double.toString((Double) data[0]));
		this.end.setText(Double.toString((Double) data[1]));
		this.step.setText(Double.toString((Double) data[2]));
	}
}
