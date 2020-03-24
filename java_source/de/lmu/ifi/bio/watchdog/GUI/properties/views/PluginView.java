package de.lmu.ifi.bio.watchdog.GUI.properties.views;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import javafx.scene.layout.Pane;

public abstract class PluginView<T extends XMLDataStore> extends Pane {

	private PluginViewController<T> controller;
	
	/** hide constructor */ 
	protected PluginView() {}
	
	public static <A extends XMLDataStore> PluginView<A> getPropertyView(String fxmlFile, PluginView<A> pane) {
		try {
			FXMLRessourceLoader<PluginView<A>, PluginViewController<A>> l = new FXMLRessourceLoader<>(fxmlFile, pane);
			Pair<PluginView<A>, PluginViewController<A>> pair = l.getNodeAndController();
			PluginView<A> p = pair.getKey();
			p.controller = pair.getValue();
				
			// set properties on GUI

			return p;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public String toString() {
		return this.getName();
	}
	
	/**
	 * name of that executor
	 * @return
	 */
	public abstract String getName();

	/**
	 * name of the fxml resource file
	 * relative path to class FXMLRessourceLoader (de.lmu.ifi.bio.watchdog.GUI.fxml): example 'GUI_test.fxml'
	 * or
	 * absolute path with package: example '/org/x/fxml/GUI_test.fxml'
	 * @return
	 */
	public abstract String getFXMLResourceFilename();

	/**
	 * adds the validate commands to the control using name as condition
	 * @param propertyViewController
	 */
	public void addValidateToControl(PluginPropertyViewController<T> propertyViewController) {
		this.controller.addPropertyViewControllerToValidate(propertyViewController, this.getName());
	}
	
	/**
	 * returns the plugin object
	 * @return
	 */
	public T getXMLPluginObject(Object[] data) {
		return this.controller.getXMLPluginObject(data);
	}

	/**
	 * loads plugin specific data
	 * @param data
	 */
	public void loadData(Object[] data) {
		this.controller.loadData(data);
	}
}