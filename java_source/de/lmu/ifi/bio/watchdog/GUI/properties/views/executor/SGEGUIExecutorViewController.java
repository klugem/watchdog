package de.lmu.ifi.bio.watchdog.GUI.properties.views.executor;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.helper.SuggestPopup;
import de.lmu.ifi.bio.watchdog.GUI.helper.TextFilter;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginPropertyViewController;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginViewController;
import de.lmu.ifi.bio.watchdog.executor.external.sge.SGEExecutorInfo;
import de.lmu.ifi.bio.watchdog.executor.external.sge.SGEWorkloadManagerConnector;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

/**
 * 
 * @author kluge
 *
 */
public class SGEGUIExecutorViewController extends PluginViewController<SGEExecutorInfo>  {

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
	public void addPropertyViewControllerToValidate(PluginPropertyViewController<SGEExecutorInfo> executorPropertyViewController, String condition) {
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

	@SuppressWarnings("unchecked")
	@Override
	public SGEExecutorInfo getXMLPluginObject(Object[] data) {
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
		String shebang = (String) data[9];
		ArrayList<String> beforeScripts = (ArrayList<String>) data[10];
		ArrayList<String> afterScripts = (ArrayList<String>) data[11];
		ArrayList<String> packageManager = (ArrayList<String>) data[12];
		String container = (String) data[13];
		
		// create the instance
		return new SGEExecutorInfo(SGEWorkloadManagerConnector.EXECUTOR_NAME, name, isDefault, isStick2Host, maxSlaveRunning, path2java, maxRunning, watchdogBaseDir, environment, shebang, Integer.parseInt(this.slots.getText()), this.memory.getText(), this.queue.getText(), workingDir, this.customParams.getText(), this.disableDefaultParams.isSelected(), beforeScripts, afterScripts, packageManager, container);
	}
}
