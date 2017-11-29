package de.lmu.ifi.bio.watchdog.GUI.properties.views.processblocks;

import java.io.File;

public class TableGUIProcessBlockView extends GUIProcessBlockView {

	@Override
	public String getName() {
		return "process table";
	}

	@Override
	public String getFXMLResourceFilename() {
		return "processblock" + File.separator + "TableProcessBlockProperty.fxml";
	}
}