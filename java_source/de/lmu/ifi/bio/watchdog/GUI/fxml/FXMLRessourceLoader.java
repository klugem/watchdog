package de.lmu.ifi.bio.watchdog.GUI.fxml;

import org.apache.commons.lang3.tuple.Pair;

import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.Pane;

/**
 * Class that can be used to load fxml files
 * @author kluge
 *
 */
public class FXMLRessourceLoader<A extends Pane,B extends Initializable> {
	
	private final String FXML;
	private final A PANE;

	public FXMLRessourceLoader(String fxmlFile, A pane) {
		this.FXML = fxmlFile;
		this.PANE = pane;
	}
	
	public Pair<A,B> getNodeAndController() {
		try {
			// create instance
			FXMLLoader loader = new FXMLLoader(FXMLRessourceLoader.class.getResource(this.FXML));
			this.PANE.getChildren().add(loader.load());
			B controller = loader.getController();
			return Pair.of(this.PANE, controller);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public A getNode() {
		try {
			// create instance
			FXMLLoader loader = new FXMLLoader(FXMLRessourceLoader.class.getResource(this.FXML));
			this.PANE.getChildren().add(loader.load());
			return this.PANE;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;	
	}
}
