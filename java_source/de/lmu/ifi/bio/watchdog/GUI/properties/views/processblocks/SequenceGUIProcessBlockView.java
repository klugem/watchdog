package de.lmu.ifi.bio.watchdog.GUI.properties.views.processblocks;

import java.io.File;

public class SequenceGUIProcessBlockView extends GUIProcessBlockView {

	@Override
	public String getName() {
		return "process sequence";
	}

	@Override
	public String getFXMLResourceFilename() {
		return "processblock" + File.separator + "SequenceProcessBlockProperty.fxml";
	}
}