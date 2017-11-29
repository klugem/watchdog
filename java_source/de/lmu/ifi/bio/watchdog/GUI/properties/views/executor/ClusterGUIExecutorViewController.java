package de.lmu.ifi.bio.watchdog.GUI.properties.views.executor;

import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.helper.SuggestPopup;
import de.lmu.ifi.bio.watchdog.GUI.helper.TextFilter;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginPropertyViewController;
import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.executor.external.drmaa.DRMAAExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

/**
 * Local executor has no additional settings --> nothing to do here
 * @author kluge
 *
 */
public class ClusterGUIExecutorViewController extends GUIExecutorViewController {

	@FXML private TextField queue;
	@FXML private TextField slots;
	@FXML private TextField memory;
	@FXML private CheckBox disableDefaultParams;
	@FXML private TextField customParams;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);
	}

	@Override
	public void addPropertyViewControllerToValidate(PluginPropertyViewController<ExecutorInfo> executorPropertyViewController, String condition) {
		// add integer enforcer
		this.slots.setTextFormatter(TextFilter.getPositiveIntFormater());
		this.memory.setTextFormatter(TextFilter.getMemoryFormater());
		
		// add validation commands
		executorPropertyViewController.addValidateToControl(this.queue, "queue", f-> !executorPropertyViewController.isEmpty((TextField) f, "An name for the queue must be given."), condition);
		executorPropertyViewController.addValidateToControl(this.slots, "slots", f -> executorPropertyViewController.isInteger((TextField) f, "Slots must be an integer."), condition);
		executorPropertyViewController.addValidateToControl(this.memory, "memory", f -> !executorPropertyViewController.isEmpty((TextField) f, "A valid memory limit in form [0-9]+[MG]{0,1} must be entered."), condition);
		executorPropertyViewController.addValidateToControl(this.customParams, "customParams", f -> true, condition);
		
		// add suggest constants support
		@SuppressWarnings("unused") SuggestPopup p1 = new SuggestPopup(this.queue);
		@SuppressWarnings("unused") SuggestPopup p2 = new SuggestPopup(this.customParams);
		
		// add event handler for GUI validation
		this.queue.textProperty().addListener(event -> executorPropertyViewController.validate());
		this.slots.textProperty().addListener(event -> executorPropertyViewController.validate());
		this.memory.textProperty().addListener(event -> executorPropertyViewController.validate());
	}

	@Override
	public void setHandlerForGUIColoring() {}

	

	@Override
	public void loadData(Object[] data) {
		this.queue.setText((String) data[0]);
		this.slots.setText(Integer.toString((Integer) data[1]));
		this.memory.setText((String) data[2]);
		this.customParams.setText((String) data[3]);
		this.disableDefaultParams.setSelected((boolean) data[4]); 
	}

	@Override
	public ExecutorInfo getXMLPluginObject(Object[] data) {
		// cast the data
		String name = (String) data[0];
		boolean isDefault = (boolean) data[1];
		boolean isStick2Host = (boolean) data[2];
		Integer maxSlaveRunning = (Integer) data[3];
		String path2java = (String) data[4];
		int maxRunning = (int) data[5];
		String watchdogBaseDir = (String) data[6];
		Environment environment = (Environment) data[7];
		String workingDir = (String) data[8];
		// create the instance
		return new DRMAAExecutorInfo(XMLParser.CLUSTER, name, isDefault, isStick2Host, maxSlaveRunning, path2java, maxRunning, watchdogBaseDir, environment, Integer.parseInt(this.slots.getText()), this.memory.getText(), this.queue.getText(), workingDir, this.customParams.getText(), this.disableDefaultParams.isSelected());
	}
}
