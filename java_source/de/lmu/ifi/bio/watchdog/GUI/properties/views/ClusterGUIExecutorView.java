package de.lmu.ifi.bio.watchdog.GUI.properties.views;

/**
 * should be constructed using the static method defined in GUIExecutorView
 * @author kluge
 *
 */
public class ClusterGUIExecutorView extends GUIExecutorView {

	@Override
	public String getName() {
		return "cluster executor";
	}

	@Override
	public String getFXMLResourceFilename() {
		return "ClusterExecutorProperty.fxml";
	}
}