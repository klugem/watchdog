package de.lmu.ifi.bio.watchdog.GUI.preferences;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.MessageType;
import de.lmu.ifi.bio.watchdog.GUI.helper.Inform;
import de.lmu.ifi.bio.watchdog.GUI.helper.PreferencesStore;
import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import de.lmu.ifi.bio.watchdog.helper.Mailer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

public class NotificationPreferencesController extends AbstractPreferencesController {

	private final static String ROOT_PATH = "select smtp mail config file";
	
	@FXML private Button configSelectPathButton;
	@FXML private TextField mailConfigPath;
	@FXML private TextField mail;
	@FXML private CheckBox enable;
		
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// add event handlers
		this.configSelectPathButton.setOnAction(e -> { this.selectMailConfigPath(); e.consume(); });
		this.configSelectPathButton.setGraphic(ImageLoader.getImage(ImageLoader.ZOOM_SMALL));
		
		// add validate stuff
		this.addValidateToControl(this.mailConfigPath, "mailConfigPath", m -> this.validateConfigPath());
		this.addValidateToControl(this.mail, "mail", m -> this.validateMail());
		this.addValidateToControl(this.enable, "enable", null);
		
		this.mail.textProperty().addListener(x -> this.validate());
		this.mailConfigPath.textProperty().addListener(x -> this.validate());
		this.enable.selectedProperty().addListener(x -> this.changeEnable());
		
		// get initial coloring
		super.initialize(location, resources);
	}

	private boolean validateConfigPath() {
		String p = this.mailConfigPath.getText();
		if(p.length() == 0) // not configured
			return true;
		
		// configure --> check if valid
		if(new File(p).isFile()) 
			return true; 

		this.addMessageToPrivateLog(MessageType.ERROR, "Mail server config must be a valid path to an existing file.");
		return false;
	}

	private boolean validateMail() {
		if(!this.enable.isSelected())
			return true;
		return Mailer.validateMail(this.mail.getText());
	}

	private void changeEnable() {
		boolean isEnabled = !this.enable.isSelected();
		this.mail.setDisable(isEnabled);
		this.mailConfigPath.setDisable(isEnabled);
		this.configSelectPathButton.setDisable(isEnabled);
		
		this.validate();
	}

	/**
	 * is called when the user want to select a new root Watchdog base dir path
	 */
	private void selectMailConfigPath() {
		FileChooser d = new FileChooser();
		d.setTitle(ROOT_PATH);
		File file = d.showOpenDialog(this.configSelectPathButton.getScene().getWindow());
		
		// check, a file was selected
		if(file != null) {
			if(file.isFile() && file.canRead()) {
				this.mailConfigPath.setText(file.getAbsolutePath()); 
			}
			else {
				Inform.error("", "Can not read '" + file.getAbsolutePath() + "'.");
				this.mailConfigPath.setText("");
			}
			this.validate();
		}
	}
	
	@Override
	public void onSave() {
		if(this.validate()) {
			// clear the data
			if(!this.enable.isSelected()) {
				this.mailConfigPath.setText("");
				this.mail.setText("");
			}
			PreferencesStore.setSMTPConfigPath(this.mailConfigPath.getText());
			PreferencesStore.setMail(this.mail.getText());
			super.onSave();
		}
	}

	@Override
	public void onLoad() {
		if(!PreferencesStore.isMailNotificationEnabled())
			this.enable.setSelected(false);
		else {
			this.enable.setSelected(true);
			this.mail.setText(PreferencesStore.getMail());
			this.mailConfigPath.setText(PreferencesStore.getSMTPConfigPath());
		}
		this.changeEnable();
		super.onLoad();
	}
}