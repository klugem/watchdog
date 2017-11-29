package de.lmu.ifi.bio.watchdog.GUI.helper;

import de.lmu.ifi.bio.watchdog.helper.returnType.BooleanReturnType;
import de.lmu.ifi.bio.watchdog.helper.returnType.ReturnType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;

/**
 * Used for separation of Parameter class that is used in cmd and GUI version from javafx dependencies
 * @author kluge
 *
 */
public class ParameterToControl {

	/**
	 * returns a GUI control element depending on the used return type
	 * @param r
	 * @return
	 */
	public static Control getControlElement(ReturnType r) {
		if(r instanceof BooleanReturnType) 
			return new CheckBox();
		else
			return new TextField();
	}
}

