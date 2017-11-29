package de.lmu.ifi.bio.watchdog.GUI.helper;

import javafx.stage.Screen;
import javafx.stage.Stage;

public class ScreenCenteredStage extends Stage {
	
	public ScreenCenteredStage() {
		Screen s = CurrentScreen.getActiveScreen();
		CurrentScreen.centerOnScreen(this, s);
	}
}
