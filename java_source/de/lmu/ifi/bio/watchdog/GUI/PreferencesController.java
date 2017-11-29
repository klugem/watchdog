package de.lmu.ifi.bio.watchdog.GUI;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.AdditionalBarController;
import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.StatusConsole;
import de.lmu.ifi.bio.watchdog.GUI.event.ToolLibraryUpdateEvent;
import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import de.lmu.ifi.bio.watchdog.GUI.helper.Inform;
import de.lmu.ifi.bio.watchdog.GUI.helper.PreferencesStore;
import de.lmu.ifi.bio.watchdog.GUI.helper.SaveablePane;
import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import de.lmu.ifi.bio.watchdog.GUI.preferences.AbstractPreferencesController;
import de.lmu.ifi.bio.watchdog.helper.PatternFilenameFilter;
import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValueBase;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Pair;

public class PreferencesController implements Initializable {
	
	private static final String PREFERENCES_SUB_FOLDER = "preferences";
	private static final String SHOW_AT_START_CAT = "General";
	private static final String FXML_PATTERN = "*.fxml";
	private static final String ENDING_REPLACE = "\\.fxml$";

	@FXML private Button saveButtonPref;
	@FXML private Button discardButton;
	@FXML private ScrollPane scroll;
	@FXML private BorderPane root;
	@FXML private TreeTableColumn<String, String> categories; 
	@FXML private TreeTableView<String> categorySelect;
	
	private ToolLibraryController toolLibController;	
	private boolean closeOnFirstSave = false;
	private HashMap<String, SaveablePane> PANES = new HashMap<>();
	private static final String STATUS_CONSOLE_NAME = "Preferences";
	private StatusConsole status = StatusConsole.getStatusConsole(STATUS_CONSOLE_NAME);
	private AdditionalBarController additionalBarController;
	private SimpleBooleanProperty canSave = new SimpleBooleanProperty(false);
	private SimpleBooleanProperty hasUnsavedData = new SimpleBooleanProperty(false);
	
	private final HashSet<String> notSaveable = new HashSet<>();
	private final HashSet<String> unsavedData = new HashSet<>();
			
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// init the categories
		this.categories.setCellValueFactory((CellDataFeatures<String, String> p) -> new PreferenceViewProperty(p.getValue().getValue()));

		// get the FXML files that can be loaded
		TreeItem<String> tree = new TreeItem<>();
		boolean first = true;
		
		URL fxmlFolder = FXMLRessourceLoader.class.getResource(PreferencesController.PREFERENCES_SUB_FOLDER);
		File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
		try {
			if(jarFile.isFile()) { 
			    JarFile jar = new JarFile(jarFile);
			    Enumeration<JarEntry> entries = jar.entries();
			    while(entries.hasMoreElements()) {
			        String name = entries.nextElement().getName();
			        if (name.matches(".*" + File.separator + PreferencesController.PREFERENCES_SUB_FOLDER + File.separator + ".*" + "\\.fxml")) {
			            String[] tmp = name.split(File.separator);
			            this.addPreferencePage(tmp[tmp.length-1], first, tree);
			            first = false;
			        }
			    }
			    jar.close();
			} else {
				File fxmlFile =	Paths.get(fxmlFolder.toURI()).toFile();
				for(File f : fxmlFile.listFiles(new PatternFilenameFilter(FXML_PATTERN, false))) {
					this.addPreferencePage(f.getName(), first, tree);
					first = false;
				}
			}
		}
		catch(Exception e) { e.printStackTrace(); }

		// set the settings
		this.categorySelect.setRoot(tree);
		this.categorySelect.getStyleClass().add("noHorScrollBar");
				
		// set images to buttons
		this.saveButtonPref.setGraphic(ImageLoader.getImage(ImageLoader.SAVE_SMALL));
		this.discardButton.setGraphic(ImageLoader.getImage(ImageLoader.DELETE_SMALL));
				
