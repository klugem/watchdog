package de.lmu.ifi.bio.watchdog.GUI.css;

import java.net.URISyntaxException;

/**
 * loads css files stored in this package based on their names
 * @author kluge
 *
 */
public class CSSRessourceLoader {

	public static String getCSS(String name) throws URISyntaxException {
		return CSSRessourceLoader.class.getResource(name).toURI().toString();
	}
}
