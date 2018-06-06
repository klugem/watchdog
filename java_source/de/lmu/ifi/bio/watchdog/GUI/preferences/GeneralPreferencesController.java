package de.lmu.ifi.bio.watchdog.GUI.preferences;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.MessageType;
import de.lmu.ifi.bio.watchdog.GUI.event.WatchdogBaseDirUpdateEvent;
import de.lmu.ifi.bio.watchdog.GUI.helper.Inform;
import de.lmu.ifi.bio.watchdog.GUI.helper.PreferencesStore;
import de.lmu.ifi.bio.watchdog.GUI.helper.TextFilter;
import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import de.lmu.ifi.bio.watchdog.runner.XMLBasedWatchdogRunner;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

public class GeneralPreferencesController extends AbstractPreferencesController {

	private final static String ROOT_PATH = "select install directory of Watchdog";
	
	@FXML private Button buttonSelectInstallPath;
	@FXML private TextField installPath;
	@FXML private TextField port;
		
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// add event handlers
		this.buttonSelectInstallPath.setOnAction(e -> { this.selectRootPath(); e.consume(); });
		this.buttonSelectInstallPath.setGraphic(ImageLoader.getImage(ImageLoader.ZOOM_SMALL));
		
		// add validate stuff
		this.addValidateToControl(this.installPath, "installPath", f ->  this.installPath.getText() !=null && this.installPath.getText().length() > 0 && XMLParser.checkWatchdogXSD(new File(this.installPath.getText())));
		this.addValidateToControl(this.port, "port", p -> this.portCheck((TextField) p));
		this.port.setTextFormatter(TextFilter.getPositiveIntFormater());
		
		this.port.textProperty().addListener(x -> this.validate());
		this.installPath.textProperty().addListener(x -> this.validate());
			
		// get initial coloring
		super.initialize(location, resources);
	}

	private boolean portCheck(TextField p) {
		if(p.getText() != null && p.getText().length() > 0) {
			Integer port = Integer.parseInt(p.getText());
			if(port > 0 && port <= 65535) {
				boolean ok = XMLBasedWatchdogRunner.portOK(port);
				if(!ok) 
					this.addMessageToPrivateLog(MessageType.ERROR, "Port '"+port+"' is already used by another tool.");
				return ok;
			}
			else {
				this.addMessageToPrivateLog(MessageType.ERROR, "A valid socket port is in the range [1:65535]. Typical webservers often use 80 or 8080 as port.");
			}
		}	
		return false;
	}

	/**
	 * is called when the user want to select a new root Watchdog base dir path
	 */
	private void selectRootPath() {
		DirectoryChooser d = new DirectoryChooser();
		d.setTitle(ROOT_PATH);
		File dir = d.showDialog(this.buttonSelectInstallPath.getScene().getWindow());
		
		// check, if valid watchdog dir was selected
		if(dir != null) {
			if(XMLParser.checkWatchdogXSD(dir)) {
				this.installPath.setText(dir.getAbsolutePath()); 
			}
			else {
				Inform.error("", "'" + dir.getAbsolutePath() + "' seems not to be a valid Watchdog installation.");
				this.installPath.setText("");
			}
			this.validate();
		}
	}
	
	@Override
	public void onSave() {
		if(this.validate()) {
			String newDir = this.installPath.getText();
			boolean change = PreferencesStore.setBaseDir(newDir);
			if(change) {
				// send the event
				this.sendEventToSiblingPages(new WatchdogBaseDirUpdateEvent(newDir));
			}					
						
			PreferencesStore.setPort(Integer.parseInt(this.port.getText()));
			super.onSave();
		}
	}

	@Override
	public void onLoad() {
		if(PreferencesStore.hasWatchdogBaseDir())
			this.installPath.setText(PreferencesStore.getWatchdogBaseDir());
		
		this.port.setText(Integer.toString(PreferencesStore.getPort()));
		
		super.onLoad();
	}
}