		// add event handler
		this.discardButton.onActionProperty().set(event -> { this.loadSettings(); event.consume();});
		this.saveButtonPref.onActionProperty().set(event -> { this.saveSettings(); event.consume();});
		this.categorySelect.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> this.changeSelect(newSelection.getValue()));
		
		// add internal event to listen to 
		this.root.addEventHandler(ToolLibraryUpdateEvent.TOOL_LIB_EVENT_TYPE, event -> { if(this.toolLibController != null) this.toolLibController.loadModuleLibrary(); });
		
		// listener for save button
		this.saveButtonPref.setDisable(true);
		this.canSave.addListener((x,y,z) -> this.saveButtonPref.setDisable(!(this.hasUnsavedData.get() && z)));
		this.hasUnsavedData.addListener((x,y,z) -> this.saveButtonPref.setDisable(!(this.canSave.get() && z)));
	}
	
	/**
	 * adds a preference page
	 * @param fullName
	 * @param first
	 * @param tree
	 */
	private void addPreferencePage(String fullName, boolean first, TreeItem<String> tree) {
		final String name = fullName.replaceFirst(ENDING_REPLACE, "");
		tree.getChildren().add(new TreeItem<>(name));
		SaveablePane s = getPreferenceSetting(fullName, this.status);
		s.setParentPaneForEvents(this.root);
		this.PANES.put(name, s);
		
		// set content of first setting 
		if(first && !this.closeOnFirstSave || name.equals(SHOW_AT_START_CAT))
			this.changeSelect(name);
		
		// update the save button indicator
		s.getValidatedDataBoolean().addListener((x, y, z) -> { if(z.booleanValue()) this.notSaveable.remove(name); else this.notSaveable.add(name); this.canSave.set(this.notSaveable.size() == 0);});
		s.getUnsavedDataBoolean().addListener((x, y, z) -> { if(!z.booleanValue()) this.unsavedData.remove(name); else this.unsavedData.add(name); this.hasUnsavedData.set(this.unsavedData.size() > 0);});
	}

	protected void onClose(Stage stage) {
		if(this.hasUnsavedData.getValue()) {
			// ask, if the new settings should be written to a config file
			Optional<ButtonType> op = Inform.confirm("If you proceed your unsaved changes will be lost.\n\nDo you really want to proceed?");
			if(op.get() == ButtonType.OK) {
				if(this.additionalBarController != null && this.status != null) {
					this.additionalBarController.removeTab(this.status.getName());
					this.status = null; // let the GC eat it
				}
				stage.close();
			}
		}
		else {
			if(this.additionalBarController != null && this.status != null) {
				this.additionalBarController.removeTab(this.status.getName());
				this.status = null; // let the GC eat it
			}
			stage.close();
		}
	}

	/**
	 * loads the correct Pane in the center window of the pref. scene
	 * @param name
	 */
	protected void changeSelect(String name) {
		if(this.PANES.containsKey(name))
			this.scroll.setContent(this.PANES.get(name));
	}

	private void loadSettings() {
		PreferencesStore.loadSettingsFromFile(PreferencesStore.defaultIniFile);
		// load all the settings
		for(SaveablePane s : this.PANES.values()) {
			s.load();
		}
	}

	private void saveSettings() {
		// save all the settings
		for(SaveablePane s : this.PANES.values()) {
			s.save();
		}
		// store it in the ini
		PreferencesStore.saveSettingsToFile(PreferencesStore.defaultIniFile);
		if(this.closeOnFirstSave) {
			this.root.getScene().getWindow().hide();
		}
		// clear the stuff or bug!
		this.unsavedData.clear();
		this.notSaveable.clear();
		this.saveButtonPref.setDisable(true);
	}
	

	/**
	 * loads a FXML file stored in the preferences sub directory
	 * @param filename
	 * @return
	 */
	public static SaveablePane getPreferenceSetting(String filename, StatusConsole status) {
		try {
			SaveablePane sp = new SaveablePane();
			FXMLRessourceLoader<SaveablePane, AbstractPreferencesController> l = new FXMLRessourceLoader<>(PreferencesController.PREFERENCES_SUB_FOLDER + File.separator + filename, sp);
			Pair<SaveablePane, AbstractPreferencesController> p = l.getNodeAndController();
			AbstractPreferencesController c = p.getValue();
			sp.setSaveController(c);
			c.setStatusConsole(status);
			return p.getKey();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	private class PreferenceViewProperty extends ObservableValueBase<String> {
		
		private final String STR;
		private PreferenceViewProperty(String str) {
			this.STR = str;
		}

		@Override
		public String getValue() {
			return this.STR;
		}
	}

	public void setToolLibrary(ToolLibraryController tlc) {
		this.toolLibController = tlc;
	}

	public void setCloseOnFirstSave(boolean cofs) {
		this.closeOnFirstSave = cofs;
	}

	public void setAdditionalToolbar(AdditionalBarController additionalBarController) {
		if(additionalBarController != null) {
			this.additionalBarController = additionalBarController;
			// add status console
			this.additionalBarController.addNewTab(this.status);
		}
	}
}
