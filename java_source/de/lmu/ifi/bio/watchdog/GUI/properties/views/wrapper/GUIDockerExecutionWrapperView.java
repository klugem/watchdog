package de.lmu.ifi.bio.watchdog.GUI.properties.views.wrapper;

import java.io.File;

/**
 * @author kluge
 *
 */
public class GUIDockerExecutionWrapperView extends GUIExecutionWrapper {

	@Override
	public String getName() {
		return "docker wrapper";
	}

	@Override
	public String getFXMLResourceFilename() {
		return "wrapper" + File.separator + "DockerWrapperProperty.fxml";
	}
}
