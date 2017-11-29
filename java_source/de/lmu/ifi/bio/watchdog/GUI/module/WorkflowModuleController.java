package de.lmu.ifi.bio.watchdog.GUI.module;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.GUI.WorkflowDesignController;
import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.MessageType;
import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.StatusConsole;
import de.lmu.ifi.bio.watchdog.GUI.css.CSSRessourceLoader;
import de.lmu.ifi.bio.watchdog.GUI.datastructure.ExtendedClipboardContent;
import de.lmu.ifi.bio.watchdog.GUI.datastructure.Module;
import de.lmu.ifi.bio.watchdog.GUI.helper.AddButtonToTitledPane;
import de.lmu.ifi.bio.watchdog.GUI.helper.LogView;
import de.lmu.ifi.bio.watchdog.GUI.helper.ParamCounts;
import de.lmu.ifi.bio.watchdog.GUI.helper.ParamValue;
import de.lmu.ifi.bio.watchdog.GUI.helper.ParameterToControl;
import de.lmu.ifi.bio.watchdog.GUI.helper.ScreenCenteredStage;
import de.lmu.ifi.bio.watchdog.GUI.helper.SuggestPopup;
import de.lmu.ifi.bio.watchdog.GUI.layout.Dependency;
import de.lmu.ifi.bio.watchdog.GUI.layout.InsertableGridPane;
import de.lmu.ifi.bio.watchdog.GUI.layout.RasteredGridPane;
import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import de.lmu.ifi.bio.watchdog.GUI.properties.Property;
import de.lmu.ifi.bio.watchdog.GUI.properties.PropertyData;
import de.lmu.ifi.bio.watchdog.GUI.properties.PropertyLine;
import de.lmu.ifi.bio.watchdog.GUI.useraction.CreateDependencyAction;
import de.lmu.ifi.bio.watchdog.GUI.useraction.MovePropertyAction;
import de.lmu.ifi.bio.watchdog.GUI.useraction.MoveWorkflowModuleAction;
import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.executor.WatchdogThread;
import de.lmu.ifi.bio.watchdog.helper.ActionType;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.ErrorCheckerStore;
import de.lmu.ifi.bio.watchdog.helper.Parameter;
import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import de.lmu.ifi.bio.watchdog.helper.returnType.DoubleReturnType;
import de.lmu.ifi.bio.watchdog.helper.returnType.FileReturnType;
import de.lmu.ifi.bio.watchdog.helper.returnType.IntegerReturnType;
import de.lmu.ifi.bio.watchdog.helper.returnType.ReturnType;
import de.lmu.ifi.bio.watchdog.helper.returnType.StringReturnType;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessBlock;
import de.lmu.ifi.bio.watchdog.task.TaskAction;
import de.lmu.ifi.bio.watchdog.task.TaskActionTime;
import de.lmu.ifi.bio.watchdog.task.TaskStatus;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Controller for module that is placed in the workflow
 * */
public class WorkflowModuleController implements XMLDataStore, Initializable {

	private static final long serialVersionUID = 2173438306802742989L;
	@FXML private AnchorPane module;
	@FXML private ImageView image;
	@FXML private AnchorPane input;
	@FXML private AnchorPane output;
	@FXML private Label label;
	@FXML private Pane moveArea;
	@FXML private HBox properties;
	
	private Property prop1;
	private Property prop2;
	private Property prop3;
	private Property prop4;
	
	private Module moduleData;
	private final WorkflowModuleData DATA = new WorkflowModuleData();
	private RasteredGridPane GRID;
	private int x = -1;
	private int y = -1;
	private final LinkedHashMap<String, ParamValue> PARAMETER = new LinkedHashMap<>();
	private final ParamCounts PARAM_COUNTS = new ParamCounts();
	private SimpleBooleanProperty configuredReady = new SimpleBooleanProperty(false);
	private static final String REPLACE_MULTI = "\\([0-9]+\\)$";
	private static final Pattern MULT_PARAM_PATTTERN = Pattern.compile("^.+ "+REPLACE_MULTI);

	private static final ArrayList<WorkflowModuleController> CONTROLLER = new ArrayList<>();
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// get properties that are hidden in first place
		this.prop1 = Property.getProperty(null, null, null, false);
		this.prop2 = Property.getProperty(null, null, null, false);
		this.prop3 = Property.getProperty(null, null, null, false);
		this.prop4 = Property.getProperty(null, null, null, false);
		
		// add them
		this.properties.getChildren().add(this.prop1);
		this.properties.getChildren().add(this.prop2);
		this.properties.getChildren().add(this.prop3);
		this.properties.getChildren().add(this.prop4);
		
		// add drag start action
		this.moveArea.setOnDragDetected(event -> this.onStartModuleDrag(event));
		
		// input and output event handlers
		this.input.setOnDragDetected(event -> this.onStartDependencyInputDrag(event));
		this.output.setOnDragDetected(event -> this.onStartDependencyOutputDrag(event));
		this.input.setOnDragOver(event -> this.onOverInputOrOutput(event, false));
		this.output.setOnDragOver(event -> this.onOverInputOrOutput(event, true));
		this.input.setOnDragDropped(event -> this.onDropDependency(event, true));
		this.output.setOnDragDropped(event -> this.onDropDependency(event, false));
		
		// set event handler to properties
		this.prop1.setOnDragDone(event -> this.onPropertyMoved(event, this.prop1));
		this.prop2.setOnDragDone(event -> this.onPropertyMoved(event, this.prop2));
		this.prop3.setOnDragDone(event -> this.onPropertyMoved(event, this.prop3));
		this.prop4.setOnDragDone(event -> this.onPropertyMoved(event, this.prop4));
		this.properties.setOnDragOver(event -> this.onDragOverProperties(event));
		this.properties.setOnDragDropped(event -> this.onDroppedProperty(event));
		
