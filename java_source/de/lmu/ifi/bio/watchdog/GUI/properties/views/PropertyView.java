package de.lmu.ifi.bio.watchdog.GUI.properties.views;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.StatusConsole;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import javafx.scene.layout.Pane;

/**
 * Class from which property views must inherit.
 * @author kluge
 *
 */
public abstract class PropertyView extends Pane {

	/**
	 * returns the stored data
	 * @return
	 */
	public abstract XMLDataStore getStoredData();

	/**
	 * loads the data onto the GUI
	 * @param xmlData
	 */
	public abstract void loadData(XMLDataStore data);
	
	/**
	 * sets the status console used for this property view
	 * @param s
	 */
	public abstract void setStatusConsole(StatusConsole s);
}
