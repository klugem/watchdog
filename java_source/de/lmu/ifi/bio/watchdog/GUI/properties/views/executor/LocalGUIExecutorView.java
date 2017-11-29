package de.lmu.ifi.bio.watchdog.GUI.properties.views.executor;

import java.io.File;

/**
 * should be constructed using the static method defined in GUIExecutorView
 * @author kluge
 *
 */
public class LocalGUIExecutorView extends GUIExecutorView {

	@Override
	public String getName() {
		return "local executor";
	}

	@Override
	public String getFXMLResourceFilename() {
		return "executor" + File.separator + "LocalExecutorProperty.fxml";
	}
}
