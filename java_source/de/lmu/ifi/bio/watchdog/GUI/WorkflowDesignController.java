package de.lmu.ifi.bio.watchdog.GUI;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Element;

import de.lmu.ifi.bio.multithreading.TimedExecution;
import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.AdditionalBarController;
import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.LogMessageEventHandler;
import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.MessageType;
import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.StatusConsole;
import de.lmu.ifi.bio.watchdog.GUI.css.CSSRessourceLoader;
import de.lmu.ifi.bio.watchdog.GUI.datastructure.Module;
import de.lmu.ifi.bio.watchdog.GUI.event.TabEvent;
import de.lmu.ifi.bio.watchdog.GUI.helper.ExecuteToolbar;
import de.lmu.ifi.bio.watchdog.GUI.helper.FinishedCheckerThread;
import de.lmu.ifi.bio.watchdog.GUI.helper.GUIConfirmation;
import de.lmu.ifi.bio.watchdog.GUI.helper.GUIPasswortGetter;
import de.lmu.ifi.bio.watchdog.GUI.helper.Inform;
import de.lmu.ifi.bio.watchdog.GUI.helper.ParamValue;
import de.lmu.ifi.bio.watchdog.GUI.helper.ParameterToControl;
import de.lmu.ifi.bio.watchdog.GUI.helper.PreferencesStore;
import de.lmu.ifi.bio.watchdog.GUI.helper.ScreenCenteredStage;
import de.lmu.ifi.bio.watchdog.GUI.interfaces.TabableNode;
import de.lmu.ifi.bio.watchdog.GUI.layout.Dependency;
import de.lmu.ifi.bio.watchdog.GUI.layout.RasteredGridPane;
import de.lmu.ifi.bio.watchdog.GUI.module.GUIStatusUpdateHandler;
import de.lmu.ifi.bio.watchdog.GUI.module.WorkflowModule;
import de.lmu.ifi.bio.watchdog.GUI.module.WorkflowModuleData;
import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import de.lmu.ifi.bio.watchdog.GUI.properties.PropertyLine;
import de.lmu.ifi.bio.watchdog.GUI.properties.PropertyManager;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PropertyViewType;
import de.lmu.ifi.bio.watchdog.docu.DocuXMLParser;
import de.lmu.ifi.bio.watchdog.executor.Executor;
import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.executor.HTTPListenerThread;
import de.lmu.ifi.bio.watchdog.executor.MonitorThread;
import de.lmu.ifi.bio.watchdog.executor.WatchdogThread;
import de.lmu.ifi.bio.watchdog.helper.Constants;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.ErrorCheckerStore;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.helper.GUIInfo;
import de.lmu.ifi.bio.watchdog.helper.GUISaveHelper;
import de.lmu.ifi.bio.watchdog.helper.Mailer;
import de.lmu.ifi.bio.watchdog.helper.Parameter;
import de.lmu.ifi.bio.watchdog.helper.SSHPassphraseAuth;
import de.lmu.ifi.bio.watchdog.helper.UserConfirmation;
import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import de.lmu.ifi.bio.watchdog.helper.returnType.ReturnType;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessBlock;
import de.lmu.ifi.bio.watchdog.resume.WorkflowResumeLogger;
import de.lmu.ifi.bio.watchdog.runner.XMLBasedWatchdogRunner;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.task.TaskAction;
import de.lmu.ifi.bio.watchdog.task.TaskStatus;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask2TaskThread;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class WorkflowDesignController implements Initializable, GUISaveHelper {

	private static final String DEFAULT_FILE_NAME = "Workflow.watchdog.xml";
	@FXML private AdditionalBarController additionalBarController;
	@FXML private ToolLibraryController toolLibController; 
	@FXML private WorkflowController workflowController;
	@FXML private VBox propertyManager;
	@FXML private VBox toolLib;
	@FXML private MenuItem save;
	@FXML private MenuItem saveAs;
	@FXML private MenuItem close;
	@FXML private MenuItem open;
	@FXML private Menu openRecent;
	@FXML private MenuItem about;
	@FXML private MenuItem newItem;
	@FXML private MenuItem preferences;
	@FXML private MenuItem updateGridSize;
	@FXML private MenuItem validateWorkflow;
	@FXML private MenuItem executeView;
	@FXML private MenuItem modifyView;
	@FXML private BorderPane root;
	@FXML private VBox bottomBox;
	@FXML private ScrollPane rootPropertyManager;
	
	private static final HashSet<KeyCode> ACTIVE_KEYS = new HashSet<>(); // stores all active Keys
	private static final String UNSAVED = "Unsaved Workflow";
	private static final String EXECUTION_CONSOLE_NAME = "Execution log";
	private HashMap<PropertyViewType, PropertyManager> PROP_MANAGER = new HashMap<>();
	private static WorkflowDesignController lastInstance;
	private SimpleStringProperty currentLoadedFile = new SimpleStringProperty();
	private HostServices hostService;
	private String hashOfLastSaveFile = null;
	private static BooleanBinding isAllConfiguredReady = new SimpleBooleanProperty(true).and(new SimpleBooleanProperty(true));
	private static KeyCodeCombination SEARCH_FOCUS = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);
	private static PropertyManager CONST_MANAGER;
	private static boolean EXECUTION_MODE = false;
	private final ExecuteToolbar EXEC_TOOLBAR = ExecuteToolbar.getExecuteToolbarer(this);
	private WatchdogThread runWatchdog;
	private XMLTask2TaskThread runXml2taskThread; 
	private FinishedCheckerThread runFinishedCheckerThread;
	private File resumeFile = null;
	private StatusConsole runLogConsole;
	private HTTPListenerThread runHttp;
	private LogMessageEventHandler GLOBAL_MESSAGE_HANDLER;
	private WorkflowDesignParameters commandlineParams;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		WorkflowDesignController.lastInstance = this;
		
		// register it on XML DATA store for change updates
		XMLDataStore.registerNotifyOnRegisterOrUnregisterData((GUISaveHelper) this);
		SSHPassphraseAuth.changePasswordRequestType(new GUIPasswortGetter());
		UserConfirmation.setUserConfirmationRequester(new GUIConfirmation());
		
		// collect log messages
		this.GLOBAL_MESSAGE_HANDLER = new LogMessageEventHandler(this.additionalBarController.getGlobalConsole());
		Logger.registerListener(this.GLOBAL_MESSAGE_HANDLER, LogLevel.INFO);
					
		// init property manager
		this.initPropertyManager();  
		
		// init tool lib
		this.loadModuleLibrary();
		
		this.newItem.setGraphic(ImageLoader.getImage(ImageLoader.NEW_SMALL));
		this.open.setGraphic(ImageLoader.getImage(ImageLoader.OPEN_SMALL));
		this.openRecent.setGraphic(ImageLoader.getImage(ImageLoader.RECENT_SMALL));
		this.save.setGraphic(ImageLoader.getImage(ImageLoader.SAVE_SMALL));
		this.saveAs.setGraphic(ImageLoader.getImage(ImageLoader.SAVE_AS_SMALL));
		this.close.setGraphic(ImageLoader.getImage(ImageLoader.DELETE_SMALL));
		this.preferences.setGraphic(ImageLoader.getImage(ImageLoader.CONFIG_SMALL));
		this.about.setGraphic(ImageLoader.getImage(ImageLoader.ABOUT_SMALL));
		this.validateWorkflow.setGraphic(ImageLoader.getImage(ImageLoader.VERIFY_SMALL));
		this.executeView.setGraphic(ImageLoader.getImage(ImageLoader.RUN_SMALL));
		this.modifyView.setGraphic(ImageLoader.getImage(ImageLoader.CONFIG_SMALL));
		this.updateGridSize.setGraphic(ImageLoader.getImage(ImageLoader.RESIZE_SMALL));
		
		// add listener to menu
		this.close.onActionProperty().set(event -> this.onClose(event));
		this.save.onActionProperty().set(event -> this.onSave(event, this.currentLoadedFile.get() != null ? new File(this.currentLoadedFile.get()) : null));
		this.saveAs.onActionProperty().set(event -> this.onSave(event, null));
		this.about.onActionProperty().set(event -> this.onAbout(event));
		this.newItem.onActionProperty().set(event -> this.onNew(event, false));
		this.open.onActionProperty().set(event -> this.loadWorkflow(null));
		this.updateGridSize.onActionProperty().set(event -> this.workflowController.adaptGrid());
		this.validateWorkflow.onActionProperty().set(event -> { this.validate(false); event.consume(); });
		this.executeView.onActionProperty().set(event -> { this.switchToExecutionMode(); event.consume(); });
		this.modifyView.onActionProperty().set(event -> { this.switchToModifyMode(); event.consume(); });
		this.preferences.onActionProperty().set(e -> {this.openPreferences(); e.consume();});
				
		// other events
		this.currentLoadedFile.addListener((a, b, c) -> this.updateTitle());
		
		if(!PreferencesStore.getUnsafeSaveXMLValidationMode()) {
			this.save.setDisable(true);
			this.saveAs.setDisable(true); 
		}
		else { // let the user save any status
			this.save.setDisable(false);
			this.saveAs.setDisable(false); 	
		}
		this.validateWorkflow.setDisable(true);
		this.executeView.setDisable(true);
		this.modifyView.setDisable(true);
		
		// set some shortcuts
		this.newItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
		this.save.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
		this.close.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN));
		
		// add open recent files
		this.updateOpenedRecently();
		
		// add global key listener
		this.root.addEventFilter(KeyEvent.KEY_PRESSED, k -> this.pressKeyEvent(k));
		this.root.addEventFilter(KeyEvent.KEY_RELEASED, k -> this.releaseKeyEvent(k));
	}
	
	public void setCommandLineArgs(WorkflowDesignParameters params) {
		this.commandlineParams = params;
	}
	
	private void switchMode(boolean executionView) {
		// do not do anything
		if(isInExecutionMode() == executionView)
			return;
		
		//this.workflowController.ignoreSizeChange(true);
		this.additionalBarController.clear(); // free space of log file in any case
		if(executionView) {
			this.root.setLeft(null);
			this.root.setRight(null);
			
			this.modifyView.setDisable(false);
			this.executeView.setDisable(true);
			this.preferences.setDisable(true);
			this.validateWorkflow.setDisable(true);
			this.updateGridSize.setDisable(true);
			this.bottomBox.getChildren().add(this.EXEC_TOOLBAR);
		}
		else {
			Logger.registerListener(this.GLOBAL_MESSAGE_HANDLER, LogLevel.INFO);
			this.root.setLeft(this.toolLib);
			this.root.setRight(this.rootPropertyManager);

			this.preferences.setDisable(false);
			this.modifyView.setDisable(true);
			this.executeView.setDisable(false);
			this.validateWorkflow.setDisable(false);
			this.updateGridSize.setDisable(false);
			this.bottomBox.getChildren().remove(this.EXEC_TOOLBAR);
			this.additionalBarController.hideGlobalConsole(false);
			// remove status log
			if(this.runLogConsole != null) {
				this.runLogConsole = null;
			}
			
			// validate all stuff
			this.workflowController.validateAllModules();
		}
		WorkflowDesignController.EXECUTION_MODE = executionView;
	}
	
	public static PropertyManager getConstManager() {
		return CONST_MANAGER;
	}
	
	private void switchToExecutionMode() {
		this.switchMode(true);
	}
	
	private void switchToModifyMode() {
		// ask, if a workflow is running
		if(this.isProcessingActive()) {
			if(!this.ask4Stop())
				return;
		}
		this.EXEC_TOOLBAR.stopWorkflow();
		this.switchMode(false);
	}

	private void configureChanged() {
		if(!this.isProcessingActive()) {
			boolean status = !PreferencesStore.getUnsafeSaveXMLValidationMode() && !isAllConfiguredReady.get();
			this.save.setDisable(status);
			this.saveAs.setDisable(status);
			this.validateWorkflow.setDisable(status);
			this.executeView.setDisable(status);
		}
	}
	
	public static void bindConfiguredReady(SimpleBooleanProperty s) {		
		isAllConfiguredReady = isAllConfiguredReady.and(s);
		isAllConfiguredReady.addListener(x -> WorkflowDesignController.lastInstance.configureChanged());
		WorkflowDesignController.configureHasChangedStatic();
	}
	
	public static void configureHasChangedStatic() {
		WorkflowDesignController.lastInstance.configureHasChanged();
	}
	
	@Override
	public void configureHasChanged() {
		WorkflowDesignController.lastInstance.configureChanged();
	}

	private void pressKeyEvent(KeyEvent k) {
		registerKeyEvent(k);
		
		// test if Strg+F was released
		if(SEARCH_FOCUS.match(k))
			this.toolLibController.setSearchFocus();
	}
	
	private void releaseKeyEvent(KeyEvent k) {
		unregisterKeyEvent(k);
	}

	private static void unregisterKeyEvent(KeyEvent k) {
		ACTIVE_KEYS.remove(k.getCode());
	}

	private static void registerKeyEvent(KeyEvent k) {
		ACTIVE_KEYS.add(k.getCode());
	}
	
	public static boolean isKeyActive(KeyCode key) {
		return ACTIVE_KEYS.contains(key);
	}

	@SuppressWarnings("unchecked")
	private void loadWorkflow(File f) {
		if(!this.confirmChange())
			return;
		
		if(f == null) {
			// filename must be selected first
			f = this.openFileSelectDialog(false, "Open workflow", "*.xml", this.root.getScene().getWindow(), "Watchdog workflow");
		}
		if(f == null)
			return;
		
		// test if file exists
		if(!(f.isFile() && f.exists() && f.canRead())) {
			String er = "Workflow file '"+f.getAbsolutePath()+"' can not be found.";
			StatusConsole.addGlobalMessage(MessageType.ERROR, er);
			Inform.error("File not found", er);
			return;
		}
		// clear all old stuff
		this.onNew(null, true);
		
		// load the new
		try {
			// test, if an unsafe workflow
			boolean xmlTaggedAsUnsafe = XMLParser.testIfUnsafe(f.getAbsolutePath());
			boolean loadUnsafeFile = PreferencesStore.getUnsafeLoadXMLValidationMode();
			// ask user if he want's to load the unsafe file
			if(xmlTaggedAsUnsafe && loadUnsafeFile == false) {
				Optional<ButtonType> confirm = Inform.confirm("This XML file is not compatible with the XSD definition."+ System.lineSeparator() + System.lineSeparator() +"Do you want to continue anyway?");
				if(confirm.get() == ButtonType.OK)
					loadUnsafeFile = true;
				else
					return;
			}
			
			XMLParser.setGUILoadAttempt(true);
			Object[] ret = XMLParser.parse(f.getAbsolutePath(), XMLBasedWatchdogRunner.findXSDSchema(f.getAbsolutePath(), false, null).getAbsolutePath(), this.commandlineParams.tmpFolder, 0, false, true, false, false, loadUnsafeFile, false, false, true);
			if(ret == null) {
				Inform.error("Failed to parse the workflow", "Check your standard out and error messages in order to identify the problem.\nOr use the command-line tool with the -validate option.");
				XMLParser.setGUILoadAttempt(false);
				return;
			}
			PreferencesStore.addLastSaveFile(f);
			this.updateOpenedRecently();
			
			boolean saveWF2Disk = false;
			ArrayList<XMLTask> xmlTasks = (ArrayList<XMLTask>) ret[0];
			String mail = (String) ret[1];
			if(mail != null && mail.length() > 0 && PreferencesStore.isMailNotificationEnabled()) {
				if(!mail.equals(PreferencesStore.getMail())) {
					// ask if the user want's to overwrite this settings with his mail.
					Optional<ButtonType> confirm = Inform.confirm("Your stored mail adress is '"+PreferencesStore.getMail()+"'."+System.lineSeparator()+" Do you want to override the mail given in the workflow ('"+mail+"') with your mail?");
					if(confirm.get() == ButtonType.OK) {
						// change
						mail = PreferencesStore.getMail();
						Task.setMail(new Mailer(mail));
						
						// write it to DISK
						saveWF2Disk = true;
					}
				}
			}
			Task.setMail(new Mailer(mail));
			String watchdogBase = (String) ret[9];
			Executor.setWatchdogBase(new File(watchdogBase), this.commandlineParams.tmpFolder == null ? null : new File(this.commandlineParams.tmpFolder)); 
			@SuppressWarnings("unused")
			HashMap<String, Pair<HashMap<String, ReturnType>, String>> retInfo = (HashMap<String, Pair<HashMap<String, ReturnType>, String>>) ret[3];
			LinkedHashMap<String, ProcessBlock> processblocks = new LinkedHashMap<>();
			LinkedHashMap<String, Environment> environments = new LinkedHashMap<>();
			LinkedHashMap<String, ExecutorInfo> executors = new LinkedHashMap<>();
			processblocks.putAll(((HashMap<String, ProcessBlock>) ret[6]));
			environments.putAll(((HashMap<String, Environment>) ret[7]));
			executors.putAll(((HashMap<String, ExecutorInfo>) ret[8]));
			LinkedHashMap<String, String> constants = (LinkedHashMap<String, String>) ret[10];
			HashMap<String, WorkflowModule> workModules = new HashMap<>();
			
			// index tasks by ID
			int maxX = 0;
			int maxY = 0;
			int x = 2;
			int y = 2;
			HashMap<Integer, XMLTask> dependency = new HashMap<>();
			for(XMLTask xt : xmlTasks) {
				dependency.put(xt.getXMLID(), xt);
				
				// generate own position as none is given
				if(xt.getGuiInfo() == null || xt.getGuiInfo().getPosX() == null || xt.getGuiInfo().getPosY() == null) {
					if(x > 10) {
						x = x % 10;
						y = y + 2;
					}
					GUIInfo g = new GUIInfo();
					g.setPosX(x);
					g.setPosY(y);
					xt.setGuiInfo(g);
					x = x + 2;
				}
				// read position from XML file
				else {
					x = xt.getGuiInfo().getPosX();
					y = xt.getGuiInfo().getPosY();
				}
				
				// update max value
				if(maxX < x)
					maxX = x;
				if(maxY < y)
					maxY = y;
			}			
			// load tasks
			int globalDepCounter = 0;
			int sepDepCounter = 0;
			RasteredGridPane grid = this.workflowController.getGrid();
			Pair<Integer, Integer> size = grid.getScreenFitGridSize(null);
			maxX = Math.max(maxX + grid.getMaxExtendDist()*2, size.getKey());
			maxY = Math.max(maxY + grid.getMaxExtendDist()*2, size.getValue());
			grid.reinitRaster(maxX, maxY);
			// ad tasks
			for(XMLTask xt : xmlTasks) {
				ExecutorInfo ex = xt.getExecutor();
				GUIInfo g = xt.getGuiInfo();
				x = g.getPosX();
				y = g.getPosY();

				// collect information
				Environment e = ex.getEnv();
				if(e != null && !XMLParser.DEFAULT_LOCAL_COPY_ENV.equals(e.getName()))
					environments.put(e.getName(), e);
				if(xt.getEnvironment() != null && !XMLParser.DEFAULT_LOCAL_COPY_ENV.equals(xt.getEnvironment().getName()))
					environments.put(xt.getEnvironment().getName(), xt.getEnvironment());
				if(e != null && !XMLParser.DEFAULT_LOCAL_NAME.equals(ex.getName()))
					executors.put(ex.getName(), ex);
				
				// create a new Task
				Module d = this.toolLibController.getModuleData(xt.getTaskType());
				if(d == null) {
					Inform.error("Missing library folder", "Task of type '"+xt.getTaskType()+"' can not be found. Please inlude the required module search folder.");
					return;
				}

				// place the new node
				WorkflowModule m = WorkflowModule.getModule(d, x, y, grid);
				workModules.put(xt.getTaskName(), m); // store it for later use
				grid.placeContent(m, x, y, true);
				WorkflowModuleData additionalData = m.getSavedData();
				// set the name and additional data
				additionalData.id = xt.getXMLID();
				additionalData.name = xt.getTaskName();
				additionalData.simMaxRunning = xt.getMaxRunning();
				additionalData.notify = xt.getNotify();
				additionalData.confirm = xt.getConfirmParam();
				additionalData.checkpoint = xt.getCheckpoint();
				
				additionalData.appendOut = xt.isOutputAppended();
				additionalData.appendErr = xt.isErrorAppended();
				additionalData.enforceStdin = !xt.isStdinExistenceDisabled();
				
				additionalData.saveRes = xt.isSaveResourceUsageEnabled();
				additionalData.stdOut = XMLParser.ensureAbsoluteFile(xt.getPlainStdOut());
				additionalData.stdErr = XMLParser.ensureAbsoluteFile(xt.getPlainStdErr());
				additionalData.workingDir = XMLParser.ensureAbsoluteFolder(xt.getPlainWorkingDir());
				additionalData.stdIn = XMLParser.ensureAbsoluteFile(xt.getPlainStdIn());
				
				// load actions
				for(TaskAction a : xt.getTaskActions()) {
					additionalData.addTaskAction(a);
				}
				
				// load checkers
				for(ErrorCheckerStore ec : xt.getCheckers()) {
					additionalData.addErrorChecker(ec);
				}

				// add the parameter
				LinkedHashMap<String, ParamValue> params = new LinkedHashMap<>();
				LinkedHashMap<String, String> loadedParams = xt.getParamList();
				for(String key : loadedParams.keySet()) {
					boolean booleanIsFalse = false;
					// handle negative boolean values
					if(!d.getParameter().containsKey(key)) {
						key = key.replaceFirst("^no", "");
						booleanIsFalse = true;
					}
					
					ParamValue pv = new ParamValue(key, ParameterToControl.getControlElement(d.getParameter().get(key).getType()), null);
					String storeValue = loadedParams.get(key);
					// handle boolean values
					if(pv.isBoolean())
						storeValue = Boolean.toString(!booleanIsFalse);

					// store the stuff
					params.put(key, pv);
					pv.updateValue(storeValue);
					pv.saveCurrentValue();
				}
				m.setLoadedParameters(params);
				
				// handle dependencies
				for(int dep : xt.getGlobalDependencies()) {
					XMLTask dx = dependency.get(dep);
					grid.addDependency(dx.getGuiInfo().getPosX(), dx.getGuiInfo().getPosY(), x, y, true);
					globalDepCounter++;
				}
				// handle seperate dependencies
				HashMap<Integer, Pair<Integer, String>> localSep = xt.getDetailSeperateDependencies();
				for(int dep: localSep.keySet()) {
					XMLTask dx = dependency.get(dep);
					Pair<Integer, String> additionalInfo = localSep.get(dep);
					int prefixLength = additionalInfo.getKey();
					String sep = additionalInfo.getValue();
					grid.addSeperateDependency(dx.getGuiInfo().getPosX(), dx.getGuiInfo().getPosY(), x, y, true, sep, prefixLength);
					globalDepCounter++;
				}
			}
			
			// get the property managers
			PropertyManager envManager = this.PROP_MANAGER.get(PropertyViewType.ENVIRONMENT);
			PropertyManager constManager = this.PROP_MANAGER.get(PropertyViewType.CONSTANTS);
			PropertyManager execManager = this.PROP_MANAGER.get(PropertyViewType.EXECUTOR);
			PropertyManager blockManager = this.PROP_MANAGER.get(PropertyViewType.PROCESS_BLOCK);
			HashMap<String, PropertyLine> blocksProp = new HashMap<>();
			HashMap<String, PropertyLine> envProp = new HashMap<>();
			HashMap<String, PropertyLine> exProp = new HashMap<>();
			
			// sort the stuff
			ArrayList<String> sortedConst = new ArrayList<>(constants.keySet());
			Collections.sort(sortedConst); // sort it by name
			
			// load constants
			PropertyLine line;
			int i = 1;
			for(String cname : sortedConst) {
				String value = constants.get(cname);
				line = constManager.loadProperty(null, i++, new Constants(cname, value));
			}

			// load environments
			int overallColor = 0;
			i = 1;
			for(Environment e : environments.values()) {
				line = envManager.loadProperty(this.getColor(e, i+overallColor), i, e);
				envProp.put(e.getName(), line);
				i++;
			}
			
			// load executors
			overallColor += i - 1;
			i = 1;
			for(ExecutorInfo e : executors.values()) {
				line = execManager.loadProperty(this.getColor(e, i+overallColor), i, e);
				exProp.put(e.getName(), line);
				i++;
			}
			
			// load process blocks
			overallColor += i - 1;
			i = 1;
			for(ProcessBlock b : processblocks.values()) {
				line = blockManager.loadProperty(this.getColor(b, i+overallColor), i, b);
				// we loaded this stuff before
				if(line != null) {
					String name = b.getName();
					if(b instanceof ProcessBlock && ((ProcessBlock) b ).gui_append) {
						name += XMLParser.SUFFIX_SEP + i;
					}
					blocksProp.put(name, line);
				}
				i++;
			}
			
			// add properties to modules on GUI
			for(XMLTask xt : xmlTasks) {
				String name = xt.getTaskName();
				WorkflowModule m = workModules.get(name);
				
				// collect the props
				ArrayList<PropertyLine> props = new ArrayList<>();
				if(xt.hasEnvironment() && !XMLParser.DEFAULT_LOCAL_COPY_ENV.equals(xt.getEnvironment().getName()))
					props.add(envProp.get(xt.getEnvironment().getName()));
				if(xt.hasExecutor() && !XMLParser.DEFAULT_LOCAL_NAME.equals(xt.getExecutor().getName()))
					props.add(exProp.get(xt.getExecutor().getName()));
				if(xt.hasProcessBlock()) {
					PropertyLine d = blocksProp.get(xt.getProcessBlock().getName());
					// find the first one with that prefix
					if(d == null) {
						String withSuffix = xt.getProcessBlock().getName() + XMLParser.SUFFIX_SEP;
						for(String key : blocksProp.keySet()) {
							if(key.startsWith(withSuffix)) {
								d = blocksProp.get(key);
								break;
							}
						}
					}
					
					if(d != null) props.add(d);
				}
				
				// add them
				m.setLoadedProperties(props);
			}
			// adjust the space
			grid.adjustGridSize(null);
			processblocks.size();
			constants.size();
			
			// finished with "real" loading
			StatusConsole.addGlobalMessage(MessageType.INFO, "workflow '"+f.getAbsolutePath()+"' was loaded");
			StatusConsole.addGlobalMessage(MessageType.INFO, "number of tasks: " + grid.getActiveModules().size());
			StatusConsole.addGlobalMessage(MessageType.INFO, "number of dependencies: on task level (" + globalDepCounter +"),on subtask level (" + sepDepCounter +")");
			StatusConsole.addGlobalMessage(MessageType.INFO, "number of properties: environment (" + environments.size() +"), executors (" + executors.size() +"), process blocks (" + processblocks.size() +"), constants (" + constants.size() +")");
			this.currentLoadedFile.set(f.getAbsolutePath());
			this.hashOfLastSaveFile = this.getSaveHash(null); // required as formating might be different
			
			if(saveWF2Disk) this.saveWorkflow(f, true, true);
		}
		catch(Exception e) {
			e.printStackTrace();
			Inform.error("Something went wrong during workflow loading. For details see below.", StringUtils.join(e.getStackTrace(), "\n"));
		}
		finally {
			XMLParser.setGUILoadAttempt(false);
		}
	}
	
	private Color getColor(XMLDataStore x, int n) {
		if(x.hasColor())
			return Color.web(x.getColor());
		else {
			PropertyViewType t = PropertyViewType.getCorrespondingType(x.getStoreClassType());
			if(t != null && PropertyManager.getPropertyManager(t) != null) 
				return PropertyManager.getPropertyManager(t).getColor(n);
		}
		return Color.rgb(new Random().nextInt(256), new Random().nextInt(256), new Random().nextInt(256)); 
	}

	protected void openPreferences() { 
		openPreferences(null, false, this.toolLibController, this.additionalBarController);
	}
	
	private boolean ask4Stop() {
		if(this.isProcessingActive()) {
			String text = "Do you really want stop processing of your workflow and as a consequence thereof kill all running tasks?";
			Optional<ButtonType> confirm = Inform.confirm(text);
			return confirm.get() == ButtonType.OK;
		}
		else
			return true;
	}	
	
	/**
	 * true, if the user choose to proceed
	 * @return
	 */
	private Boolean ask4Save(boolean beforeExecute) {
		String text;
		if(beforeExecute)
			text = "Before you can execute the workflow, you must save your changes. Do you want to do that now?";
		else
			text = "If you proceed your unsaved changes will be lost.\n\nDo you really want to proceed?";
		
		// check, if there were any changes since the last save
		if(this.hashOfLastSaveFile != null) {
			String newHash = "%notAllReady%";
			if(isAllConfiguredReady.get())
				newHash = this.getSaveHash(null);
			
			// hash differs --> ask for save
			if(!this.hashOfLastSaveFile.equals(newHash)) {
				Optional<ButtonType> confirm = Inform.confirm(text);
				return confirm.get() == ButtonType.OK;
			}
			// no change was made
			else
				return null;
		}
		// test if there is anything to save yet
		else {
			ArrayList<XMLDataStore> constants = this.PROP_MANAGER.get(PropertyViewType.CONSTANTS).getXMLData();
			ArrayList<XMLDataStore> environments = this.PROP_MANAGER.get(PropertyViewType.ENVIRONMENT).getXMLData();
			ArrayList<XMLDataStore> executor = this.PROP_MANAGER.get(PropertyViewType.EXECUTOR).getXMLData();
			ArrayList<XMLDataStore> processblock = this.PROP_MANAGER.get(PropertyViewType.PROCESS_BLOCK).getXMLData();
			
			boolean somethingSaveAble = constants.size() > 0 || environments.size() > 0 || executor.size() > 0 || processblock.size() > 0 || this.workflowController.getActiveModules().size() > 0;
			if(somethingSaveAble) {
				Optional<ButtonType> confirm = Inform.confirm(text);
				return confirm.get() == ButtonType.OK;
			}
		}
		return null;
	}
	
	private String getSaveHash(File filename) {
		boolean delete = false;
		if(filename == null) {
			filename = Functions.generateRandomTmpExecutionFile("saveCheck", false);
			this.saveWorkflow(filename, false, true);
			delete = true;
		}
		String newHash = null;
		try { newHash = Functions.getFileHash(filename); } catch(Exception e) { e.printStackTrace(); }
		if(delete) 
			filename.delete();
		return newHash;
	}
	
	protected String getTitle() {
		// get title
		String t = WorkflowDesignerRunner.MAIN_TITLE;
		if(this.currentLoadedFile.get() != null && new File(this.currentLoadedFile.get()).isFile())
			t = new File(this.currentLoadedFile.get()).getName() + " - " + t;
		else
			t = UNSAVED + " - " + t;
		
		return t;
	}
	
	private void updateTitle() {
		((Stage) this.root.getScene().getWindow()).setTitle(this.getTitle());
	}
	
	/** 
	 * shows the preferences page
	 */
	public static void openPreferences(String tabname, boolean closeOnFirstSave, ToolLibraryController lib, AdditionalBarController toolbar) {
		try {
			Stage stage = new ScreenCenteredStage();
			stage.setTitle("Preferences");
			stage.setResizable(false);
			stage.initModality(Modality.APPLICATION_MODAL);
			Preferences p = Preferences.getPreferences();
			stage.setOnCloseRequest(x -> { x.consume(); p.onClose(stage); });
			Scene scene = new Scene(p);
			scene.getStylesheets().add(CSSRessourceLoader.getCSS("control.css"));
			stage.setScene(scene);	
			p.setToolLibrary(lib);
			p.setAdditionalToolbar(toolbar);
			p.setCloseOnFirstSave(closeOnFirstSave);
			if(tabname != null) 
				p.changeSelect(tabname);
			stage.showAndWait();
		}
		catch(Exception e) { e.printStackTrace(); }
	}

	private boolean validate(boolean afterSave) {
		File tmpXLMFile = Functions.generateRandomTmpExecutionFile("validate", false);
		if(this.saveWorkflow(tmpXLMFile, false, true)) {
			try {
				XMLParser.parse(tmpXLMFile.getAbsolutePath(), new File(PreferencesStore.getWatchdogBaseDir()) + File.separator + XMLParser.FILE_CHECK, this.commandlineParams.tmpFolder, 0, false, true, true, false, false, false, true, true);
				tmpXLMFile.delete();
				
				if(!afterSave)
					Inform.inform("Workflow was validated successfully.");
				return true;
			}
			catch(Exception e) {
				if(!afterSave) 
					Inform.warn("Failed to validate workflow."+System.lineSeparator() + System.lineSeparator()+"Saved as workflow not compatible with the XSD definition." , e.getMessage());
			}
		}
		else
			Inform.error("Failed to generate XML representation of workflow.", null);
		
		return false;
	}

	public void initPropertyManager() {
		this.propertyManager.getChildren().clear();

		// add the property manager
		for(PropertyViewType t : PropertyViewType.values()) {
			PropertyManager m = PropertyManager.getPropertyManager(t);
			this.propertyManager.getChildren().add(m);
			this.PROP_MANAGER.put(t, m);
		}
		CONST_MANAGER = this.PROP_MANAGER.get(PropertyViewType.CONSTANTS);
		StatusConsole.addGlobalMessage(MessageType.INFO, "empty workflow was opened");
	}
	
	private boolean confirmChange() {
		// check, if the users is sure
		if(!isInExecutionMode()) {
			Boolean returnSave = this.ask4Save(false);
			if(returnSave != null && !returnSave)
				return false;
		}
		// ensure that no workflow is running
		else {
			Boolean askStop = this.ask4Stop();
			if(askStop != null && !askStop)
				return false;
		}
		return true;
	}
	
	private void onNew(ActionEvent event, boolean checkWasAlreadyPerformed) {
		if(event != null) event.consume();
		
		// ensure that the user knows what he is doing
		if(!checkWasAlreadyPerformed && !this.confirmChange())
			return;
		
		this.currentLoadedFile.set(null);
		isAllConfiguredReady = new SimpleBooleanProperty(true).and(new SimpleBooleanProperty(true));
		this.workflowController.clear();
		this.additionalBarController.clear();
		
		
		for(Node n : this.propertyManager.getChildrenUnmodifiable()) {
			if(n instanceof PropertyManager)
				((PropertyManager) n).clear();
		}
		XMLDataStore.clearAllRegisteredData();
		this.switchMode(false);
	}

	/**
	 * is called when the user requests the application to terminate itself
	 * @param event 
	 * @return
	 */
	protected void onClose(Event event) {
		event.consume();
		// check, if process is running --> and do not stop if the user says NO
		if(!this.ask4Stop())
			return;
		// stop processing if required
		else if(this.isProcessingActive())
				this.stopWorkflow();
		
		// check, if the user wants to save his changes first
		Boolean returnSave = this.ask4Save(false);
		if(returnSave == null || returnSave) {
			// save all changes in the ini file
			PreferencesStore.saveSettingsToFile(PreferencesStore.defaultIniFile);
			TimedExecution.stopNow();
			Platform.exit();
		}
	}
	
	/**
	 * is called when the user clicks on the about button in the menu
	 * @param event 
	 */
	private void onAbout(ActionEvent event) {
		Stage stage = new ScreenCenteredStage();
		stage.setTitle("About Watchdog");
		stage.setResizable(false);
		stage.initModality(Modality.APPLICATION_MODAL);
		About about = About.getAbout(this.hostService);
		Scene scene = new Scene(about);
		stage.setScene(scene);	
		stage.showAndWait();
		event.consume();
	}
	
	/**
	 * opens a select file dialog
	 * @param savenFile
	 * @param title
	 * @param initialName
	 * @param rootWindow
	 * @return
	 */
	public File openFileSelectDialog(boolean savenFile, String title, String initialName, Window rootWindow, String filterDescription) {
		FileChooser c = new FileChooser();
		c.setTitle(title);
		if(savenFile && initialName != null)
			c.setInitialFileName(initialName);
		else if(!savenFile && initialName != null)
			c.getExtensionFilters().add(new ExtensionFilter(filterDescription, initialName));
			
		return savenFile ? c.showSaveDialog(rootWindow) : c.showOpenDialog(rootWindow);
	}
	
	private boolean saveWorkflow(File filename, boolean markAsSaved, boolean isValidToXSD) {
		// try to find a valid execution order
		ArrayList<WorkflowModule> executionOrder = this.getExecutionOrder();
		if(executionOrder == null) {
			StatusConsole.addGlobalMessage(MessageType.ERROR, "No valid task execution order was found as the dependency graph contains some loops.");
			Inform.error("No valid task execution order was found!", "Remove loops in the dependency graph to resolve that issue.");
			return false;
		}
		
		// re-order IDs in execution order
		boolean newIDs = false;
		for(WorkflowModule m : this.workflowController.getActiveModules().values()) {
			if(m.getSavedData().id == null || m.getSavedData().id <= 0) {
				newIDs = true;
				break;
			}
		}
		if(newIDs) {
			for(WorkflowModule m : this.workflowController.getActiveModules().values()) 
				m.resetID();
		}
		
		// try to write the file
		boolean canWrite = false;
		try {
			canWrite = (filename.exists() && filename.canRead() && filename.canWrite()) || ((filename.getParentFile().exists() || filename.getParentFile().mkdirs()) && filename.createNewFile());
		} catch(Exception e) {
			canWrite = false;
		}
		if(canWrite == false) {
			Inform.error("Failed to save", "Can not write file '"+filename.getAbsolutePath()+"'.");
			return false;
		}
		
		XMLBuilder b = new XMLBuilder();
		b.startDocument();
		b.startWachdog(new File(PreferencesStore.getWatchdogBaseDir()), isValidToXSD);
		b.addComment("Created by WorkflowDesigner of Watchdog v. " + XMLBasedWatchdogRunner.getVersion());
		
		ArrayList<XMLDataStore> constants = this.PROP_MANAGER.get(PropertyViewType.CONSTANTS).getXMLData();
		ArrayList<XMLDataStore> environments = this.PROP_MANAGER.get(PropertyViewType.ENVIRONMENT).getXMLData();
		ArrayList<XMLDataStore> executor = this.PROP_MANAGER.get(PropertyViewType.EXECUTOR).getXMLData();
		ArrayList<XMLDataStore> processblock = this.PROP_MANAGER.get(PropertyViewType.PROCESS_BLOCK).getXMLData();
		
		// collect module folders that must be loaded
		HashSet<String> requiredModules = new HashSet<String>();
		boolean defaultInUse = false;
		for(WorkflowModule m : this.workflowController.getActiveModules().values()) {
			String path = this.toolLibController.getIncludeDir4Module(m.getModule().getName());
			// check, if it is the default folder
			if(!new File(path).getAbsolutePath().equals(new File(PreferencesStore.getWatchdogBaseDir() + File.separator + XMLParser.MODULES).getAbsolutePath())) {
				defaultInUse = true;
				if(!path.endsWith(File.separator))
					path = path + File.separator;
				
				requiredModules.add(path);
			}
		}
		
		if(requiredModules.size() > 0 || constants.size() > 0 || environments.size() > 0 || executor.size() > 0 || processblock.size() > 0) {
			b.startTag(XMLParser.SETTINGS, true, true);
			b.endOpeningTag();
			
			// module folders
			if(requiredModules.size() > 0) {
				b.startTag(XMLParser.MODULES, true, true, false);
				if(!defaultInUse)
					b.addQuotedAttribute(XMLParser.DEFAULT_FOLDER, "");
				b.endOpeningTag(false);
				for(String f : requiredModules) {
					b.startTag(XMLParser.FOLDER, true, true);
					b.endOpeningTag(false);
					b.addContent(f, false);
					b.endCurrentTag(true);
				}
				b.newline();
				b.endCurrentTag(true);
			}
			
			// add constants section if required
			if(constants.size() > 0) {
				b.startTag(XMLParser.CONSTANTS, true, true);
				b.endOpeningTag();
				b.addXMLDataStoreContent(constants, true);
				b.endCurrentTag(true);
			}
			// add environment section if required
			if(executor.size() > 0) {
				b.startTag(XMLParser.EXECUTORS, true, true);
				b.endOpeningTag();
				b.addXMLDataStoreContent(executor, true);
				b.endCurrentTag(true);
			}
			// add environment section if required
			if(processblock.size() > 0) {
				b.startTag(XMLParser.PROCESS_BLOCK, true, true);
				b.endOpeningTag();
				b.addXMLDataStoreContent(processblock, true);
				b.endCurrentTag(true);
			}
			// add environment section if required
			if(environments.size() > 0) {
				b.startTag(XMLParser.ENVIRONMENTS, true, true);
				b.endOpeningTag();
				b.addXMLDataStoreContent(environments, true);
				b.endCurrentTag(true);
			}
			b.endCurrentTag();
		}
		
		// start tasks tag
		b.startTag(XMLParser.TASKS, true, true);
		if(PreferencesStore.isMailNotificationEnabled()) {
			String mail = PreferencesStore.getMail();
			if(Task.getMailer() != null && Task.getMailer().hasMail())
				mail = Task.getMailer().getMail();
			// set the mail
			b.addQuotedAttribute(XMLParser.MAIL, mail);
		}
		b.endOpeningTag();
		
		WorkflowModule.resetStartID();
		for(WorkflowModule m : executionOrder) {
			b.addContent(m.toXML(), true);
			b.newline();	
		}
					
		// try to write the stuff
		try { 
			// write the stuff & store indentation
			DocumentBuilderFactory dbf = DocuXMLParser.prepareDBF(PreferencesStore.getWatchdogBaseDir());
			Element root = XMLParser.getRootElement(dbf, b.toString().replace(XMLParser.NEWLINE, ""));
			XMLParser.writePrettyXML(root, filename);
			
			// update last used filename
			if(markAsSaved) {
				this.currentLoadedFile.set(filename.getAbsolutePath());
			}
		}
		catch(Exception e) {
			Inform.error("An error occurred during saving of workflow. See detailed stack trace below.", StringUtils.join(e.getStackTrace(), System.lineSeparator()));
			return false;
		}
		return true;
	}
	
	
	public ArrayList<WorkflowModule> getExecutionOrder() {
		ArrayList<WorkflowModule> order = new ArrayList<>();
		int size = this.workflowController.getActiveModules().size();
		HashMap<String, HashMap<String, WorkflowModule>> dependencies = new HashMap<>();
		// get initial dependencies
		for(WorkflowModule m : this.workflowController.getActiveModules().values()) {
			dependencies.put(m.getKey(), tranformDependencies(m.getDependencies(true)));
		}
		
		// try to find an order
		while(order.size() < size) {
			boolean progress = false;
		
			// try to find one module without dependencies
			String[] keys = dependencies.keySet().toArray(new String[0]);
			for(String key : keys) {
				HashMap<String, WorkflowModule> dep = dependencies.get(key);
				// no dependencies found
				if(dep.size() == 0) {
					progress = true;
					order.add(this.workflowController.getGrid().getModule(key));
					
					// delete module
					dependencies.remove(key);
					
					// delete the dependency from all remaining modules.
					for(HashMap<String, WorkflowModule> removeTest : dependencies.values()) {
						removeTest.remove(key);
					}
				}
			}
			
			// no progress was made --> loop
			if(progress == false) {
				return null;
			}
		}
		return order;
	}
	
	/**
	 * transform hashmap in order to find execution order
	 * @param dependencies
	 * @return
	 */
	private HashMap<String, WorkflowModule> tranformDependencies(HashMap<Dependency, WorkflowModule> dependencies) {
		HashMap<String, WorkflowModule> r = new HashMap<>();
		for(WorkflowModule m : dependencies.values())
			r.put(m.getKey(), m);
		return r;
	}

	private void updateOpenedRecently() {
		int i = 1;
		this.openRecent.getItems().clear();
		for(File f : PreferencesStore.getLastSaveFiles()) {
			MenuItem m = new MenuItem(Integer.toString(i) + ": " + f.getName());
			m.setMnemonicParsing(false); // otherwise first underscore is not shown
			m.onActionProperty().set(e -> this.loadWorkflow(f));
			this.openRecent.getItems().add(m);
			i++;
		}
		this.openRecent.setDisable(PreferencesStore.getLastSaveFiles().size() == 0);
	}
	
	/**
	 * saves the file
	 * @param event 
	 * @param filename
	 * @return
	 * @throws IOException 
	 */
	protected boolean onSave(ActionEvent event, File filename) {
		// ensure that all modules are configured
		if(!PreferencesStore.getUnsafeSaveXMLValidationMode() && !isAllConfiguredReady.get())
			return false;
		
		boolean ret = false;
		if(filename == null) {
			// filename must be selected first
			filename = this.openFileSelectDialog(true, "Save workflow", DEFAULT_FILE_NAME, this.root.getScene().getWindow(), null);
		}

		if(filename == null)
			return false;
		
		boolean validateReturn = this.validate(false);
		ret = this.saveWorkflow(filename, true, validateReturn);
		boolean retVal = false;
		if(ret) {
			// check, if it also validates according to the XSD
			
			retVal = PreferencesStore.getUnsafeSaveXMLValidationMode() || this.validate(true);
			if(retVal == false) {
				Inform.error("Your XML file is not valid according to Watchdog's XSD definition!", "Try to load it in unsafe mode which can be set in the preferences.");
			}
			
			// add file to last used files
			if(ret && retVal) {
				PreferencesStore.addLastSaveFile(filename);
				this.updateOpenedRecently();
				StatusConsole.addGlobalMessage(MessageType.INFO, "workflow was saved to '"+filename.getAbsolutePath()+"'"); 
				this.hashOfLastSaveFile = this.getSaveHash(filename);
				for(WorkflowModule m : this.workflowController.getActiveModules().values())
					m.wasSavedToFile();
			}
			// add warning
			if(!validateReturn) {
				StatusConsole.addGlobalMessage(MessageType.WARNING, "Saved workflow is not compatible with XSD definition. File must be loaded in unsafe mode."); 
			}
		}
		if(event != null) event.consume();
		return ret && retVal;
	}

	public void setModules(HashMap<String, String> moduleFolders, HashMap<String, Pair<Pair<File, File>, HashMap<String, Parameter>>> modulesAndParameters, HashMap<String, Pair<HashMap<String, ReturnType>, String>> retInfo) {
		this.toolLibController.setModules(moduleFolders, modulesAndParameters, retInfo);
	}
	
	public void initGrid(int xSize, int ySize, int maxExtendDist) {		
		this.workflowController.initGrid(xSize, ySize, maxExtendDist);
	}
	
	public static boolean sendEventToAdditionalBarController(TabEvent e) {
		return lastInstance.additionalBarController.sendEventToTab(e);
	}
	
	public static void addNewTab(TabableNode content) {
		lastInstance.additionalBarController.addNewTab(content);
	}
	
	public static boolean removeTab(String tabName) {
		return lastInstance.additionalBarController.removeTab(tabName);
	}

	public void setHostService(HostServices hostServices) {
		this.hostService = hostServices;
	}
	
	public void loadModuleLibrary() {
		this.toolLibController.loadModuleLibrary();
	}
	
	public static boolean isInExecutionMode() {
		return EXECUTION_MODE;
	}
	
	@SuppressWarnings("unchecked")
	public boolean runWorkflow() {
		if(this.isProcessingActive())
			return false;
		
		// force the user to save
		Boolean returnSave = this.ask4Save(true);
		if(returnSave != null && !returnSave)
			return false;
		else if(returnSave != null) // do not force save if there is nothing to save
			if(!this.onSave(null, this.currentLoadedFile.get() != null ? new File(this.currentLoadedFile.get()) : null))
				return false;
			
		// now it is saved --> proceed
		try {
			// set handler for messages
			if(this.runLogConsole != null) {
				// let the GC eat it
				this.runLogConsole.clear();
				this.runLogConsole = null;
				
				this.additionalBarController.removeTab(EXECUTION_CONSOLE_NAME);
			}
			this.runLogConsole = StatusConsole.getStatusConsole(EXECUTION_CONSOLE_NAME);
			this.additionalBarController.hideGlobalConsole(true);
			this.additionalBarController.addNewTab(this.runLogConsole);
			LogMessageEventHandler messageHandler = new LogMessageEventHandler(this.runLogConsole);
			Logger.registerListener(messageHandler, LogLevel.INFO);
			
			// load the XML file
			File f = new File(this.currentLoadedFile.get());
			File xsdSchema = XMLBasedWatchdogRunner.findXSDSchema(f.getAbsolutePath(), false, null); 
			// file is already loaded --> we parse it safe if valid and unsafe if not
			Object[] ret = XMLParser.parse(f.getAbsolutePath(), xsdSchema.getAbsolutePath(), this.commandlineParams.tmpFolder, 0, false, true, false, false, XMLParser.testIfUnsafe(f.getAbsolutePath()), false, false, false);
			ArrayList<XMLTask> xmlTasks = (ArrayList<XMLTask>) ret[0];
			String mail = (String) ret[1];		
			Mailer mailer = new Mailer(mail);
			Task.setMail(mailer);
			HashMap<String, Pair<HashMap<String, ReturnType>, String>> retInfo = (HashMap<String, Pair<HashMap<String, ReturnType>, String>>) ret[3];
			
			// change all IMAGES
			for(WorkflowModule m : this.workflowController.getActiveModules().values()) {
				m.setStatus(TaskStatus.WAITING_RESTRICTIONS, null, false);
			}
			
			WatchdogThread watchdog = new WatchdogThread(false, null, xsdSchema, null);
						
			// start the http server
			int port = PreferencesStore.getPort();
			Mailer.updatePort(port);
			if(this.runHttp != null) {
				this.runHttp.stop();
				this.runHttp = null;
			}
			this.runHttp = new HTTPListenerThread(port, xmlTasks, PreferencesStore.getWatchdogBaseDir());
			watchdog.setWebserver(this.runHttp);
			try {
				runHttp.start();
			}
			catch(Exception e) {
				Inform.error("Port already in use", "Webserver can not bind to port '"+port+"'. Ports 1 to 1023 might be protected by the system for privileged usage. Please use another one.");
				e.printStackTrace();
			}
			
			// create a new watchdog object and xml2 thread stuff
			this.resumeFile = new File(WorkflowResumeLogger.generateResumeFilename(f, false));
			XMLTask2TaskThread xml2taskThread = new XMLTask2TaskThread(watchdog, xmlTasks, mailer, retInfo, f, 5, null, null, this.resumeFile);
			
			// change detail degree of output
			watchdog.setLogLevel(LogLevel.ERROR);
			xml2taskThread.setLogLevel(LogLevel.ERROR);

			// set status handler
			xml2taskThread.addTaskStatusHandler(new GUIStatusUpdateHandler(this.workflowController.getActiveModules()));
												
			WatchdogThread.addUpdateThreadtoQue(xml2taskThread, true);
			Executor.setXml2Thread(xml2taskThread);
			Executor.setWatchdogBase(new File(PreferencesStore.getWatchdogBaseDir()), this.commandlineParams.tmpFolder == null ? null : new File(this.commandlineParams.tmpFolder));
			watchdog.start();
			
			this.runFinishedCheckerThread = new FinishedCheckerThread(() -> this.processingHasFinished(xmlTasks), xml2taskThread, null, this.resumeFile);
			this.runFinishedCheckerThread.start();

			// set some stuff
			this.runWatchdog = watchdog;
			this.runXml2taskThread = xml2taskThread;
			return true;
		}
		catch(Exception e) {
			e.printStackTrace();
			Inform.error("Something went wrong during the start of the workflow execution. For details see below.", StringUtils.join(e.getStackTrace(), "\n"));
		}
		return false;
	}

	private void processingHasFinished(ArrayList<XMLTask> xmlTasks) {
		this.EXEC_TOOLBAR.setIsFinished();
		
		if(this.resumeFile != null)
			StatusConsole.addGlobalMessage(MessageType.INFO, "Resume file was written to '"+this.resumeFile.getAbsolutePath()+"'");
		
		if(Task.isMailSet()) 
			Task.getMailer().goodbye(xmlTasks);
		this.runFinishedCheckerThread.requestStop(100, TimeUnit.MILLISECONDS);
		// do all stuff before this or thread will be killed by itself!
		Platform.runLater(() -> this.stopWorkflow()); 
	}
	
	public void requestDetach() {
		if(!MonitorThread.wasDetachModeOnAllMonitorThreads()) {
			MonitorThread.setDetachModeOnAllMonitorThreads(true);
			StatusConsole.addGlobalMessage(MessageType.INFO, "Watchdog will stop to schedule new tasks and detach as soon as possible.");
		}
	}

	public boolean setPauseWorkflow(boolean pause) {
		if(!this.isProcessingActive())
			return false;
		
		this.runXml2taskThread.setPauseScheduling(pause);
		return true;
	}

	public boolean isProcessingPaused() {
		return this.isProcessingActive() && this.runXml2taskThread.isSchedulingPaused();
	}
	
	public boolean stopWorkflow() {
		if(this.isProcessingActive()) {
			if(this.resumeFile != null)
				StatusConsole.addGlobalMessage(MessageType.INFO, "Resume file was written to '"+this.resumeFile.getAbsolutePath()+"'");
			
			this.runFinishedCheckerThread.interrupt();
			this.runWatchdog.stopExecution();
			this.runWatchdog = null;
			this.resumeFile = null;
			
			if(this.runHttp != null) {
				this.runHttp.stop();
				this.runHttp = null;
			}
			this.runXml2taskThread.requestStop(5, TimeUnit.SECONDS);
			this.runFinishedCheckerThread = null;
			this.runXml2taskThread = null;
			MonitorThread.stopAllMonitorThreads(true);
			MonitorThread.setDetachModeOnAllMonitorThreads(false);
			return true;
		}
		return false;
	}

	private boolean isProcessingActive() {
		return this.runWatchdog != null && this.runXml2taskThread != null && this.runFinishedCheckerThread != null;
	}

	public static void enforcePreferences(Runnable r) {
		// collect base dir first!
		while(!PreferencesStore.hasWatchdogBaseDir()) {			
			openPreferences(WorkflowDesignerRunner.GENERAL_NAME, true, null, null);
		}
		
		// will be executed only once --> init plugins
		XMLParser.initPlugins(PreferencesStore.getWatchdogBaseDir(), new Logger(), true, true);
		// once we got out here, start the GUI
		Platform.runLater(r);
	}
}
