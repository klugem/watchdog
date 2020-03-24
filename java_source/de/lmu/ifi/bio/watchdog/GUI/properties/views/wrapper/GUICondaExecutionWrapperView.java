package de.lmu.ifi.bio.watchdog.GUI.properties.views.wrapper;

import java.io.File;

/**
 * @author kluge
 *
 */
public class GUICondaExecutionWrapperView extends GUIExecutionWrapper {

	@Override
	public String getName() {
		return "conda wrapper";
	}

	@Override
	public String getFXMLResourceFilename() {
		return "wrapper" + File.separator + "CondaWrapperProperty.fxml";
	}
}
