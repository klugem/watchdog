package de.lmu.ifi.bio.watchdog.GUI.properties.views.wrapper;

import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginPropertyViewController;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PropertyViewType;
import de.lmu.ifi.bio.watchdog.executionWrapper.ExecutionWrapper;

public class ExecutionWrapperPropertyViewController extends PluginPropertyViewController<ExecutionWrapper> {			

	@Override
	protected PropertyViewType getPropertyTypeName() {
		return PropertyViewType.WRAPPERS;
	}
	
	@Override
	protected void saveData() {
		// get data common to all wrappers
		String name = this.name.getText();
		
		// get the wrapper info
		ExecutionWrapper w = this.activeGUIView.getXMLPluginObject(new Object[] {name});
		
		// save the wrapper
		this.storeXMLData(w);
		super.saveData();
	}
	
	@Override
	protected void initGUIElements() {
				
	}

	@Override
	protected void loadAdditionalUnspecificBaseData(ExecutionWrapper data) {
		if(data != null) {
			
		}
	}
}
