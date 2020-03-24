package de.lmu.ifi.bio.watchdog.GUI.properties.views.wrapper;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.StatusConsole;
import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PropertyView;
import de.lmu.ifi.bio.watchdog.executionWrapper.ExecutionWrapper;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;

public class ExecutionWrapperPropertyView extends PropertyView {
	
	private ExecutionWrapperPropertyViewController controller;

	/** hide constructor */ 
	private ExecutionWrapperPropertyView() {}

	public static ExecutionWrapperPropertyView getExecutionWrapperPropertyView() {
		try {
			FXMLRessourceLoader<ExecutionWrapperPropertyView, ExecutionWrapperPropertyViewController> l = new FXMLRessourceLoader<>("ExecutionWrapperPropertyView.fxml", new ExecutionWrapperPropertyView());
			Pair<ExecutionWrapperPropertyView, ExecutionWrapperPropertyViewController> pair = l.getNodeAndController();
			ExecutionWrapperPropertyView p = pair.getKey();
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
		if(data instanceof ExecutionWrapper)
			this.controller.loadData((ExecutionWrapper) data);
	}

	@Override
	public void setStatusConsole(StatusConsole s) {
		this.controller.setStatusConsole(s);
	}
}