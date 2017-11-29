package de.lmu.ifi.bio.watchdog.GUI;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import javafx.application.HostServices;
import javafx.scene.layout.Pane;

public class About extends Pane {
	
	private AboutController controller;

	/** hide constructor */
	private About() {}

	public static About getAbout(HostServices hostservice) {
		try {
			FXMLRessourceLoader<About, AboutController> l = new FXMLRessourceLoader<>("About.fxml", new About());
			Pair<About, AboutController> pair = l.getNodeAndController();
			About p = pair.getKey();
			p.controller = pair.getValue();
			p.controller.setHostServices(hostservice);
			return p;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}