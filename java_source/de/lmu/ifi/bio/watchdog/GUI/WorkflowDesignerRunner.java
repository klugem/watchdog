package de.lmu.ifi.bio.watchdog.GUI;

import java.io.IOException;
import java.net.URISyntaxException;

import org.xml.sax.SAXException;

import de.lmu.ifi.bio.multithreading.TimedExecution;
import de.lmu.ifi.bio.watchdog.GUI.css.CSSRessourceLoader;
import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import de.lmu.ifi.bio.watchdog.GUI.helper.AddButtonToTitledPane;
import de.lmu.ifi.bio.watchdog.GUI.helper.CurrentScreen;
import de.lmu.ifi.bio.watchdog.GUI.helper.PreferencesStore;
import de.lmu.ifi.bio.watchdog.GUI.helper.ScreenCenteredStage;
import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Runner application
 * @author kluge
 *
 */
public class WorkflowDesignerRunner extends Application {
	
	private static final String DISABLE_LOAD_SCREEN = "-disableLoadScreen";
	public static final int GRID_SIZE_X = 120;
	public static final int GRID_SIZE_Y = 80;
	public static final int MAX_EXTEND_DIST = 1;
	public static final int MIN_HEIGHT = 578;
	public static final int MIN_WIDTH = 911;
	public static final String GENERAL_NAME = "General";
	protected static final String MAIN_TITLE = "Watchdog's workflow designer";
    
	protected static BooleanProperty isLoaded = new SimpleBooleanProperty(false);
	private static boolean isGUIRunning = false;
	
	/**
	 * starts the GUI
	 * @param args
	 */
	public static void main(String[] args) {
		XMLParser.setNoExitInCaseOfError(true); // do not exit in case of errors!
		isGUIRunning = true;
		launch(args);
	} 
	
	public static boolean isGUIRunning() {
		return isGUIRunning;
	}
	
	/**
	 * in order to avoid import from javafx.* packages in non GUI version
	 * @param ex
	 */
	private void initRunableExecutor() {
		Logger.setRunnableExecutor(Platform::runLater);
		TimedExecution.setRunnableExecutor(Platform::runLater);
	}
 
	@Override
	public void start(Stage primaryStage) {
		try {			
			// set javafx Platform executor for classes that require that
			this.initRunableExecutor();
						
			// load settings from ini file
			PreferencesStore.loadSettingsFromFile(PreferencesStore.defaultIniFile);
		    Parameters p = this.getParameters();
		    HostServices hs = this.getHostServices();
		    Launch launch = null;
		    
			// will be executed only once --> init plugins
			XMLParser.initPlugins(PreferencesStore.getWatchdogBaseDir(), new Logger(), true, true);
		    
		    // get current screen where window should be spawned
		    Screen curScreen = CurrentScreen.getScreenWithMouseOnIt();
		    
		    // test if launch screen is enabled
			if(!p.getUnnamed().contains(DISABLE_LOAD_SCREEN)) {
				// show launch view
				Stage stage = new ScreenCenteredStage();
				stage.initStyle(StageStyle.UNDECORATED);
				launch = Launch.getLaunch(stage);
				Scene sceneL = new Scene(launch);
				sceneL.getStylesheets().add(CSSRessourceLoader.getCSS("control.css"));
				stage.setScene(sceneL);

				// select screen and center it there
				CurrentScreen.centerOnScreen(stage, curScreen);
				
				Thread t = new Thread(launch);
				Platform.runLater(() -> t.start());
			}
			Runnable startMainWindow = () -> { try { this.loadApplication(hs, primaryStage, curScreen); } catch(Exception e) { e.printStackTrace(); System.exit(1);}};
			if(launch != null)
				launch.setRunable(() -> WorkflowDesignController.enforcePreferences(startMainWindow));
			// show it 
			if(p.getUnnamed().contains(DISABLE_LOAD_SCREEN)) {
				WorkflowDesignController.enforcePreferences(startMainWindow);
			}			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private WorkflowDesignController loadApplication(HostServices hs, Stage primaryStage, Screen screen) throws URISyntaxException, IOException, SAXException {
		// load interface
		FXMLLoader loader = new FXMLLoader(FXMLRessourceLoader.class.getResource("WorkflowDesigner.fxml"));
		BorderPane page = (BorderPane) loader.load();
		WorkflowDesignController mainController = loader.getController();
		mainController.setHostService(hs);
		
		// build the page
        Scene scene = new Scene(page);
        // add css stuff
        scene.getStylesheets().add(CSSRessourceLoader.getCSS("control.css"));
        primaryStage.setTitle(mainController.getTitle());
        primaryStage.getIcons().add(ImageLoader.getImage(ImageLoader.WATCHDOG_ICON_SMALL).getImage());
        primaryStage.setScene(scene);

        primaryStage.setMinHeight(MIN_HEIGHT);
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setWidth(PreferencesStore.getWidth());
        primaryStage.setHeight(PreferencesStore.getWidth());
        
		// select screen and center it there
		CurrentScreen.centerOnScreen(primaryStage, screen);
        
        if(PreferencesStore.isFullScreenMode())
        	primaryStage.setMaximized(true);

        // forward on close action
        primaryStage.setOnCloseRequest(event -> mainController.onClose(event));
        
        // add callback to get new window position if window was moved
        primaryStage.xProperty().addListener((a, b, c) -> CurrentScreen.updateLastActiveScreen(primaryStage));
        primaryStage.yProperty().addListener((a, b, c) -> CurrentScreen.updateLastActiveScreen(primaryStage));
        primaryStage.widthProperty().addListener((a, b, c) -> CurrentScreen.updateLastActiveScreen(primaryStage));
        primaryStage.heightProperty().addListener((a, b, c) -> CurrentScreen.updateLastActiveScreen(primaryStage));

        // init the grid
		mainController.initGrid(GRID_SIZE_X, GRID_SIZE_Y, MAX_EXTEND_DIST);
		
		// show the window
		if(primaryStage.getScene() != null)
			primaryStage.show();
		AddButtonToTitledPane.initAddButtonsAsGUIisLoaded();
		WorkflowDesignerRunner.isLoaded.set(true);
		
		return mainController;
	}
	
	@Override
	public void stop() {
		System.out.flush();
		System.err.flush();
	}

	
	public static void addListenerOnLoadReady(ChangeListener<? super Object> listener) {
		WorkflowDesignerRunner.isLoaded.addListener(listener);
	}
}
