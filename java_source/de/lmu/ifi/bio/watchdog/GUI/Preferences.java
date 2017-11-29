package de.lmu.ifi.bio.watchdog.GUI;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.AdditionalBarController;
import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Preferences extends Pane {
	
	private PreferencesController controller;

	/** hide constructor */
	private Preferences() {}

	public static Preferences getPreferences() {
		try {
			FXMLRessourceLoader<Preferences, PreferencesController> l = new FXMLRessourceLoader<>("Preferences.fxml", new Preferences());
			Pair<Preferences, PreferencesController> pair = l.getNodeAndController();
			Preferences p = pair.getKey();
			p.controller = pair.getValue();
			return p;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void setToolLibrary(ToolLibraryController tlc) {
		this.controller.setToolLibrary(tlc);
	}

	public void changeSelect(String tabname) {
		this.controller.changeSelect(tabname);
	}

	public void setCloseOnFirstSave(boolean closeOnFirstSave) {
		this.controller.setCloseOnFirstSave(closeOnFirstSave);
	}

	public void setAdditionalToolbar(AdditionalBarController additionalBarController) {
		this.controller.setAdditionalToolbar(additionalBarController);
	}

	public void onClose(Stage stage) {
		this.controller.onClose(stage);
	}
}