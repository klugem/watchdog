package de.lmu.ifi.bio.watchdog.GUI.properties.views.processblocks;

import java.io.File;

public class InputGUIProcessBlockView extends GUIProcessBlockView {

	@Override
	public String getName() {
		return "process input";
	}

	@Override
	public String getFXMLResourceFilename() {
		return "processblock" + File.separator + "InputProcessBlockProperty.fxml";
	}
}