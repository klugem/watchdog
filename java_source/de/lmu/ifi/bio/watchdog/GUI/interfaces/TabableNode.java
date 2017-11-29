package de.lmu.ifi.bio.watchdog.GUI.interfaces;

import de.lmu.ifi.bio.watchdog.GUI.event.TabEvent;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;

public abstract class TabableNode extends AnchorPane {
		
	public abstract void setWidth(double w);
	
	public abstract void setHeight(double h);
	
	/**
	 * returns the name for the tab
	 * @return
	 */
	public abstract String getName();
	
	/**
	 * returns the image or null for the tab
	 * @return
	 */
	public abstract ImageView getImage();
	
	/**
	 * fires the event on the root element
	 * @param e
	 */
	public void fireTabEvent(TabEvent e) {
		this.getRoot().fireEvent(e);	
	}

	/**
	 * returns the element on which tab events are fired
	 * @return
	 */
	protected abstract Node getRoot();
}
