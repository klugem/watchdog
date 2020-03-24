package de.lmu.ifi.bio.watchdog.GUI.properties.views.executor;

import java.util.ArrayList;
import java.util.HashMap;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.MessageType;
import de.lmu.ifi.bio.watchdog.GUI.css.GUIFormat;
import de.lmu.ifi.bio.watchdog.GUI.helper.PreferencesStore;
import de.lmu.ifi.bio.watchdog.GUI.helper.SuggestPopup;
import de.lmu.ifi.bio.watchdog.GUI.helper.TextFilter;
import de.lmu.ifi.bio.watchdog.GUI.properties.PropertyManager;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginPropertyViewController;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PropertyViewType;
import de.lmu.ifi.bio.watchdog.executionWrapper.ExecutionWrapper;
import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import de.lmu.ifi.bio.watchdog.xmlParser.plugins.executorParser.XMLExecutorInfoParser;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ExecutorPropertyViewController extends PluginPropertyViewController<ExecutorInfo> {

	@FXML private CheckBox useAsDefault;
	@FXML private TitledPane advancedPane;
	@FXML private CheckBox isSlaveMode;
	@FXML private TextField workingDir;
	@FXML private TextField javaPath;
	@FXML private TextField maxRunning;
	@FXML private TextField maxSlaveRunning;
	@FXML private VBox parentBox;
	@FXML private BorderPane root;
	@FXML private ChoiceBox<Environment> environment;
	@FXML private TextField shebangHeader;
	@FXML private TextField beforeScripts;
	@FXML private TextField afterScripts;
	@FXML private TextField packageManagers;
	@FXML private TextField container;
	
	private static final Environment NOT_SELECT;
	
	static { 
		NOT_SELECT = new Environment("-- no environment set --", false, false);
	}
			
	/**
	 * update the height of the view
	 */
	private void onExpandChange() {
		Stage s = (Stage) this.advancedPane.getScene().getWindow();
		double value = ((Region) this.advancedPane.getContent()).getHeight();
		if(this.advancedPane.isExpanded()) {
			this.root.setMinHeight(this.root.getHeight() + value);
			this.root.setPrefHeight(this.root.getHeight() + value);
			this.root.setMaxHeight(this.root.getHeight() + value);
			this.parentBox.setMinHeight(this.parentBox.getHeight() + value);
			this.parentBox.setPrefHeight(this.parentBox.getHeight() + value);
			this.parentBox.setMaxHeight(this.parentBox.getHeight() + value);
			s.setHeight(s.getHeight() + value);
		}
		else {
			this.root.setMinHeight(this.root.getHeight() - value);
			this.root.setPrefHeight(this.root.getHeight() - value);
			this.root.setMaxHeight(this.root.getHeight() - value);
			this.parentBox.setMinHeight(this.parentBox.getHeight() - value);
			this.parentBox.setPrefHeight(this.parentBox.getHeight() - value);
			this.parentBox.setMaxHeight(this.parentBox.getHeight() - value);
			s.setHeight(s.getHeight() - value);
		}
	}

	@Override
	protected PropertyViewType getPropertyTypeName() {
		return PropertyViewType.EXECUTOR;
	}
	
	@Override
	protected void saveData() {
		// get data common to all executors
		String name = this.name.getText();
		boolean isDefault = this.useAsDefault.isSelected();
		// get advanced settings
		boolean isStick2Host = this.isSlaveMode.isSelected();

		String path2java = this.javaPath.getText();
		int maxRunning = Integer.parseInt(this.maxRunning.getText());
		String workingDir = this.workingDir.getText();
		String shebang = this.shebangHeader.getText();
		String container = this.container.getText();
		
		// read max slave running property
		Integer maxSlaveRunning = null;
		if(isStick2Host)
			maxSlaveRunning = Integer.parseInt(this.maxSlaveRunning.getText());
		
		String watchdogBaseDir = PreferencesStore.getWatchdogBaseDir();
		Environment environment = this.getSelectedEnvironment();
		ArrayList<String> beforeScripts = XMLParser.splitString(this.beforeScripts.getText(), XMLParser.SCRIPTS_SEP);
		ArrayList<String> afterScripts = XMLParser.splitString(this.afterScripts.getText(), XMLParser.SCRIPTS_SEP);
		ArrayList<String> wrappers = XMLParser.splitString(this.packageManagers.getText(), XMLParser.WRAPPERS_SEP);
		
		// get the executor info
		ExecutorInfo e = this.activeGUIView.getXMLPluginObject(new Object[] {name, isDefault, isStick2Host, maxSlaveRunning, path2java, maxRunning, watchdogBaseDir, environment, workingDir, shebang, beforeScripts, afterScripts, wrappers, container});
		
		// save the executor.
		this.storeXMLData(e);
		super.saveData();
	}
	
	private Environment getSelectedEnvironment() {
		// the dummy element is selected
		if(this.environment.getSelectionModel().getSelectedIndex() == 0)
			return null;
		return this.environment.getSelectionModel().getSelectedItem();
	}
	
	@Override
	protected void initGUIElements() {
		// type enforcer
		this.maxRunning.setTextFormatter(TextFilter.getIntFormater());
		this.maxSlaveRunning.setTextFormatter(TextFilter.getIntFormater());
		
		// init env
		PropertyManager manager = PropertyManager.getPropertyManager(PropertyViewType.ENVIRONMENT);
		ArrayList<Environment> names = new ArrayList<>();
		for(XMLDataStore d : manager.getXMLData()) {
			names.add((Environment) d);
		}

		// disable it, if no environments are there
		if(names.size() == 0)
			this.environment.setDisable(true);
		else {
			names.add(0, NOT_SELECT);
			this.environment.setItems(FXCollections.observableArrayList(names));
			this.environment.getSelectionModel().select(0);
		}
		
		// event handler
		this.advancedPane.expandedProperty().addListener(event -> this.onExpandChange());
		this.isSlaveMode.setOnAction(x -> this.javaPath.setDisable(!this.isSlaveMode.isSelected()));
		
		// add checker
		this.addValidateToControl(this.workingDir, "workingDir", f -> this.isAbsoluteFolder((TextField) f, "Working directory must be an absolute path to a file."));
		this.addValidateToControl(this.javaPath, "javaPath", f -> !this.isSlaveMode.isSelected() || this.isAbsoluteFile((TextField) f, "Java path must be an absolute path to a file."));
		this.addValidateToControl(this.maxRunning, "maxRunning", f -> this.isInteger((TextField) f, "Max running must be an integer value."));
		this.addValidateToControl(this.shebangHeader, "commandHeader", f -> !this.isEmpty((TextField) f, "Shebang for external command call can not be empty."));
		this.addValidateToControl(this.container, "container", f -> this.validateContainer((TextField) f));
		this.addValidateToControl(this.packageManagers, "packageManagers", f -> this.validatePackageManagers((TextField) f));

		// add event handler for GUI validation
		this.workingDir.textProperty().addListener(event -> this.validate());
		this.javaPath.textProperty().addListener(event -> this.validate());
		this.maxRunning.textProperty().addListener(event -> this.validate());
		this.shebangHeader.textProperty().addListener(event -> this.validate());
		this.container.textProperty().addListener(event -> this.validate());
		this.packageManagers.textProperty().addListener(event -> this.validate());
		
		// add suggest constants support
		@SuppressWarnings("unused") SuggestPopup p1 = new SuggestPopup(this.workingDir);
		@SuppressWarnings("unused") SuggestPopup p2 = new SuggestPopup(this.javaPath);
	}

	private boolean validateContainer(TextField f) {
		PropertyManager mgr = PropertyManager.getPropertyManager(PropertyViewType.WRAPPERS);
		String text = f.getText();
		String error = "No container wrapper with name '"+text+"' is defined!";
		boolean ret = false;
		
		if(!text.isEmpty())	{
			for(XMLDataStore d : mgr.getXMLData()) {
				if(d.getName().equals(text)) {
					// test if is it a container wrapper
					if(((ExecutionWrapper) d).isPackageManager()) {
						ret = false;
						error = "Wrapper with name '"+text+"' is no container wrapper!";
					}
					else ret = true;
					break;
				}
			}
			if(!ret) this.addMessageToPrivateLog(MessageType.WARNING, error);
		}
		else {
			ret = true;
		}
		
		GUIFormat.colorTextField(f, ret); // color the stuff correctly
		return ret;
	}

	private boolean validatePackageManagers(TextField f) {
		PropertyManager mgr = PropertyManager.getPropertyManager(PropertyViewType.WRAPPERS);
		String text = f.getText();
		boolean ret = true;
		
		if(!text.isEmpty())	{
			ArrayList<String> names = XMLParser.splitString(text, XMLParser.WRAPPERS_SEP);

			HashMap<String, ExecutionWrapper> wrapper = new HashMap<>();
			for(XMLDataStore d : mgr.getXMLData()) {
				wrapper.put(d.getName(), (ExecutionWrapper) d);
			}
			for(String n : names) {
				if(!wrapper.containsKey(n)) {
					this.addMessageToPrivateLog(MessageType.WARNING, "No package manager wrapper with name '"+n+"' is defined!");
					ret = false;
				}
				else {
					// test if is it a package manager wrapper
					if(!wrapper.get(n).isPackageManager()) {
						ret = false;
						this.addMessageToPrivateLog(MessageType.WARNING, "Wrapper with name '"+n+"' is no package manager wrapper!");
					}
				}
			}
		}
		GUIFormat.colorTextField(f, ret); // color the stuff correctly
		return ret;
	}

	@Override
	protected void loadAdditionalUnspecificBaseData(ExecutorInfo data) {
		if(data != null) {
			this.javaPath.setDisable(!data.isStick2Host());
			this.useAsDefault.setSelected(data.isDefaultExecutor());
			
			// load advanced settings
			this.isSlaveMode.setSelected(data.isStick2Host());
			this.workingDir.setText(data.getStaticWorkingDir());
			this.javaPath.setText(data.getPath2Java());
			this.maxRunning.setText(Integer.toString(data.getMaxSimRunning()));
			this.shebangHeader.setText(data.getShebang());
			this.beforeScripts.setText(XMLExecutorInfoParser.joinString(data.getBeforeScriptNames()));
			this.afterScripts.setText(XMLExecutorInfoParser.joinString(data.getAfterScriptNames()));
			this.packageManagers.setText(XMLExecutorInfoParser.joinString(data.getPackageManagers()));
			this.container.setText(data.getContainer());
			try { this.maxSlaveRunning.setText(Integer.toString(data.getMaxSlaveRunningTasks())); } catch(Exception e) {}
			
			// check, if environment is set and can be loaded
			if(data.getEnv() != null) {
				String envName = data.getEnv().getName();
				int index = 0;
				boolean found = false;
				for(Environment e: this.environment.getItems()) {
					if(envName.equals(e.getName())) {
						this.environment.getSelectionModel().select(index);
						found = true;
						break;
					}
					index++;
				}
				
				if(found == false) {
					this.addMessageToPrivateLog(MessageType.ERROR, "Was not able to find environment with name '"+envName+"'.");
				}
			}
		}
	}
}
