package de.lmu.ifi.bio.watchdog.GUI.properties.views.processblocks;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.MessageType;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginPropertyViewController;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PropertyViewType;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessBlock;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

public class ProcessBlockPropertyViewController extends PluginPropertyViewController<ProcessBlock> {

	@FXML private CheckBox append;
	
	@Override
	protected PropertyViewType getPropertyTypeName() {
		return PropertyViewType.PROCESS_BLOCK;
	}
	
	@Override
	protected void saveData() {
		// get data common to all executors
		String name = this.name.getText();
		boolean append = this.append.isSelected();
		
		// get the process block
		ProcessBlock b = this.activeGUIView.getXMLPluginObject(new Object[] {name, append});
		
		// save the process block.
		this.storeXMLData(b);
		super.saveData();
	}
	
	@Override
	protected boolean hasUniqueName(String name) {
		// look for append
		if(!this.append.isDisabled() && this.append.isSelected())
			return true;

		if(!XMLDataStore.hasRegistedData(name, ProcessBlock.class))
			return true;
		else {
			this.addMessageToPrivateLog(MessageType.ERROR, "An process block with name '"+name+"' exists already.");
			return false;
		}
	}
	
	@Override
	protected void initGUIElements() {
		// type enforcer
		this.append.selectedProperty().addListener(event -> this.validate());
	}

	@Override
	protected void loadAdditionalUnspecificBaseData(ProcessBlock data) {
		if(data != null) {
			if(data.isAppendAble())
				this.append.setSelected(data.gui_append);
			else
				this.append.setDisable(true);
		}
	}
}