		// add parameter event handler
		this.module.setOnMouseClicked(event -> this.onMouseClickedOnModule(event));
		WorkflowDesignController.bindConfiguredReady(this.configuredReady);
		 
		// add the controller
		CONTROLLER.add(this);
	}
	
	@Override
	public void finalize() {
		// ensure that the value of the deleted modules is set to true
		this.configuredReady.set(true);
	}
	
	private ModuleProperties getModuleProperties(Stage stage, boolean readOnly) {
		// create the parameter views
		InsertableGridPane optionalGrid = new InsertableGridPane();
		InsertableGridPane requiredGrid = new InsertableGridPane();
		requiredGrid.setPadding(new Insets(10));
		optionalGrid.setPadding(new Insets(10));
		requiredGrid.setVgap(5);
		requiredGrid.setHgap(5);
		optionalGrid.setVgap(5);
		optionalGrid.setHgap(5);

		LinkedHashMap<String, ParamValue> params = new LinkedHashMap<>();
		ParamCounts counts = new ParamCounts();
		ModuleProperties pWindow = ModuleProperties.getModuleProperties(requiredGrid, optionalGrid, this);
		
		// init parameters
		for(Parameter p : this.moduleData.getParameter().values()) {
			String pName = p.getName();
			int minNumber = p.isOptional() ? 1 : p.getMin();
			int addedCounter = 0;
			// adjust min number if already some data is stored.
			if(this.PARAM_COUNTS.hasCount(pName)) {
				addedCounter = this.PARAM_COUNTS.getCount(pName) - minNumber; 
				minNumber = this.PARAM_COUNTS.getCount(pName);
			}
			boolean onlySingleInstance = (p.getMax() != null && p.getMax() == 1);
			
			Integer maxNumber = p.getMax();
			int i = 0;
			// check to which grid the options should be added
			final InsertableGridPane grid = !p.isOptional() ? requiredGrid : optionalGrid;
			Button lastAdd = null;
			// add the elements
			while(i < minNumber) {
				final ParamValue pv = new ParamValue(p.getName(), ParameterToControl.getControlElement(p.getType()), (!onlySingleInstance ? i+1 : null));
				params.put(pv.getName(), pv);
				int c = grid.getRowCount();
				grid.add(new Label(pv.getName()), 0, c);
				grid.add(pv.getControl(), 1, c);
				counts.increaseCount(pv.getPlainName());
				
				ReturnType retType = p.getType();
				// enforce non optional parameters to contain a value
				if(!p.isOptional() && pv.getControl() instanceof Control) {
					if(pv.getControl() instanceof TextField) {
						TextField t = (TextField) pv.getControl();
						if(retType instanceof StringReturnType)
							pWindow.addValidateToControl(t, f -> !pWindow.controller.isEmpty((TextField) f, "Parameter with name '"+pv.getPlainName()+"' can not be empty."));	
						else if(retType instanceof IntegerReturnType)
							pWindow.addValidateToControl(t, f -> pWindow.controller.isInteger((TextField) f, "Parameter with name '"+pv.getPlainName()+"' must be an integer."));	
						else if(retType instanceof DoubleReturnType)
							pWindow.addValidateToControl(t, f -> pWindow.controller.isDouble((TextField) f, "Parameter with name '"+pv.getPlainName()+"' must be a numeric value."));	
						else if(retType instanceof FileReturnType)
							pWindow.addValidateToControl(t, f -> pWindow.controller.isValidReturnType((TextField) f, retType, "Parameter with name '"+pv.getPlainName()+"' must be an valid" + retType.toString() + "."));	
					}
				}
				// add suggest variables support
				if(pv.getControl() instanceof TextField) {
					TextField t = (TextField) pv.getControl();
					@SuppressWarnings("unused")
					SuggestPopup sp = new SuggestPopup(t, this.GRID, this.hasProcessBlockProperty() ? this.getProcessBlockProperty() : null, this.getKey(), true);
					t.textProperty().addListener(ev -> pWindow.validate());
				}
				
				// create the add Button
				if(i == 0) {
					final int number = i + 2 + addedCounter;
					Button add = new Button("");
					lastAdd = add;
					add.setGraphic(ImageLoader.getImage(ImageLoader.ADD_SMALL));
					add.onActionProperty().set(e -> { this.createCopyOfArgument(grid, pv, number, stage, add, p, pWindow); e.consume();});
					grid.add(add, 2, grid.getRowCount()-1);
					// add add button in first row
					if(!(maxNumber == null || (minNumber < maxNumber))) {
						add.setVisible(false);
					}
					
				}
				
				// check, if remove button must be added
				if(minNumber-i <= addedCounter) {
					Button add = lastAdd;
					Button remove = new Button();
					remove.setGraphic(ImageLoader.getImage(ImageLoader.DELETE_SMALL));
					grid.add(remove, 2, grid.getRowCount()-1);
					remove.onActionProperty().set(e -> { this.removeCopyOfArgument(pv.getName(), grid, stage, pv.getPlainName(), add); e.consume();});
				}
				i++;
			}
		}
		// try to load some data
		// get the keys in the same order
		ArrayList<String> storedParamsNames = new ArrayList<>(this.PARAMETER.keySet()); 
		ArrayList<String> newParamsNames = new ArrayList<>(params.keySet());
		Collections.sort(storedParamsNames);
		Collections.sort(newParamsNames);

		// update the values
		int paramCount = 0;
		for(int valueCount = 0; valueCount < storedParamsNames.size(); valueCount++) {
			// if names are equal --> all is good
			String paramName = newParamsNames.get(paramCount);
			String storeName = storedParamsNames.get(valueCount);
			// check if names are equal
			if(paramName.equals(storeName)) {
				params.get(paramName).updateValue(this.PARAMETER.get(paramName).getSavedValue());
				params.get(paramName).saveCurrentValue();
				paramCount++;
			}
			// check, if shift of count numbers is there (in case of edits (+/-)
			else if(MULT_PARAM_PATTTERN.matcher(paramName).matches() && MULT_PARAM_PATTTERN.matcher(storeName).matches()) {
				// check, if both have the same name
				if(paramName.replaceAll(REPLACE_MULTI, "").equals(storeName.replaceAll(REPLACE_MULTI, ""))) {
					params.get(paramName).updateValue(this.PARAMETER.get(storeName).getSavedValue());
					params.get(paramName).saveCurrentValue();
					paramCount++;
				}
			}
			// value was not used --> check if no value is there
			else {
				if(paramName.compareTo(storeName) < 0) {
					paramCount++;
					valueCount--;
				}
			}
			
			// end checks, if parameters out of range
			
			if(!(paramCount < newParamsNames.size()))
				break;
		}
				
		// save the new properties
		this.PARAMETER.clear();
		this.PARAM_COUNTS.clear();
		this.PARAMETER.putAll(params);
		this.PARAM_COUNTS.set(counts);

		// make the stuff read-only
		pWindow.setReadOnly(readOnly);
		return pWindow;
	}
	
	private void onMouseClickedOnModule(MouseEvent event) {
		if(event.getButton().compareTo(MouseButton.PRIMARY) == 0) {			
			// create the window
			Stage stage = new ScreenCenteredStage();
			stage.setTitle("Propertymanager");
			stage.setResizable(true);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setMinHeight(647);
			stage.setMinWidth(600);
			
			ModuleProperties pWindow = this.getModuleProperties(stage, WorkflowDesignController.isInExecutionMode());

			// show the window
			Scene scene = new Scene(pWindow);
			try { scene.getStylesheets().add(CSSRessourceLoader.getCSS("control.css")); } catch(Exception e) {e.printStackTrace();}
			stage.setScene(scene);
			
			// set status console
			String propTabName = "Task properties: " + this.getLabel();
			StatusConsole c = StatusConsole.getStatusConsole(propTabName);
			WorkflowDesignController.addNewTab(c);
			pWindow.controller.setStatusConsole(c);
			
			stage.show();
			pWindow.validate(); // get initial coloring
			stage.hide();
			AddButtonToTitledPane.initAddButtonsAsGUIisLoaded();			
			stage.showAndWait();

			// remove the console once with window is closed
			WorkflowDesignController.removeTab(propTabName);
		}
		else
			this.showContextMenu(event);
		
		event.consume();
	}
	
	public String getKey() {
		return RasteredGridPane.getKey(this.x, this.y);
	}

	private void createCopyOfArgument(InsertableGridPane grid, ParamValue param, int nextNumber, Window w, Button add, Parameter p, ModuleProperties pWindow) {
		String plainName = param.getPlainName();
		Button remove = new Button();
		remove.setGraphic(ImageLoader.getImage(ImageLoader.DELETE_SMALL));
		ParamValue newNumber = param.copy(nextNumber);
		int insertPosition = grid.getLastBaseName(plainName);
		grid.insertRow(insertPosition, new Label(newNumber.getName()), newNumber.getControl(), remove); 
		w.setHeight(w.getHeight() + 32);
		
		// update counts
		this.PARAM_COUNTS.increaseCount(plainName);
		this.PARAMETER.put(newNumber.getName(), newNumber);
		
		// update event handler
		add.onActionProperty().set(e -> { this.createCopyOfArgument(grid, newNumber, nextNumber + 1, w, add, p, pWindow); e.consume();});
		remove.onActionProperty().set(e -> { this.removeCopyOfArgument(newNumber.getName(), grid, w, newNumber.getPlainName(), add) ;e.consume();});
		
		// add event handler for validate if not in optional range
		if(!p.isOptional() && newNumber.getControl() instanceof TextField) {
			TextField t = (TextField) newNumber.getControl();
			ReturnType retType = p.getType();
			if(retType instanceof StringReturnType)
				pWindow.addValidateToControl(t, f -> !pWindow.controller.isEmpty((TextField) f, "Parameter with name '"+newNumber.getPlainName()+"' can not be empty."));	
			else if(retType instanceof IntegerReturnType)
				pWindow.addValidateToControl(t, f -> pWindow.controller.isInteger((TextField) f, "Parameter with name '"+newNumber.getPlainName()+"' must be an integer."));	
			else if(retType instanceof DoubleReturnType)
				pWindow.addValidateToControl(t, f -> pWindow.controller.isDouble((TextField) f, "Parameter with name '"+newNumber.getPlainName()+"' must be a numeric value."));	
			else if(retType instanceof FileReturnType)
				pWindow.addValidateToControl(t, f -> pWindow.controller.isValidReturnType((TextField) f, retType, "Parameter with name '"+newNumber.getPlainName()+"' must be an valid" + retType.toString() + "."));				
			// validate the window
			pWindow.validate();
		}
		// add suggest variables support
		if(newNumber.getControl() instanceof TextField) {
			TextField t = (TextField) newNumber.getControl();
			@SuppressWarnings("unused")
			SuggestPopup sp = new SuggestPopup(t, this.GRID, this.hasProcessBlockProperty() ? this.getProcessBlockProperty() : null, this.getKey(), true);
			t.textProperty().addListener(ev -> pWindow.validate());
		}
		
		// check, if we can add some more
		Parameter pp = this.moduleData.getParameter().get(plainName);
		if(!pp.isUnbounded() && pp.getMax() <= this.PARAM_COUNTS.getCount(plainName)) 
			add.setVisible(false);
	}
	
	private void removeCopyOfArgument(String label, InsertableGridPane grid, Window w, String plainName, Button add) {
		// find row to delete
		Integer deleteRow = grid.getRowNumber(label);
		if(deleteRow != null) {
			grid.deleteRow(deleteRow);
			w.setHeight(w.getHeight() - 32);
			
			// update counts
			this.PARAM_COUNTS.decreaseCount(plainName);
			this.PARAMETER.remove(label);;
			
			// check, if we can add some more again
			Parameter p = this.moduleData.getParameter().get(plainName);
			if(p.isUnbounded() || p.getMax() > this.PARAM_COUNTS.getCount(plainName) && add != null) 
				add.setVisible(true);
		}
	}

	protected void onSave(ModulePropertiesController gui) {
		boolean status = true;
		XMLDataStore.unregisterData(this);
		
		// save name and max running stuff
		this.DATA.name = gui.name.getText();
		try { 
				int maxR = Integer.parseInt(gui.maxRunning.getText());
				this.DATA.simMaxRunning = maxR;
			} catch(Exception e) {
				this.DATA.simMaxRunning = -1;
			}
		// save the radio boxes
		this.DATA.checkpoint = ActionType.getType(((RadioButton) gui.checkpoint.getSelectedToggle()).getText());
		this.DATA.confirm = ActionType.getType(((RadioButton) gui.confirm.getSelectedToggle()).getText());
		this.DATA.notify = ActionType.getType(((RadioButton) gui.notify.getSelectedToggle()).getText());
		this.DATA.enforceStdin = gui.enforceStdin.isSelected();
				
		// save the streams
		this.DATA.stdErr = gui.stderr.getText();
		this.DATA.stdOut = gui.stdout.getText();
		this.DATA.stdIn = gui.stdin.getText();
		this.DATA.workingDir = gui.workingDir.getText();
		this.DATA.appendErr = gui.appendErr.isSelected();
		this.DATA.appendOut = gui.appendOut.isSelected();
		this.DATA.saveRes = gui.saveRes.isSelected();
		
		// save the parameters
		for(ParamValue p : this.PARAMETER.values())
			p.saveCurrentValue();
		
		// save error checkers
		this.DATA.CHECKERS.clear();
		for(ErrorCheckerStore e : gui.getActiveCheckers()) {
			this.DATA.addErrorChecker(e);
		}
		// save actions
		this.DATA.ACTIONS.clear();
		for(TaskAction a : gui.getActiveTaskActions()) {
			this.DATA.addTaskAction(a);
		}
		
		this.setStatusImage(status, false);
		XMLDataStore.registerData(this);
	}
	
	private void setStatusImage(boolean allOk, boolean isUnchangedFromSavedVersion) {
		// change the image
		if(allOk)
			if(isUnchangedFromSavedVersion)
				this.setImage(ImageLoader.getImage(ImageLoader.GREEN).getImage());
			else {
				this.setImage(ImageLoader.getImage(ImageLoader.YELLOW).getImage());
			}
		else
			this.setImage(ImageLoader.getImage(ImageLoader.RED).getImage());
		
		// update the value for the save/button
		this.configuredReady.set(allOk);
	}
	
	public boolean isModuleConfigured() {
		return this.configuredReady.get();
	}

	private void showContextMenu(MouseEvent event) {
		ContextMenu contextMenu = new ContextMenu();
		
		// switch between design and run mode
		if(!WorkflowDesignController.isInExecutionMode()) {	
	
			// construct contextMenu
			SeparatorMenuItem sep1 = new SeparatorMenuItem();
			SeparatorMenuItem sep2 = new SeparatorMenuItem();
	
			// get menu items
			MenuItem cut = new MenuItem("cut task");
			MenuItem copy = new MenuItem("copy task");
			MenuItem delete = new MenuItem("delete task");
			MenuItem dd = new MenuItem("delete dependencies");
			MenuItem id = new MenuItem("delete incoming dependencies");
			MenuItem od = new MenuItem("delete outgoing dependencies");
			MenuItem properties = new MenuItem("remove assigned properties");
			contextMenu.getItems().addAll(cut, copy, delete, sep1, dd, id, od, sep2, properties);
			
			// add the event handler
			properties.onActionProperty().set(e -> this.removeAllProperties());
			delete.onActionProperty().set(e -> { this.GRID.deleteModule(this.x, this.y, true); StatusConsole.addGlobalMessage(MessageType.INFO, "task was deleted at "+RasteredGridPane.getKey(this.x, this.y));});
			dd.onActionProperty().set(e -> { this.GRID.removeDependencies(this.x, this.y, true); this.GRID.removeDependencies(this.x, this.y, false); StatusConsole.addGlobalMessage(MessageType.INFO, "dependencies from "+RasteredGridPane.getKey(this.x, this.y)+" were deleted");});
			id.onActionProperty().set(e -> {this.GRID.removeDependencies(this.x, this.y, true); StatusConsole.addGlobalMessage(MessageType.INFO, "incoming dependencies from "+RasteredGridPane.getKey(this.x, this.y)+" were deleted");});
			od.onActionProperty().set(e -> { this.GRID.removeDependencies(this.x, this.y, false); StatusConsole.addGlobalMessage(MessageType.INFO, "outgoing dependencies from "+RasteredGridPane.getKey(this.x, this.y)+" were deleted");});
			cut.onActionProperty().set(e -> this.GRID.setClipboard(this.x, this.y, true));
			copy.onActionProperty().set(e -> this.GRID.setClipboard(this.x, this.y, false));
			
			// decide which options should be active
			ArrayList<Dependency> incoming = this.GRID.getDependencies(this.x, this.y, true);
			ArrayList<Dependency> outgoing = this.GRID.getDependencies(this.x, this.y, false);
			if(incoming.size() == 0 && outgoing.size() == 0)
				dd.setDisable(true);
			if(incoming.size() == 0)
				id.setDisable(true);
			if(outgoing.size() == 0)
				od.setDisable(true);
			if(!this.prop1.hasPropertyData() && !this.prop2.hasPropertyData() && !this.prop3.hasPropertyData() && !this.prop4.hasPropertyData())
				properties.setDisable(true);
		}
		else {
			// offer streams
			MenuItem out = new MenuItem("display standard output");
			MenuItem err = new MenuItem("display standard error");
			// offer return parameters
			MenuItem returnp = new MenuItem("display return parameters");
			MenuItem release = new MenuItem("release task");
			contextMenu.getItems().addAll(out, err, returnp, release);
		
			// decide which options should be active
			if(this.getSavedData().stdOut == null || this.getSavedData().stdOut.isEmpty()) {
				out.setDisable(true);
			}
			else {
				File o = new File(this.getSavedData().stdOut);
				out.onActionProperty().set(x -> this.showLogfile(o));
			}
			if(this.getSavedData().stdErr == null || this.getSavedData().stdErr.isEmpty()) {
				err.setDisable(true);
			}
			else {
				File e = new File(this.getSavedData().stdErr);
				err.onActionProperty().set(x -> this.showLogfile(e));
			}
			// test for release
			XMLTask x = XMLTask.getXMLTask(this.DATA.id);
			if(!x.isSomeTaskBlocked() || x.hasRunningTasks()) {
				release.setDisable(true);
			}
			else {
				release.onActionProperty().set(z -> x.releaseAllTasksFromCheckpoint());	
			}
		}
		// show the contextmenu
		contextMenu.show(this.module.getScene().getWindow(), event.getScreenX(), event.getScreenY());
	}
	
	private void showLogfile(File log) {
		// check, if file is readablef
		if(!(log != null && log.exists() && log.isFile() && log.canRead()))
			return;
		
		// open window to show the log
		Stage stage = new ScreenCenteredStage();
		stage.setTitle("LogViewer: " + log.getName());
		stage.setResizable(true);
		stage.initModality(Modality.WINDOW_MODAL);
		LogView viewer = LogView.getLogViewer(log);
		Scene scene = new Scene(viewer);
		stage.setScene(scene);	
		stage.show();
	}


	private void removeAllProperties() {
		this.prop1.setPropertyData(null, null);
		this.prop2.setPropertyData(null, null);
		this.prop3.setPropertyData(null, null);
		this.prop4.setPropertyData(null, null);
	}

	/**
	 * can be called when a property was deleted
	 * @param type
	 * @param number
	 */
	public static void deleteProperty(XMLDataStore data, int number){
		for(WorkflowModuleController c : CONTROLLER) {
			Property pGUI = getPropertyToUse(c, data);
			if(pGUI != null && pGUI.hasPropertyData() && pGUI.getPropertyData().hasXMLData() && pGUI.getPropertyData().getNumber() == number)
				pGUI.setPropertyData(null, null);
		}
	}
	
	/**
	 * is used to update properties that can be appended (note to me: never implement such a feature again!)
	 * @param data
	 * @param number
	 */
	public static void updateAppendProperties(XMLDataStore data, Integer number, Integer newNumber, Color col){
		if(data == null || number == null)
			return;
		for(WorkflowModuleController c : CONTROLLER) {
			Property pGUI = getPropertyToUse(c, data);
			if(pGUI != null && pGUI.hasPropertyData() && pGUI.getPropertyData().hasXMLData() && pGUI.getPropertyData().getNumber() == number) {
				pGUI.getPropertyData().setXMLData(data);
				pGUI.getPropertyData().setNumber(newNumber, false);
				pGUI.getPropertyData().setColor(col, false);
				pGUI.getPropertyData().setDisplayNumber(newNumber, col);
			}
		}
	}
	
	public void setModuleData(Module m) {
		this.moduleData = m;
		this.setLabel(this.moduleData.getName());
		this.setImage(new Image(this.moduleData.getImage()));
	}

	public void setCoordinates(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	protected void setImage(Image i) {
		this.image.setImage(i);
	}

	public void setGrid(RasteredGridPane grid) {
		this.GRID = grid;
	}

	protected void setLabel(String label) {
		this.label.setText(label);
	}
	
	public String getLabel() {
		return this.label.getText();
	}
	public Image getImage() {
		return this.image.getImage();
	}
	public RasteredGridPane getGrid() {
		return this.GRID;
	}
	
	/************************************************* EVENT HANDLER ***************************************/
	/**
	 * drag of module is started
	 * @param event
	 */
	private void onStartModuleDrag(MouseEvent event) {
		// no drag&drop in read-only mode
		if(WorkflowDesignController.isInExecutionMode())
			return;
		
		Dragboard db = this.moveArea.startDragAndDrop(TransferMode.MOVE);
		ExtendedClipboardContent content = new ExtendedClipboardContent();
        content.putUserAction(new MoveWorkflowModuleAction(x, y));
        db.setContent(content);
        event.consume();
	}
	
	/**
	 * drag of new dependency is started
	 * @param event
	 * @param startIsOutput
	 */
	private void onStartDependencyDrag(MouseEvent event, boolean startIsOutput) {
		// no drag&drop in read-only mode
		if(WorkflowDesignController.isInExecutionMode())
			return;
		
		Dragboard db = this.module.startDragAndDrop(TransferMode.LINK);
		ExtendedClipboardContent content = new ExtendedClipboardContent();
        content.putUserAction(new CreateDependencyAction(x, y, startIsOutput));
        db.setContent(content); 
        event.consume();
	}
	private void onStartDependencyInputDrag(MouseEvent event) {
		// no drag&drop in read-only mode
		if(WorkflowDesignController.isInExecutionMode())
			return;
		
		this.onStartDependencyDrag(event, false);
	}
	private void onStartDependencyOutputDrag(MouseEvent event) {
		// no drag&drop in read-only mode
		if(WorkflowDesignController.isInExecutionMode())
			return;
		
		this.onStartDependencyDrag(event, true);
	}
	
	/**
	 * a drag that contains should result in creation of a new link is over input or output token
	 * @param event
	 * @param isOverOutput
	 */
	private void onOverInputOrOutput(DragEvent event, boolean isOverOutput) {
		CreateDependencyAction c = CreateDependencyAction.getCreateDependencyAction(event);
		if(c != null) {
			boolean accept = true;
			String originKey = c.getKey();
			String targetKey = RasteredGridPane.getKey(this.x, this.y);
			// we don't want to link it with itself
			if(originKey.equals(targetKey))
				accept = false;
			
			// we dont't want to link input with input or output with output
			if(isOverOutput == c.isStartOutput())
				accept = false;
			
			// we don't want to create dependencies that already exit
			if(this.GRID.isDependencySet(originKey, targetKey))
				accept = false;
				
			// accept the new dependency and show linking symbol
			if(accept)
				event.acceptTransferModes(TransferMode.LINK);
		}
		event.consume();
	}
	
	/**
	 * a new dependency was finally dropped over input or ouput
	 * @param event
	 * @param isFirstOutput
	 */
	private void onDropDependency(DragEvent event, boolean isFirstOutput) {
		boolean success = false;
		CreateDependencyAction c = CreateDependencyAction.getCreateDependencyAction(event);
		if(c != null) {
			Pair<Integer, Integer> start = c.getCoordinates();
			this.GRID.addDependency(start.getKey(), start.getValue(), this.x, this.y, isFirstOutput);
			StatusConsole.addGlobalMessage(MessageType.INFO, "dependency between "+RasteredGridPane.getKey(start)+" and "+RasteredGridPane.getKey(this.x, this.y)+" was added");
		}
		event.setDropCompleted(success);
		event.consume();
	}
	
	/**
	 * is called when a property is dropped over the property Hbox "properties"
	 * @param event
	 */
	private void onDroppedProperty(DragEvent event) {
		MovePropertyAction m = MovePropertyAction.getMovePropertyAction(event);
		if(m != null) {
			this.setPropertyData(m.getPropertyData(), m.getParentLine());
		}
		event.consume();
	}
	
	public void setPropertyData(PropertyData p, PropertyLine parent) {
		Property pGUI = getPropertyToUse(this, p.getXMLData());
		if(pGUI != null) { // set the new data there 
			pGUI.setPropertyData(p, parent);
		}
	}
			
	/**
	 * returns the property field that should be used depending on the type of the data
	 * @param p
	 * @return
	 */
	protected static Property getPropertyToUse(WorkflowModuleController c, XMLDataStore data) {
		if(data instanceof ProcessBlock)
			return c.prop3;
		else if(data instanceof ExecutorInfo)
			return c.prop2;
		else if(data instanceof Environment)
			return c.prop1;
		else
			return null;
	}
	
	/*** checker, if properties are there ***/
	public boolean hasEnvironmentProperty() {
		return this.prop1.hasPropertyData() && this.prop1.getPropertyData().hasXMLData();
	}
	public boolean hasExecutorProperty() {
		return this.prop2.hasPropertyData() && this.prop2.getPropertyData().hasXMLData();
	}
	public boolean hasProcessBlockProperty() {
		return this.prop3.hasPropertyData() && this.prop3.getPropertyData().hasXMLData();
	}
	/*** getter of properties ***/
	public Environment getEnvironmentProperty() {
		return (Environment) this.prop1.getPropertyData().getXMLData();
	}
	public ExecutorInfo getExecutorProperty() {
		return (ExecutorInfo) this.prop2.getPropertyData().getXMLData();
	}
	public ProcessBlock getProcessBlockProperty() {
		return (ProcessBlock) this.prop3.getPropertyData().getXMLData();
	}

	/**
	 * is called when a element is dragged over the Hbox "properties"
	 * @param event
	 */
	private void onDragOverProperties(DragEvent event) {
		MovePropertyAction m = MovePropertyAction.getMovePropertyAction(event);
		if(m != null) {
			if(m.getPropertyData() != null)
				event.acceptTransferModes(TransferMode.LINK);
		}
		event.consume();
	}

	/**
	 * is called when a property is moved away
	 * @param event
	 * @return
	 */
	private void onPropertyMoved(DragEvent event, Property p) {
		// copy the property if control is active
		if(!WorkflowDesignController.isKeyActive(KeyCode.CONTROL)) {
			p.setPropertyData(null, null);
		}
		event.consume();
	}

	public Pair<Integer, Integer> getPosition() {
		return Pair.of(this.x, this.y);
	}

	public void copyProperties(WorkflowModuleController source) {
		if(source.prop1.hasPropertyData()) this.prop1.setPropertyData(source.prop1.getPropertyData(), source.prop1.getParentLine());
		if(source.prop2.hasPropertyData()) this.prop2.setPropertyData(source.prop2.getPropertyData(), source.prop2.getParentLine());
		if(source.prop3.hasPropertyData()) this.prop3.setPropertyData(source.prop3.getPropertyData(), source.prop3.getParentLine());
		if(source.prop4.hasPropertyData()) this.prop4.setPropertyData(source.prop4.getPropertyData(), source.prop4.getParentLine());
	}

	public Module getModuleData() {
		return this.moduleData;
	}

	@Override
	public String toXML() {
		XMLBuilder b = new XMLBuilder();
		b.startTag(this.moduleData.getName(), false);
		if(this.DATA.id == null || this.DATA.id <= 0)
			this.DATA.id = WorkflowModule.getNextID();
		
		b.addQuotedAttribute(XMLParser.ID, this.DATA.id);
		b.addQuotedAttribute(XMLParser.NAME, this.DATA.name);
		
		// add maxSimRunning
		if(this.DATA.simMaxRunning != -1) b.addQuotedAttribute(XMLParser.MAX_RUNNING, this.DATA.simMaxRunning);
		
		// add boolean arguments
		if(!this.DATA.notify.isDisabled()) b.addQuotedAttribute(XMLParser.NOTIFY, this.DATA.notify);
		if(!this.DATA.checkpoint.isDisabled()) b.addQuotedAttribute(XMLParser.CHECKPOINT, this.DATA.checkpoint);
		if(!this.DATA.confirm.isDisabled()) b.addQuotedAttribute(XMLParser.CONFIRM_PARAM, this.DATA.confirm);
		
		if(this.hasEnvironmentProperty())
			b.addQuotedAttribute(XMLParser.ENVIRONMENT, this.getEnvironmentProperty().getName());
		if(this.hasProcessBlockProperty())
			b.addQuotedAttribute(XMLParser.PROCESS_BLOCK, this.getProcessBlockProperty().getName());
		if(this.hasExecutorProperty() && !this.getExecutorProperty().isDefaultExecutor())
			b.addQuotedAttribute(XMLParser.EXECUTOR, this.getExecutorProperty().getName());
		
		// write GUI position
		b.addQuotedAttribute(XMLParser.POSX, this.getPosition().getKey());
		b.addQuotedAttribute(XMLParser.POSY, this.getPosition().getValue());
		b.endOpeningTag();
		
		// add dependencies
		HashMap<Dependency, WorkflowModule> dependencies = this.getDependencies(true);
		if(dependencies.size() > 0) {
			XMLBuilder depx = new XMLBuilder();
			depx.startTag(XMLParser.DEPENDENCIES, false);
			depx.endOpeningTag();
			for(Dependency d : dependencies.keySet()) {
				WorkflowModule m = dependencies.get(d);
				depx.startTag(XMLParser.DEPENDS, true, true);
				// check for separate dependency
				if(d.isSeparateDependency()) {
					depx.addQuotedAttribute(XMLParser.SEPARATE, true);
					if(d.getPrefixLength() != null && d.getPrefixLength() > 0) {
						depx.addQuotedAttribute(XMLParser.PREFIX_NAME, "[" + d.getPrefixLength() + "]");
						
						if(!d.getSeparator().equals("."))
							depx.addQuotedAttribute(XMLParser.SEP, d.getSeparator());
					}
				}
				depx.addContentAndCloseTag(Integer.toString(m.getSavedData().id));
			}
			depx.endCurrentTag(false);
			b.addContent(depx.toString(), true);
		}
		
		// add parameters
		if(this.PARAMETER.size() > 0) {
			XMLBuilder parameter = new XMLBuilder();
			parameter.startTag(XMLParser.PARAMETER, false);
			parameter.endOpeningTag(false);
			for(String key : this.PARAMETER.keySet()) {
				ParamValue p = this.PARAMETER.get(key);
				if(p.hasSavedValue() && p.getCurrentValue().length() > 0) {
					parameter.newline();
					parameter.addContent(p.toString(), false);
				}
			}
			parameter.endCurrentTag(false);
			b.addContent(parameter.toString(), false);
		}
		
		// add streams
		if((this.DATA.workingDir != null && this.DATA.workingDir.length() > 0) || (this.DATA.stdErr != null && this.DATA.stdErr.length() > 0) || (this.DATA.stdOut != null && this.DATA.stdOut.length() > 0) || (this.DATA.stdIn != null && this.DATA.stdIn.length() > 0)) {
			XMLBuilder streams = new XMLBuilder();
			streams.newline();
			streams.startTag(XMLParser.STREAMS, false);
			if(this.DATA.saveRes)
				streams.addQuotedAttribute(XMLParser.SAVE_RESOURCE_USAGE, true);
			streams.endOpeningTag(false);
			boolean content = false;
			if(this.DATA.stdErr != null) {
				streams.startTag(XMLParser.STD_ERR, true, true);
				if(this.DATA.appendErr)
					streams.addQuotedAttribute(XMLParser.APPEND, true);
				streams.addContentAndCloseTag(XMLParser.ensureAbsoluteFile(this.DATA.stdErr));
				content = true;
			}
			if(this.DATA.stdOut != null) {
				streams.startTag(XMLParser.STD_OUT, true, true);
				if(this.DATA.appendOut)
					streams.addQuotedAttribute(XMLParser.APPEND, true);				
				streams.addContentAndCloseTag(XMLParser.ensureAbsoluteFile(this.DATA.stdOut));
				content = true;
			}
			if(this.DATA.stdIn != null) {
				streams.startTag(XMLParser.STD_IN, true, true);
				if(!this.DATA.enforceStdin)
					streams.addQuotedAttribute(XMLParser.DISABLE_EXISTANCE_CHECK, true);
				streams.addContentAndCloseTag(XMLParser.ensureAbsoluteFile(this.DATA.stdIn));
				content = true;
			}
			if(this.DATA.workingDir != null && !this.DATA.workingDir.equals(WatchdogThread.DEFAULT_WORKDIR)) {
				streams.startTag(XMLParser.WORKING_DIR, true, true);
				streams.addContentAndCloseTag(XMLParser.ensureAbsoluteFile(this.DATA.workingDir) + File.separator);
				content = true;
			}
			streams.endCurrentTag();
			// add it finally to the XML document if some content is there
			if(content)
				b.addContent(streams.toString(), false);
		}

		// add actions if some are there
		if(this.DATA.ACTIONS.size() > 0) {
			HashMap<Pair<TaskActionTime, Boolean>, ArrayList<TaskAction>> groups = new HashMap<>();
			// group actions that should be executed at the same time
			for(TaskAction a : this.DATA.ACTIONS) {
				TaskActionTime t = a.getActionTime();
				boolean unc = a.isUncoupledFromExecutor();
				Pair<TaskActionTime, Boolean> p = Pair.of(t, unc);
				if(!groups.containsKey(p))
					groups.put(p, new ArrayList<TaskAction>());
				
				groups.get(p).add(a);
			}
			
			// add action tags
			for(Pair<TaskActionTime, Boolean> p : groups.keySet()) {
				TaskActionTime t = p.getKey();
				boolean unc = p.getValue();
				
				XMLBuilder actions = new XMLBuilder();
				actions.newline();
				actions.startTag(XMLParser.ACTIONS, false);
				actions.addQuotedAttribute(XMLParser.TIME, t.getType());
				if(unc) actions.addQuotedAttribute(XMLParser.UNCOUPLE_FROM_EXECUTOR, unc);
				actions.endOpeningTag();
				
				// add all actions that belong to that group
				for(TaskAction a : groups.get(p)) {
					actions.addContent(a.toXML(), true);
				}
				actions.endCurrentTag(false);
				b.addContent(actions.toString(), false);
			}
		}
		
		// add checkers if some are there
		if(this.DATA.CHECKERS.size() > 0) {
			XMLBuilder checkers = new XMLBuilder();
			checkers.newline();
			checkers.startTag(XMLParser.CHECKERS, false);
			checkers.endOpeningTag();
			
			for(ErrorCheckerStore e : this.DATA.CHECKERS) {
				checkers.addContent(e.toXML(), true);
			}
			checkers.endCurrentTag(false);
			b.addContent(checkers.toString(), false);
		}
		return b.toString();
	}
	
	public HashMap<Dependency, WorkflowModule> getDependencies(boolean incoming) {
		HashMap<Dependency, WorkflowModule> r = new HashMap<>();
		ArrayList<Dependency> deps = this.GRID.getDependencies(this.x, this.y, incoming);
		for(Dependency dep : deps) {
			String first = dep.getFirstKey().getKey() + RasteredGridPane.SEP + dep.getFirstKey().getValue();
			WorkflowModule m = this.GRID.getModule(first); // origin of dependency
			r.put(dep, m);
		}
		return r;
	}
	
	public void setLoadedParameters(LinkedHashMap<String, ParamValue> params) {
		if(params != null) {
			this.PARAMETER.clear();
			for(String key : params.keySet()) {
				ParamValue p = params.get(key);
				// split the parameters correctly
				Parameter parameter = this.moduleData.getParameter().get(key);
				ArrayList<ParamValue> split = p.split(parameter.isOnlySingleInstanceAllowed());
				for(ParamValue c : split) {				
					this.PARAMETER.put(c.getName(), c);
				}
				this.PARAM_COUNTS.set(key, split.size());
			}
		}
		
		/// validate the module and set the correct status
		Stage s = new ScreenCenteredStage();
		ModuleProperties p = this.getModuleProperties(s, false);
		boolean ret = p.validate();
		this.setStatusImage(ret, true);
		XMLDataStore.registerData(this);
	}

	@Override
	public String getName() {
		return this.DATA.name;
	}

	public WorkflowModuleData getSavedData() {
		return this.DATA;
	}

	/**
	 * [WARN] x might be null
	 * @param t
	 * @param x
	 * @param requiresRelease
	 */
	public void setStatus(TaskStatus t, XMLTask x, boolean requiresRelease) {
		// only update icon if all tasks of a process block are finished
		if(x != null && x.hasRunningTasks() && x.hasProcessBlock() && x.getProcessBlock().size() > 1)
			return;
		
		if(t.isGUIRunning())
			this.setImage(ImageLoader.getImage(ImageLoader.RUNNING).getImage());
		if(t.isGUIFinished()) {
			if(x.isProcessingOfTaskFinished())
				this.setImage(ImageLoader.getImage(ImageLoader.FINISHED).getImage());
			else if(requiresRelease) {
				this.setImage(ImageLoader.getImage(ImageLoader.BLOCK).getImage());
			}
		}
		else if(t.isGUIWaitingQueue())
			this.setImage(ImageLoader.getImage(ImageLoader.QUEUE).getImage());
		else if(t.isGUIWaitingDependencies())
			this.setImage(ImageLoader.getImage(ImageLoader.QUEUE).getImage());
		else if(t.isGUIWaitingRestrictions())
			this.setImage(ImageLoader.getImage(ImageLoader.QUEUE).getImage());
		else if(t.isTerminated())
			this.setImage(ImageLoader.getImage(ImageLoader.DELETE).getImage());
		else if(t.isGUIFailed())
			this.setImage(ImageLoader.getImage(ImageLoader.DELETE).getImage());
	}
	
	@Override
	public void setColor(String c) {}
	@Override
	public String getColor() {return null;}

	/**
	 * updates the status of the image when the workflow was saved to a file (yellow -> green)
	 */
	public void wasSavedToFile() {
		this.setStatusImage(this.configuredReady.get(), true);
	}
	
	@Override
	public void onDeleteProperty() {}

	public void resetID() {
		this.DATA.id = null;
	}

	@Override
	public Object[] getDataToLoadOnGUI() { return null; }
}
