package de.lmu.ifi.bio.watchdog.GUI;

import java.lang.reflect.Method;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Launch extends Pane implements Runnable {

	private static final String REQUIRED_FX_VERSION = "8.0.65";
	private LaunchController controller;
	private Stage secondaryStage;
	private Runnable enforcePreferences; 
	
	/** hide constructor */
	private Launch() {}

	public static Launch getLaunch(Stage secondaryStage) {
		
		try {
			// checking the version JavaFX API - print warning if not supported
		    String installedFXVersion = System.getProperty("javafx.runtime.version");
	        String nsVersion = REQUIRED_FX_VERSION.substring(REQUIRED_FX_VERSION.lastIndexOf("/") + 1);
	        Method m = FXMLLoader.class.getDeclaredMethod("compareJFXVersions", String.class, String.class);
	        m.setAccessible(true);
	        Object ret = m.invoke(null, new Object[] {installedFXVersion, nsVersion});
	        if (ret == null) {
	        	System.out.println("[WARN] Failed to check installed javaFX version.");
	        }
	        else if((int) ret < 0) {
	        	System.out.println("[ERROR] Installed javaFX Version ('"+installedFXVersion+"') is smaller than required version '" + REQUIRED_FX_VERSION + "'.");
	        	System.exit(1);
	        }
		}
		catch(Exception e) {
			e.printStackTrace();
			System.out.println("[WARN] Failed to check installed javaFX version.");
		}
        
	   try {
			FXMLRessourceLoader<Launch, LaunchController> l = new FXMLRessourceLoader<>("Launch.fxml", new Launch());
			Pair<Launch, LaunchController> pair = l.getNodeAndController();
			Launch p = pair.getKey();;
			p.secondaryStage = secondaryStage;
			p.controller = pair.getValue();		
			return p;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void run() {
		Platform.runLater(() -> this.secondaryStage.show());
		this.controller.run();
		// wait a short time and close the window afterwards
		try { Thread.sleep(25); } catch(Exception e) {}
		Platform.runLater(() -> this.switchWindows());
	}
	
	private void switchWindows() {
		this.getScene().getWindow().hide();
		
		// enforce preferences if required
		this.enforcePreferences.run();
	}

	public void setRunable(Runnable r) {
		this.enforcePreferences = r;
	}
}