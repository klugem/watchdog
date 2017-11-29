package de.lmu.ifi.bio.watchdog.GUI.helper;

import java.awt.MouseInfo;
import java.awt.Point;

import javafx.geometry.Rectangle2D;
import javafx.scene.control.Dialog;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class CurrentScreen {
	
	private static Screen lastActiveScreen = null;
	
	/**
	 * returns the screen with the point on it
	 * @param p
	 * @return
	 */
	private static Screen getScreenWithPointOnIt(Point p) {
		// if there is only one screen, we have the answer ;)
		if(Screen.getScreens().size() == 1) {
			CurrentScreen.lastActiveScreen = Screen.getScreens().get(0);
			return Screen.getScreens().get(0);
		}
		
		for(Screen s : Screen.getScreens()) {
			Rectangle2D r = s.getBounds();
			if(r.getMinX() <= p.getX() && r.getMinY() <= p.getY() && r.getMaxX() > p.getX() && r.getMaxY() > p.getY()) {
				CurrentScreen.lastActiveScreen = s;
				return s;
			}
		}		
		// we did not found it, return main screen instead
		CurrentScreen.lastActiveScreen = Screen.getPrimary();
		return Screen.getPrimary();
	}

	/**
	 * returns the screen with the mouse on it
	 * @return
	 */
	public static Screen getScreenWithMouseOnIt() {
		// try to find the screen based on the mouse position
		Point p = MouseInfo.getPointerInfo().getLocation();
		return 	CurrentScreen.getScreenWithPointOnIt(p);
	}
	
	/**
	 * centers the stage on the given screen
	 * @param stage
	 * @param curScreen
	 */
	public static void centerOnScreen(Stage stage, Screen curScreen) {
		stage.setX(curScreen.getBounds().getMinX() + 10); // give it a little bit of buffer
		stage.setY(curScreen.getBounds().getMinY() + 10);
		stage.centerOnScreen();
	}
	
	
	/**
	 * centers the stage on the given screen
	 * @param stage
	 * @param curScreen
	 */
	public static void centerOnScreen(Dialog<?> d, Screen curScreen) {
		d.setX(curScreen.getBounds().getMinX() + 10); // give it a little bit of buffer
		d.setY(curScreen.getBounds().getMinY() + 10);
		d.getDialogPane().getScene().getWindow().centerOnScreen();
	}

	public static Screen getActiveScreen() {
		if(CurrentScreen.lastActiveScreen != null)
			return CurrentScreen.lastActiveScreen;
		else
			return CurrentScreen.getScreenWithMouseOnIt();
	}

	/**
	 * updates the active screen
	 * @param primaryStage
	 */
	public static void updateLastActiveScreen(Stage primaryStage) {
		double x = primaryStage.getX() + (primaryStage.getWidth() / 2.0);
		double y = primaryStage.getY() + (primaryStage.getHeight() / 2.0);
		Point p = new Point();
		p.setLocation(x, y);

		CurrentScreen.lastActiveScreen = getScreenWithPointOnIt(p);
	}
}
