package de.lmu.ifi.bio.watchdog.GUI.helper;

import java.io.File;

import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import javafx.scene.layout.Pane;
import javafx.util.Pair;

/**
 * should be constructed using the static method defined in LogView
 * @author kluge
 *
 */
public class LogView extends Pane {

	private LogViewController controller;
	
	/** hide constructor */ 
	protected LogView() {}
	
	public static LogView getLogViewer(File file) {
		try {
			FXMLRessourceLoader<LogView, LogViewController> l = new FXMLRessourceLoader<>("LogView.fxml", new LogView());
			Pair<LogView, LogViewController> pair = l.getNodeAndController();
			LogView p = pair.getKey();
			p.controller = pair.getValue();
				
			// set properties on GUI
			p.controller.setFile(file);

			return p;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}


