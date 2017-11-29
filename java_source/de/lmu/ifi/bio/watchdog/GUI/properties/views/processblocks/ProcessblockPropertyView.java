package de.lmu.ifi.bio.watchdog.GUI.properties.views.processblocks;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.StatusConsole;
import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PropertyView;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessBlock;

public class ProcessblockPropertyView extends PropertyView {
	
	private ProcessBlockPropertyViewController controller;

	/** hide constructor */ 
	private ProcessblockPropertyView() {}

	public static ProcessblockPropertyView getProcessblockPropertyView() {
		try { 
			FXMLRessourceLoader<ProcessblockPropertyView, ProcessBlockPropertyViewController> l = new FXMLRessourceLoader<>("ProcessblockPropertyView.fxml", new ProcessblockPropertyView());
			Pair<ProcessblockPropertyView, ProcessBlockPropertyViewController> pair = l.getNodeAndController();
			ProcessblockPropertyView p = pair.getKey();
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
		if(data instanceof ProcessBlock)
			this.controller.loadData((ProcessBlock) data);
	}

	@Override
	public void setStatusConsole(StatusConsole s) {
		this.controller.setStatusConsole(s);
	}
}