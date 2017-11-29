package de.lmu.ifi.bio.watchdog.GUI.properties.views;

import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TitledPane;

public abstract class PluginViewController<T extends XMLDataStore> implements Initializable {
	
	@FXML private TitledPane advancedPane;

	@Override
	public void initialize(URL location, ResourceBundle resources) {} 

	/**
	 * adds the checker to the parent controller using condition as condition
	 * @param executorPropertyViewController
	 * @param condition
	 */
	public abstract void addPropertyViewControllerToValidate(PluginPropertyViewController<T> propertyViewController, String condition);
	
	/**
	 * must set be handlers that are required to color the GUI
	 */
	public abstract void setHandlerForGUIColoring();
	
	/**
	 * returns the XML plugin object that was saved on GUI
	 * @return
	 */
	public abstract T getXMLPluginObject(Object[] data);
	
	/**
	 * loads plugin object specific data
	 * @param data
	 */
	public abstract void loadData(Object[] data);
}
