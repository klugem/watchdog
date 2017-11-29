package de.lmu.ifi.bio.watchdog.GUI.properties.views;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.StatusConsole;
import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;

public class EnvironmentPropertyView extends PropertyView {
	
	private EnvironmentPropertyViewController controller;

	/** hide constructor */ 
	private EnvironmentPropertyView() {}

	public static EnvironmentPropertyView getEnvironmentPropertyView() {
		try {
			FXMLRessourceLoader<EnvironmentPropertyView, EnvironmentPropertyViewController> l = new FXMLRessourceLoader<>("EnvironmentPropertyView.fxml", new EnvironmentPropertyView());
			Pair<EnvironmentPropertyView, EnvironmentPropertyViewController> pair = l.getNodeAndController();
			EnvironmentPropertyView p = pair.getKey();
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
	public XMLDataStore getStoredData() {
		return this.controller.getStoredData();
	}

	@Override
	public void loadData(XMLDataStore data) {
		if(data instanceof Environment)
			this.controller.loadData((Environment) data);
	}

	@Override
	public void setStatusConsole(StatusConsole s) {
		this.controller.setStatusConsole(s);
	}
}