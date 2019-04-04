package de.lmu.ifi.bio.watchdog.GUI.properties.views.executor;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.helper.SuggestPopup;
import de.lmu.ifi.bio.watchdog.GUI.helper.TextFilter;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginPropertyViewController;
import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.executor.external.slurm.SlurmExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

/**
 * 
 * @author kluge
 *
 */
public class SlurmGUIExecutorViewController extends GUIExecutorViewController {

	@FXML private TextField cluster;
	@FXML private TextField partition;
	@FXML private TextField cpu;
	@FXML private TextField memory;
	@FXML private TextField timelimit;
	@FXML private CheckBox disableDefaultParams;
	@FXML private TextField customParams;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);
	}

	@Override
	public void addPropertyViewControllerToValidate(PluginPropertyViewController<ExecutorInfo> executorPropertyViewController, String condition) {
		// add integer enforcer
		this.cpu.setTextFormatter(TextFilter.getPositiveIntFormater());
		this.memory.setTextFormatter(TextFilter.getMemoryFormater());
		this.timelimit.setTextFormatter(TextFilter.getTimelimitFormater());
		
		// add validation commands
		executorPropertyViewController.addValidateToControl(this.cluster, "cluster", f-> !executorPropertyViewController.isEmpty((TextField) f, "An name for the cluster queue must be given."), condition);
		executorPropertyViewController.addValidateToControl(this.partition, "partition", f-> !executorPropertyViewController.isEmpty((TextField) f, "An name for the partition must be given."), condition);
		executorPropertyViewController.addValidateToControl(this.cpu, "cpu", f -> executorPropertyViewController.isInteger((TextField) f, "CPU must be an integer."), condition);
		executorPropertyViewController.addValidateToControl(this.memory, "memory", f -> !executorPropertyViewController.isEmpty((TextField) f, "A valid memory limit in form [0-9]+[MG]{0,1} must be entered."), condition);
		executorPropertyViewController.addValidateToControl(this.timelimit, "timelimit", f -> !executorPropertyViewController.isEmpty((TextField) f, "A valid time limit in form [0-9]+-[0-9]+:[0-9]+ must be entered."), condition);
		executorPropertyViewController.addValidateToControl(this.customParams, "customParams", f -> true, condition);
		
		// add suggest constants support
		@SuppressWarnings("unused") SuggestPopup p1 = new SuggestPopup(this.cluster);
		@SuppressWarnings("unused") SuggestPopup p2 = new SuggestPopup(this.customParams);
		
		// add event handler for GUI validation
		this.cluster.textProperty().addListener(event -> executorPropertyViewController.validate());
		this.partition.textProperty().addListener(event -> executorPropertyViewController.validate());
		this.cpu.textProperty().addListener(event -> executorPropertyViewController.validate());
		this.memory.textProperty().addListener(event -> executorPropertyViewController.validate());
		this.timelimit.textProperty().addListener(event -> executorPropertyViewController.validate());
	}

	@Override
	public void setHandlerForGUIColoring() {}

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
		String shebang = (String) data[9];
		@SuppressWarnings("unchecked")
		ArrayList<String> beforeScripts = (ArrayList<String>) data[10];
		@SuppressWarnings("unchecked")
		ArrayList<String> afterScripts = (ArrayList<String>) data[11];
		
		return new SlurmExecutorInfo(XMLParser.CLUSTER, name, isDefault, isStick2Host, maxSlaveRunning, path2java, maxRunning, watchdogBaseDir, environment, shebang, Integer.parseInt(this.cpu.getText()), this.memory.getText(), this.cluster.getText(), this.partition.getText(), this.timelimit.getText(), workingDir, this.customParams.getText(), this.disableDefaultParams.isSelected(), beforeScripts, afterScripts);
	}

	@Override
	public void loadData(Object[] data) {
		this.cluster.setText((String) data[0]);
		this.partition.setText((String) data[1]);
		this.cpu.setText(Integer.toString((Integer) data[2]));
		this.memory.setText((String) data[3]);
		this.timelimit.setText((String) data[4]);
		this.customParams.setText((String) data[5]);
		this.disableDefaultParams.setSelected((boolean) data[6]); 
	}
}
