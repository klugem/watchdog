package de.lmu.ifi.bio.watchdog.GUI.interfaces;

import javafx.scene.control.Control;

/**
 * validator for a java fx control
 * @author kluge
 *
 * @param <T>
 */
public interface Validator<T extends Control> {

	/**
	 * true, if control is valid
	 * @return
	 */
	public abstract boolean validate(T control);
}
