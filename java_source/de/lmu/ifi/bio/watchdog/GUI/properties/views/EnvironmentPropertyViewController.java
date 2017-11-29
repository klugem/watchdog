package de.lmu.ifi.bio.watchdog.GUI.properties.views;


import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.MessageType;
import de.lmu.ifi.bio.watchdog.GUI.css.GUIFormat;
import de.lmu.ifi.bio.watchdog.GUI.helper.SuggestPopup;
import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class EnvironmentPropertyViewController extends PropertyViewController {

	@FXML private CheckBox copy;
	@FXML private TextField name;
	@FXML private Button addButton;
	@FXML private Button deleteEmptyButton;
	@FXML private CheckBox externalExport;
	@FXML private TextField exportHeader;
	@FXML private TextField exportCommand;
	@FXML private VBox envs;
	@FXML private Label l_name;
	@FXML private Label l_value;
	@FXML private Label l_copy;
	@FXML private Label l_update;
	@FXML private Label l_sep;
	@FXML private HBox label_box;
	@FXML private TitledPane advancedProp;
	@FXML private GridPane gridAdvanced;
	
	private ArrayList<TextField> store_name = new ArrayList<>();
	private ArrayList<TextField> store_value = new ArrayList<>();
	private ArrayList<TextField> store_sep = new ArrayList<>();
	private ArrayList<CheckBox> store_update = new ArrayList<>();
	private ArrayList<CheckBox> store_copy = new ArrayList<>();
	
	private Environment environmentStore;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);
		this.addButton.setDisable(true);
		this.deleteEmptyButton.setDisable(true);
		this.addButton.setGraphic(ImageLoader.getImage(ImageLoader.ADD_SMALL));
		this.deleteEmptyButton.setGraphic(ImageLoader.getImage(ImageLoader.DELETE_SMALL));
		
		// event handler
		this.addButton.setOnAction(event -> this.onAddVar());
		this.deleteEmptyButton.setOnAction(event -> this.deleteEmptyVars());
		this.externalExport.setOnAction(event -> this.onChangeExternalExport());
		this.name.textProperty().addListener(event -> this.updateName());
		this.advancedProp.expandedProperty().addListener(event -> this.onExpandChange());
		
		// add checker
		this.addValidateToControl(this.name, "name", f -> this.checkName((TextField) f));
		this.addValidateToControl(this.exportHeader,"exportHeader", f -> !this.externalExport.isSelected() || !this.isEmpty((TextField) f, "Shebang for external export command can not be empty."));
		this.addValidateToControl(this.exportCommand, "exportCommand", f -> validateExportCommand());
		this.addValidateToControl(null, null, e -> this.validateEnvironmentVariables());

		// add event handler for GUI validation
		this.name.textProperty().addListener(event -> this.validate());
		this.exportHeader.textProperty().addListener(event -> this.validate());
		this.exportCommand.textProperty().addListener(event -> this.validate());
		this.copy.selectedProperty().addListener(event -> this.validate());

		// call it once to get initial coloring
		this.validate();
	}

	/**
	 * deletes empty variables
	 */
	private void deleteEmptyVars() {
		ArrayList<Integer> deleteIds = new ArrayList<>();
		TextField t;
		
		// get the ones to delete
		for(int i = 0; i < this.store_name.size(); i++) {
			t = this.store_name.get(i);
			if(t.getText().isEmpty())
				deleteIds.add(i);
		}
		
		// finally, delete them
		int deleted = 0;
		int delID;
		for(int i : deleteIds) {
			delID = i - deleted;
			this.store_name.remove(delID);
			this.envs.getChildren().remove(delID+1);
			deleted++;
		}
		this.validate();
	}

	private boolean checkName(TextField f) {
		if(this.isEmpty((TextField) f, "Name for environment property is missing."))
				return false;
		if(!this.hasUniqueName(((TextField) f).getText()))
			return false;
		
		// all was ok!
		return true;
	}

	protected PropertyViewType getPropertyTypeName() {
		return PropertyViewType.ENVIRONMENT;
	}
	
	/**
	 * update the height of the view
	 */
	private void onExpandChange() {
		Stage s = (Stage) this.advancedProp.getScene().getWindow();
		if(this.advancedProp.isExpanded())
			s.setHeight(s.getHeight() + this.gridAdvanced.getHeight());
		else
			s.setHeight(s.getHeight() - this.gridAdvanced.getHeight());
	}
	
	private void updateName() {
		boolean s = this.name.getText().isEmpty(); 
		this.addButton.setDisable(s);
		if(!s && !this.isDuringLoadProcess() && this.store_name.size() == 0)
			this.onAddVar();
	}

	public Environment getStoredData() {
		return this.environmentStore;
	}

	private void onChangeExternalExport() {
		if(this.externalExport.isSelected()) {
			this.exportCommand.setDisable(false);
			this.exportHeader.setDisable(false);
		}
		else {
			this.exportCommand.setDisable(true);
			this.exportHeader.setDisable(true);
		}
		this.validate();
	}
	
	private void onAddVar() {
		this.onAddVar("", "", "", false, false);
	}

	private void onAddVar(String n, String v, String s, boolean c, boolean u) {		
		// create new line
		HBox hbox = new HBox();
		hbox.setMaxWidth(this.label_box.getMaxWidth());
		hbox.setPadding(this.label_box.getInsets());
		hbox.setSpacing(this.label_box.getSpacing());
		
		// create values
		TextField name = new TextField(n);
		TextField value = new TextField(v);
		@SuppressWarnings("unused") // magic inside ;)
		SuggestPopup popup = new SuggestPopup(value);
		TextField sep = new TextField(s);
		CheckBox copy = new CheckBox();
		CheckBox update = new CheckBox();
		update.selectedProperty().addListener(x -> this.activatedUpdateProperty(update, sep));
		copy.setSelected(c);
		update.setSelected(u);
		name.setPrefWidth(this.l_name.getPrefWidth());
		value.setPrefWidth(this.l_value.getPrefWidth());
		sep.setPrefWidth(this.l_sep.getPrefWidth());
		copy.setPrefWidth(this.l_copy.getPrefWidth());
		update.setPrefWidth(this.l_update.getPrefWidth());
		
		// add event handler
		name.textProperty().addListener(e -> this.validate());
		
		// store them
		this.store_name.add(name);
		this.store_value.add(value);
		this.store_sep.add(sep);
		this.store_copy.add(copy);
		this.store_update.add(update);
		
		// add them to line
		hbox.getChildren().addAll(name, value, copy, update, sep);
		this.envs.getChildren().add(hbox);
		
		// update stuff
		this.validate();
	}
	
	protected boolean validate() {
		boolean ret = super.validate();
		boolean change = !ret && this.envs.getChildren().size() > 1 && this.hasUniqueName(this.name.getText());
		this.deleteEmptyButton.setDisable(!change);
		return ret;
	}

	private void activatedUpdateProperty(CheckBox update, TextField sep) {
		if(update.isSelected() && sep.getText().length() == 0)
			sep.setText(Environment.DEFAULT_UPDATE_SEP);
	}

	@Override
	protected void saveData() {
		Environment e = new Environment(this.name.getText(), false, this.copy.isSelected(), this.externalExport.isSelected());
		// add external commands
		if(this.externalExport.isSelected()) {
			e.setShebang(this.exportHeader.getText());
			e.setCommand(this.exportCommand.getText());
		}
		// save the variables
		for(int i = 0; i < this.store_name.size(); i++)
			e.storeData(this.store_name.get(i).getText(), this.store_value.get(i).getText(), this.store_sep.get(i).getText(), this.store_copy.get(i).isSelected(), this.store_update.get(i).isSelected());
		
		// save the env.
		this.storeXMLData(e);
		super.saveData();
	}
	
	private boolean validateExportCommand() {
		if(this.externalExport.isSelected()) {
			if(!this.isEmpty(this.exportCommand, "External export command can not be empty.")) {
				if(!Environment.isCommandValid(this.exportCommand.getText())) {
					this.addMessageToPrivateLog(MessageType.ERROR, Environment.COMMAND_ERROR);	
					return false;
				}
			}
			else
				return false;
		}
		return true;
	}
	
	private boolean validateEnvironmentVariables() {
		boolean allOk = true;
		HashMap<String, Integer> usedNames = new HashMap<>();
		// check entries
		TextField t;
		for(int i = 0; i < this.store_name.size(); i++) {
			t = this.store_name.get(i);
			if(!this.isEmpty(t, i+1, this.l_name.getText())) {
				// check, if value is not used yet
				if(!usedNames.containsKey(t.getText())) {
					usedNames.put(t.getText(), i+1);
					GUIFormat.colorTextField(t, true);
				}
				else {
					int id = usedNames.get(t.getText());
					this.addMessageToPrivateLog(MessageType.ERROR, "Environment variable with name '"+t.getText()+"' was already defined before in row '"+id+"'.");
					GUIFormat.colorTextField(t, false);
					allOk = false;
				}
			}
			else
				allOk = false;
		}
		// check, if at least one entry is there
		if(this.store_name.size() == 0 && !this.copy.isSelected()) {
			this.addMessageToPrivateLog(MessageType.ERROR, "At least one variable must be defined in each environment.");
			allOk = false;
		}
		return allOk;
	}
	
	private void storeXMLData(Environment data) {
		this.environmentStore = data;
		XMLDataStore.registerData(data);
	}

	public void loadData(Environment data) {
		if(data != null) {
			// unregister that data or otherwise name will be blocked!
			XMLDataStore.unregisterData(data);
			this.isDataLoaded = true;
			
			// set basic settings
			this.name.setText(data.getName());
			this.copy.setSelected(data.isCopyLocalValues());
			
			// load advanced settings
			if(data.useExternalCommand()) {
				this.externalExport.setSelected(true);
				this.exportCommand.setText(data.getExternalCommand());
				this.exportHeader.setText(data.getShebang());
				this.onChangeExternalExport();
			}
			
			// load all the parameters
			for(Object[] d : data.getStoredData())
				this.onAddVar((String) d[0], (String) d[1], (String) d[2], (boolean) d[3],(boolean) d[4]);
			
			// load the data
			this.environmentStore = data;
			this.isDataLoaded = false;
		}
	}

	@Override
	protected boolean hasUniqueName(String name) {
		if(name == null || name.length() == 0)
			return false;
		if(!XMLDataStore.hasRegistedData(name, Environment.class))
			return true;
		else {
			this.addMessageToPrivateLog(MessageType.ERROR, "An environment property with name '"+name+"' exists already.");
			return false;
		}
	}
}
