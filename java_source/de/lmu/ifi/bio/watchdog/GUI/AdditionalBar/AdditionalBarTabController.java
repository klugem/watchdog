package de.lmu.ifi.bio.watchdog.GUI.AdditionalBar;

import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;

public abstract class AdditionalBarTabController implements Initializable {
	
	@FXML protected ScrollPane root;
	
	public void setWidth(double width) {
		this.root.setMinWidth(width);
		this.root.setMaxWidth(width);
		this.root.setPrefWidth(width);
	}
	
	public void setHeight(double height) {
		this.root.setMinHeight(height);
		this.root.setMaxHeight(height);
		this.root.setPrefHeight(height);
	}
	
	/**
	 * returns the name for the tab
	 * @return
	 */
	public abstract String getName();
	
	/**
	 * returns the name of the image that must be stored in the package de.lmu.ifi.bio.watchdog.GUI.png
	 * @return
	 */
	public abstract String getImageName();
	
	/**
	 * returns the image or null for the tab
	 * @return
	 */
	public ImageView getImage() {
		return ImageLoader.getImage(this.getImageName());
	}
	
	/**
	 * returns the element on which tab events are fired
	 * @return
	 */
	protected abstract Node getRoot();
}
