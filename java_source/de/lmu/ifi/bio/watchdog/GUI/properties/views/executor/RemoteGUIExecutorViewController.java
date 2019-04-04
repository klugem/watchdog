package de.lmu.ifi.bio.watchdog.GUI.properties.views.executor;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.helper.SuggestPopup;
import de.lmu.ifi.bio.watchdog.GUI.helper.TextFilter;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginPropertyViewController;
import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.executor.remote.RemoteExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.SSHPassphraseAuth;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

/**
 * Local executor has no additional settings --> nothing to do here
 * @author kluge
 *
 */
public class RemoteGUIExecutorViewController extends GUIExecutorViewController {
	
	@FXML private TextField host;
	@FXML private TextField port;
	@FXML private TextField user;
	@FXML private TextField privateKey;
	@FXML private CheckBox disableStrictHostCheck;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);
	}

	@Override
	public void addPropertyViewControllerToValidate(PluginPropertyViewController<ExecutorInfo> executorPropertyViewController, String condition) {
		// add integer enforcer
		this.port.setTextFormatter(TextFilter.getPositiveIntFormater());
		
		// add validation commands
		executorPropertyViewController.addValidateToControl(this.host, "host", f -> !executorPropertyViewController.isEmpty((TextField) f), condition);
		executorPropertyViewController.addValidateToControl(this.port, "port", f -> executorPropertyViewController.isInteger((TextField) f, "SSH port must be an integer. (f.e. 22)"), condition);
		executorPropertyViewController.addValidateToControl(this.user, "user", f -> !executorPropertyViewController.isEmpty((TextField) f), condition);
		executorPropertyViewController.addValidateToControl(this.privateKey, "privateKey", f -> executorPropertyViewController.isAbsoluteFile((TextField) f, "Private key must be an absolute path to a file."), condition);
		
		// add suggest constants support
		@SuppressWarnings("unused") SuggestPopup p1 = new SuggestPopup(this.host);
		@SuppressWarnings("unused") SuggestPopup p2 = new SuggestPopup(this.user);
		@SuppressWarnings("unused") SuggestPopup p3 = new SuggestPopup(this.privateKey);
		
		// add event handler for GUI validation
		this.host.textProperty().addListener(event -> executorPropertyViewController.validate());
		this.port.textProperty().addListener(event -> executorPropertyViewController.validate());
		this.user.textProperty().addListener(event -> executorPropertyViewController.validate());
		this.privateKey.textProperty().addListener(event -> executorPropertyViewController.validate());
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
		
		return new RemoteExecutorInfo(XMLParser.REMOTE, name, isDefault, isStick2Host, maxSlaveRunning, path2java, maxRunning, watchdogBaseDir, environment, shebang, this.host.getText(), this.user.getText(), Integer.parseInt(this.port.getText()), !this.disableStrictHostCheck.isSelected(), workingDir, new SSHPassphraseAuth(name, this.privateKey.getText(), true), beforeScripts, afterScripts);
	}

	@Override
	public void loadData(Object[] data) {
		this.host.setText((String) data[0]);
		this.user.setText((String) data[1]);
		this.privateKey.setText((String) data[2]);
		data[2] = null;
		this.port.setText(Integer.toString((Integer) data[3]));
		this.disableStrictHostCheck.setSelected((boolean) data[4]);
		data = null;
		System.gc();
	}
}
