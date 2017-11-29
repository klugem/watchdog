package de.lmu.ifi.bio.watchdog.GUI.css;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;

/**
 * helps to format the GUI using CSS uniform
 * @author kluge
 *
 */
public class GUIFormat {

	public static void colorTextField(TextField f, boolean ok) {
		colorWithDefaultColors(f, ok);
	}
	
	public static void colorWithDefaultColors(Control c, boolean ok) {
		// try to remove old class
		c.getStyleClass().remove(getCssClassName(!ok));
		// add class if it is not there yet
		if(!c.getStyleClass().contains(getCssClassName(ok)))
			c.getStyleClass().add(getCssClassName(ok));
	}
	
	public static void clearColorTextField(TextField f) {
		f.getStyleClass().remove(getCssClassName(true));
		f.getStyleClass().remove(getCssClassName(false));
	}
	
	private static String getCssClassName(boolean ok) {
		return ok ? "ok" : "error";
	}

	public static void colorControl(Control c, Boolean ok) {
		// for checks, that are performed on non specific elements
		if(c == null || ok == null)
			return;
		
		if(c instanceof TextField)
			colorTextField((TextField) c, ok);
		else if(c instanceof ChoiceBox)
			colorWithDefaultColors(c, ok);
		else if(c instanceof CheckBox) {
			// do nothing but throw no error
		}
		else {
			System.err.println("Function colorControl in GUIFormat is not implemented for type '"+c.getClass().getSimpleName()+"'");
			System.exit(1);
		}
	}
}
