package de.lmu.ifi.bio.watchdog.GUI.helper;

import java.util.Optional;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

public class Inform {

	public static void inform(String text) {
		alert(AlertType.INFORMATION, "Information", text, null);
	}
	
	public static void error(String header, String text) {
		alert(AlertType.ERROR, "Error", header, text);
	}
	
	public static Optional<ButtonType> alert(AlertType type, String title, String header, String text) {
		Alert alert = new Alert(type);
		alert.setTitle(title);
		alert.setResizable(true);
		if(header != null) alert.setHeaderText(header);
		if(text != null) alert.setContentText(text.replace(". ", "." + System.lineSeparator()));
		Platform.runLater(() -> CurrentScreen.centerOnScreen(alert, CurrentScreen.getActiveScreen()));
		return alert.showAndWait();
	}

	public static Optional<ButtonType> confirm(String header) {
		return alert(AlertType.CONFIRMATION, "Confirmation required", header, null);
	}
}
