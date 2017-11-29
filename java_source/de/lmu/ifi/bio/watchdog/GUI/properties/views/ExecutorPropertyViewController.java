package de.lmu.ifi.bio.watchdog.GUI.properties.views;


import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.MessageType;
import de.lmu.ifi.bio.watchdog.GUI.helper.PreferencesStore;
import de.lmu.ifi.bio.watchdog.GUI.helper.SuggestPopup;
import de.lmu.ifi.bio.watchdog.GUI.helper.TextFilter;
import de.lmu.ifi.bio.watchdog.GUI.properties.PropertyManager;
import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.executor.cluster.ClusterExecutorInfo;
import de.lmu.ifi.bio.watchdog.executor.local.LocalExecutorInfo;
import de.lmu.ifi.bio.watchdog.executor.remote.RemoteExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ExecutorPropertyViewController extends PropertyViewController {

	@FXML private TextField name;
	@FXML private CheckBox useAsDefault;
	@FXML private ChoiceBox<GUIExecutorView> type;
	@FXML private Pane specific;
	@FXML private TitledPane advancedPane;
	@FXML private CheckBox isSlaveMode;
	@FXML private TextField workingDir;
	@FXML private TextField javaPath;
	@FXML private TextField maxRunning;
	@FXML private TextField maxSlaveRunning;
	@FXML private VBox parentBox;
	@FXML private BorderPane root;
	@FXML private ChoiceBox<Environment> environment;

	private static final LinkedHashMap<Class<? extends ExecutorInfo>, Class<? extends GUIExecutorView>> EXECUTOR_VIEWS = new LinkedHashMap<>();
	private static final HashMap<Class<? extends ExecutorInfo>, Integer> EXECUTOR_INDEX = new HashMap<>();
	private static final Environment NOT_SELECT;
	
	private ExecutorInfo executorStore;
	private GUIExecutorView activeGUIView;
	private final HashMap<String, GUIExecutorView> REAL_VIEWS = new HashMap<>();
	
	// register the classes shipped with watchdog
	static { 
		ExecutorPropertyViewController.addNewExecutor(LocalExecutorInfo.class, LocalGUIExecutorView.class);
		ExecutorPropertyViewController.addNewExecutor(RemoteExecutorInfo.class, RemoteGUIExecutorView.class);
		ExecutorPropertyViewController.addNewExecutor(ClusterExecutorInfo.class, ClusterGUIExecutorView.class);
		NOT_SELECT = new Environment("-- no environment set --", false, false);
	}
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);
		// add all registered executors
		boolean first = true;
		try {
			for(Class<? extends ExecutorInfo> c : EXECUTOR_VIEWS.keySet()) {
				GUIExecutorView viewDummy = EXECUTOR_VIEWS.get(c).newInstance();
				this.type.getItems().add(viewDummy);
				if(first)
					this.type.getSelectionModel().select(viewDummy);
				first = false;
			}
		} catch(Exception e) { e.printStackTrace(); System.exit(1);} // must work
		this.onChangeType();
		
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
		this.type.setOnAction(c -> this.onChangeType());
		this.advancedPane.expandedProperty().addListener(event -> this.onExpandChange());
		this.isSlaveMode.setOnAction(x -> this.javaPath.setDisable(!this.isSlaveMode.isSelected()));
		
		// add checker
		this.addValidateToControl(this.name, "name", f -> this.checkName((TextField) f));
		this.addValidateToControl(this.workingDir, "workingDir", f -> this.isAbsoluteFolder((TextField) f, "Working directory must be an absolute path to a file."));
		this.addValidateToControl(this.javaPath, "javaPath", f -> !this.isSlaveMode.isSelected() || this.isAbsoluteFile((TextField) f, "Java path must be an absolute path to a file."));
		this.addValidateToControl(this.maxRunning, "maxRunning", f -> this.isInteger((TextField) f, "Max running must be an integer value."));

		// add event handler for GUI validation
		this.name.textProperty().addListener(event -> this.validate());
		this.workingDir.textProperty().addListener(event -> this.validate());
		this.javaPath.textProperty().addListener(event -> this.validate());
		this.maxRunning.textProperty().addListener(event -> this.validate());
		
		// add suggest constants support
		@SuppressWarnings("unused") SuggestPopup p1 = new SuggestPopup(this.workingDir);
		@SuppressWarnings("unused") SuggestPopup p2 = new SuggestPopup(this.javaPath);
	}
	
	@Override
	protected boolean validate() {
		return this.validate(this.activeGUIView.getName());
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


	/**
	 * is called when ever the type is changed
	 */
	private void onChangeType() {
		try {
			GUIExecutorView viewDummy = this.type.getSelectionModel().getSelectedItem();
			// create instance
			if(!this.REAL_VIEWS.containsKey(viewDummy.getName()))
				this.REAL_VIEWS.put(viewDummy.getName(), this.getInstance(viewDummy));
			
			// set the instance
			this.activeGUIView = this.REAL_VIEWS.get(viewDummy.getName());

			// add it on the GUI
			this.specific.getChildren().clear();
			if(this.specific.getChildren().size() > 0)
				this.specific.getChildren().set(0, this.activeGUIView);
			else
				this.specific.getChildren().add(this.activeGUIView);
			
			// call validate to get initial coloring
			this.validate();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private GUIExecutorView getInstance(GUIExecutorView viewDummy) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		GUIExecutorView pane = null;
		try {
			pane = viewDummy.getClass().newInstance();
		} catch(Exception e) { e.printStackTrace(); System.exit(1);} // must work
		
		GUIExecutorView view = GUIExecutorView.getExecutorPropertyView(viewDummy.getFXMLResourceFilename(), pane);
		view.addValidateToControl(this);
		return view;
	}
	
	private boolean checkName(TextField f) {
		if(this.isEmpty((TextField) f, "Name for executor property is missing."))
				return false;
		if(!this.hasUniqueName(((TextField) f).getText()))
			return false;
		
		// all was ok!
		return true;
	}

	public ExecutorInfo getStoredData() {
		return this.executorStore;
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
		
		// read max slave running property
		Integer maxSlaveRunning = null;
		if(isStick2Host)
			maxSlaveRunning = Integer.parseInt(this.maxSlaveRunning.getText());
		
		String watchdogBaseDir = PreferencesStore.getWatchdogBaseDir();
		Environment environment = this.getSelectedEnvironment();

		// get the executor info
		ExecutorInfo e = this.activeGUIView.getExecutor(name, isDefault, isStick2Host, maxSlaveRunning, path2java, maxRunning, watchdogBaseDir, environment, workingDir);
		
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
	
	protected PropertyViewType getPropertyTypeName() {
		return PropertyViewType.EXECUTOR;
	}
	
	private void storeXMLData(ExecutorInfo data) {
		this.executorStore = data;
		XMLDataStore.registerData(data);
	}

	public void loadData(ExecutorInfo data) {
		if(data != null) {
			// unregister that data or otherwise name will be blocked!
			XMLDataStore.unregisterData(data);
			this.isDataLoaded = true;
			this.type.getSelectionModel().select(EXECUTOR_INDEX.get(data.getClass()));
			this.javaPath.setDisable(!data.isStick2Host());
		
			// set basic settings
			this.name.setText(data.getName());
			this.useAsDefault.setSelected(data.isDefaultExecutor());
			
			// load advanced settings
			this.isSlaveMode.setSelected(data.isStick2Host());
			this.workingDir.setText(data.getStaticWorkingDir());
			this.javaPath.setText(data.getPath2Java());
			this.maxRunning.setText(Integer.toString(data.getMaxSimRunning()));
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
			
			// load executor specific settings
			if(data instanceof LocalExecutorInfo) {
				// nothing to do here
			}
			else if(data instanceof RemoteExecutorInfo) {
				RemoteExecutorInfo r = (RemoteExecutorInfo) data;
				this.activeGUIView.loadData(new Object[] { r.getOriginalHostList(), r.getUser(), r.getPrivateKey(), r.getPort(), !r.isStrictHostCheckingEnabled() });
			}
			else if(data instanceof ClusterExecutorInfo) {
				ClusterExecutorInfo c = (ClusterExecutorInfo) data;
				this.activeGUIView.loadData(new Object[] { c.getQueue(), c.getSlots(), c.getMemory(), c.getCustomParameters(), c.isDefaultParametersIgnored() });		
			}
			this.type.getSelectionModel().select(this.activeGUIView);
			
			// load the data
			this.executorStore = data;
			this.isDataLoaded = false;
		}
	}

	@Override
	protected boolean hasUniqueName(String name) {
		if(!XMLDataStore.hasRegistedData(name, ExecutorInfo.class))
			return true;
		else {
			this.addMessageToPrivateLog(MessageType.ERROR, "An executor property with name '"+name+"' exists already.");
			return false;
		}
	}
	
	/**
	 * adds a new factory for executors 
	 * @param f
	 */
	public static void addNewExecutor(Class<? extends ExecutorInfo> c, Class<? extends GUIExecutorView> view) {
		// save the class for further use
		EXECUTOR_INDEX.put(c, EXECUTOR_VIEWS.size());
		EXECUTOR_VIEWS.put(c, view);
	}
}
