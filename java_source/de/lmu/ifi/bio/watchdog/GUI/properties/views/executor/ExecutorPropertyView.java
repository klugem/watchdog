package de.lmu.ifi.bio.watchdog.GUI.properties.views.executor;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.StatusConsole;
import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PropertyView;
import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;

public class ExecutorPropertyView extends PropertyView {
	
	private ExecutorPropertyViewController controller;

	/** hide constructor */ 
	private ExecutorPropertyView() {}

	public static ExecutorPropertyView getExecutorPropertyView() {
		try {
			FXMLRessourceLoader<ExecutorPropertyView, ExecutorPropertyViewController> l = new FXMLRessourceLoader<>("ExecutorPropertyView.fxml", new ExecutorPropertyView());
			Pair<ExecutorPropertyView, ExecutorPropertyViewController> pair = l.getNodeAndController();
			ExecutorPropertyView p = pair.getKey();
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
		if(data instanceof ExecutorInfo)
			this.controller.loadData((ExecutorInfo) data);
	}

	@Override
	public void setStatusConsole(StatusConsole s) {
		this.controller.setStatusConsole(s);
	}
}