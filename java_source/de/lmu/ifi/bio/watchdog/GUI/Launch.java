package de.lmu.ifi.bio.watchdog.GUI;

import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Pair;

public class Launch extends Pane implements Runnable {

	
	private LaunchController controller;
	private Stage secondaryStage;
	private Runnable enforcePreferences;
	
	/** hide constructor */
	private Launch() {}

	public static Launch getLaunch(Stage secondaryStage) {
		try {
			FXMLRessourceLoader<Launch, LaunchController> l = new FXMLRessourceLoader<>("Launch.fxml", new Launch());
			Pair<Launch, LaunchController> pair = l.getNodeAndController();
			Launch p = pair.getKey();;
			p.secondaryStage = secondaryStage;
			p.controller = pair.getValue();		
			return p;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void run() {
		Platform.runLater(() -> this.secondaryStage.show());
		this.controller.run();
		// wait a short time and close the window afterwards
		try { Thread.sleep(25); } catch(Exception e) {}
		Platform.runLater(() -> this.switchWindows());
	}
	
	private void switchWindows() {
		this.getScene().getWindow().hide();
		
		// enforce preferences if required
		this.enforcePreferences.run();
	}

	public void setRunable(Runnable r) {
		this.enforcePreferences = r;
	}
}