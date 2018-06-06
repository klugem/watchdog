package de.lmu.ifi.bio.watchdog.GUI.preferences;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.ToolLibraryController;
import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.MessageType;
import de.lmu.ifi.bio.watchdog.GUI.event.ToolLibraryUpdateEvent;
import de.lmu.ifi.bio.watchdog.GUI.event.WatchdogBaseDirUpdateEvent;
import de.lmu.ifi.bio.watchdog.GUI.helper.Inform;
import de.lmu.ifi.bio.watchdog.GUI.helper.PreferencesStore;
import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;

public class ModulesPreferencesController extends AbstractPreferencesController {

	private final static String SELECT_DIR = "select module library directory for Watchdog";
	
	@FXML private BorderPane root;
	@FXML private Button add;
	@FXML private Button remove;
	@FXML private TableView<SimpleModuleDir> table;
	@FXML private TableColumn<SimpleModuleDir, String> dirname;
	@FXML private TableColumn<SimpleModuleDir, String> dirs;
	@FXML private TextField name;
	
	private final LinkedHashMap<String, String> MOD_DIRS = new LinkedHashMap<>();
	private final ObservableList<SimpleModuleDir> MOD_OBSERV_DIRS = FXCollections.observableArrayList();
	private final HashMap<String, ArrayList<String>> MODULE_NAMES_IN_FOLDER = new HashMap<>();
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// init table
		this.dirname.setCellValueFactory(new PropertyValueFactory<SimpleModuleDir, String>("Name"));
		this.dirs.setCellValueFactory(new PropertyValueFactory<SimpleModuleDir, String>("Dir"));
		this.table.setItems(this.MOD_OBSERV_DIRS);

		// add validate stuff
		this.addValidateToControl(this.name, "name", f -> this.isNameValid());
		
		this.add.setOnMouseClicked(e -> this.selectModuleFolder());
		this.remove.setOnMouseClicked(e -> this.removeFolder());
		
		// add validation event
		this.name.textProperty().addListener(e -> this.validate());
		
		// add fancy images
		this.add.setGraphic(ImageLoader.getImage(ImageLoader.ZOOM_SMALL));
		this.remove.setGraphic(ImageLoader.getImage(ImageLoader.DELETE_SMALL));
		
		// get initial coloring
		super.initialize(location, resources);
	}
	
	@Override
	public void recieveEventFromSiblingPages(Event e) {
		if(e instanceof WatchdogBaseDirUpdateEvent) {
			WatchdogBaseDirUpdateEvent ew = (WatchdogBaseDirUpdateEvent) e;
			this.updateDefaultModule(ew.getNewBaseDir());
		}
	}
	
	private void updateDefaultModule(String newBaseDir) {
		this.MOD_DIRS.putAll(PreferencesStore.getMouleFolders());
		this.updateGUI();
		// send the event to update module lib
		Parent target = this.getParentToSendEvents();
		ToolLibraryUpdateEvent event = new ToolLibraryUpdateEvent();
		target.fireEvent(event);
	}

	private void removeFolder() {
		String name = this.table.getSelectionModel().selectedItemProperty().getValue().getName();
		this.removeFolder(name);
	}

	private boolean isNameValid() {
		// check, if name is already in use
		return !this.MOD_DIRS.containsKey(this.name.getText());
	}

	/**
	 * is called when the user want to select a new module folder
	 */
	private void selectModuleFolder() {
		DirectoryChooser d = new DirectoryChooser();
		d.setTitle(SELECT_DIR);
		File dir = d.showDialog(this.add.getScene().getWindow());
		
		// check, if ja dir with modules was selected
		if(dir != null) {
			ArrayList<String> moduleFolders = new ArrayList<>();
			String dd = dir.getAbsolutePath();
			if(dd.endsWith(File.separator))
				dd = dd.replaceFirst(File.separator + "$", "");
			dd = dd.replaceAll(File.separator + "{2,}", File.separator);
			moduleFolders.add(dd);
			HashMap<String, String> m = ToolLibraryController.getNamesOfModules(moduleFolders);
			if(m.size() == 0) {
				Optional<ButtonType> o = Inform.confirm("No module was found in the folder you selected. Do you want to add this folder anyway?");
				if (o.get() == ButtonType.OK){
					this.addFolder(dir, m);
				} 
			}
			else 
				this.addFolder(dir, m);
		}
	}
	
	public boolean validate() {
		boolean ret = super.validate();
		this.add.setDisable(!ret || this.name.getText().replace(" ", "").length() == 0);
		this.remove.setDisable(this.MOD_DIRS.size() == 0);
		return ret;
	}
	
	private boolean checkModuleNameConflict(HashSet<String> newNames) {
		HashSet<String> allNames = new HashSet<>(newNames);
		for(ArrayList<String> names : this.MODULE_NAMES_IN_FOLDER.values()) {
			for(String n : names) {
				if(!allNames.contains(n))
					allNames.add(n);
				else {
					this.addMessageToPrivateLog(MessageType.ERROR, "A module with name '"+n+"' is already stored in another module folder.");
					return true;
				}
			}
		}
		return false;
	}

	private void addFolder(File dir, HashMap<String, String> modules) {
		if(!this.checkModuleNameConflict(new HashSet<>(modules.values()))) {
			String name = this.name.getText();
			this.MOD_DIRS.put(name, dir.getAbsolutePath() + File.separator);
			this.updateGUI();
			this.name.setText("");
			this.validate();
			
			// add the module names
			this.MODULE_NAMES_IN_FOLDER.put(name, new ArrayList<String>(modules.values()));
			this.hasUnsavedData.set(false);
			this.hasUnsavedData.set(true);
		}
		else {
			Inform.inform("Module folder can not be added because of duplicated modules names.");
		}
	}
	
	private void removeFolder(String name) {
		if(this.MOD_DIRS.remove(name) != null) {
			this.MODULE_NAMES_IN_FOLDER.remove(name);
			this.updateGUI();
			this.hasUnsavedData.set(false);
			this.hasUnsavedData.set(true);
		}
	}

	@Override
	public void onSave() {
		boolean change = PreferencesStore.setModuleDirectories(this.MOD_DIRS);
		super.onSave();
		
		if(change) {
			// send the event
			Parent target = this.getParentToSendEvents();
			ToolLibraryUpdateEvent event = new ToolLibraryUpdateEvent();
			target.fireEvent(event);
		}
	}

	
	@Override
	public void onLoad() {
		this.MOD_DIRS.putAll(PreferencesStore.getMouleFolders());
		this.updateGUI();
		super.onLoad();
	}

	private void updateGUI() {
		this.MOD_OBSERV_DIRS.clear();
		for(String key : this.MOD_DIRS.keySet()) {
			this.MOD_OBSERV_DIRS.add(new SimpleModuleDir(key, this.MOD_DIRS.get(key)));
		}
	}
	
	public class SimpleModuleDir {			 
		private final SimpleStringProperty NAME;
		private final SimpleStringProperty DIR;
		
		public SimpleModuleDir(String name, String dir) {
			this.NAME = new SimpleStringProperty(name);
			this.DIR = new SimpleStringProperty(dir);
		}
		
		public String getName() {
            return this.NAME.get();
        }
 
        public void setName(String name) {
        	this.NAME.set(name);
        }
        
		public String getDir() {
            return this.DIR.get();
        }
 
        public void setDir(String dir) {
        	this.DIR.set(dir);
        }
	}
}