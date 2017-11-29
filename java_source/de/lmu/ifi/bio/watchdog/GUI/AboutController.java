package de.lmu.ifi.bio.watchdog.GUI;

import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.runner.XMLBasedWatchdogRunner;
import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.Label;

public class AboutController implements Initializable {

	@FXML private Label version;
	@FXML private Label website;
	@FXML private Label author;
	@FXML private Label mail;
	
	private HostServices hostservice;
		
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// set version
		this.version.setText(XMLBasedWatchdogRunner.getVersion());
		
		// set event handler
		this.website.onMouseClickedProperty().set(event -> this.openURL());
		this.mail.onMouseClickedProperty().set(event -> this.openMail());
		
		// change cursor
		this.website.onMouseEnteredProperty().set(event -> this.website.getScene().setCursor(Cursor.HAND));
		this.website.onMouseExitedProperty().set(event -> this.website.getScene().setCursor(Cursor.DEFAULT));
		this.mail.onMouseEnteredProperty().set(event -> this.website.getScene().setCursor(Cursor.HAND));
		this.mail.onMouseExitedProperty().set(event -> this.website.getScene().setCursor(Cursor.DEFAULT));
	}

	private void openMail() {
		String mailto = "mailto:?to="+this.mail.getText()+"&subject=Watchdog: "; 
		this.hostservice.showDocument(mailto);
	}

	private void openURL() {
		this.hostservice.showDocument(this.website.getText());
	}

	public void setHostServices(HostServices hostservice) {
		this.hostservice = hostservice;
	}
}
