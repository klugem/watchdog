package de.lmu.ifi.bio.watchdog.GUI.properties.views;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.StatusConsole;
import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import de.lmu.ifi.bio.watchdog.helper.Constants;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import javafx.util.Pair;

public class ConstantsPropertyView extends PropertyView {
	
	private ConstantsPropertyViewController controller;

	/** hide constructor */ 
	private ConstantsPropertyView() {}

	public static ConstantsPropertyView getConstantsPropertyView() {
		try {
			FXMLRessourceLoader<ConstantsPropertyView, ConstantsPropertyViewController> l = new FXMLRessourceLoader<>("ConstantsPropertyView.fxml", new ConstantsPropertyView());
			Pair<ConstantsPropertyView, ConstantsPropertyViewController> pair = l.getNodeAndController();
			ConstantsPropertyView p = pair.getKey();
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
		if(data instanceof Constants)
			this.controller.loadData((Constants) data);
	}

	@Override
	public void setStatusConsole(StatusConsole s) {
		this.controller.setStatusConsole(s);
	}
}