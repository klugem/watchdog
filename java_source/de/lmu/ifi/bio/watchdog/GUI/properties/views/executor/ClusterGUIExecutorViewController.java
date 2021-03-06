package de.lmu.ifi.bio.watchdog.GUI.properties.views.executor;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.helper.SuggestPopup;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginPropertyViewController;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginViewController;
import de.lmu.ifi.bio.watchdog.executor.external.drmaa.DRMAAExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/**
 * 
 * @author kluge
 *
 */
public class ClusterGUIExecutorViewController extends  PluginViewController<DRMAAExecutorInfo>  {

	@FXML private TextField customParams;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);
	}

	@Override
	public void addPropertyViewControllerToValidate(PluginPropertyViewController<DRMAAExecutorInfo> executorPropertyViewController, String condition) {
		executorPropertyViewController.addValidateToControl(this.customParams, "customParams", f -> true, condition);
		
		// add suggest constants support
		@SuppressWarnings("unused") SuggestPopup p2 = new SuggestPopup(this.customParams);
		
		// add event handler for GUI validation
	}

	@Override
	public void setHandlerForGUIColoring() {}	

	@Override
	public void loadData(Object[] data) {
		this.customParams.setText((String) data[0]);
	}

	@SuppressWarnings("unchecked")
	@Override
	public DRMAAExecutorInfo getXMLPluginObject(Object[] data) {
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
		return new DRMAAExecutorInfo(XMLParser.CLUSTER, name, isDefault, isStick2Host, maxSlaveRunning, path2java, maxRunning, watchdogBaseDir, environment, shebang, workingDir, this.customParams.getText(), beforeScripts, afterScripts, packageManager, container);
	}
}
