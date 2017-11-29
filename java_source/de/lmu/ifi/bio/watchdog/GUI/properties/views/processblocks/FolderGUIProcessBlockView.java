package de.lmu.ifi.bio.watchdog.GUI.properties.views.processblocks;

import java.io.File;

public class FolderGUIProcessBlockView extends GUIProcessBlockView {

	@Override
	public String getName() {
		return "process folder";
	}

	@Override
	public String getFXMLResourceFilename() {
		return "processblock" + File.separator + "FolderProcessBlockProperty.fxml";
	}
}