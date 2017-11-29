package de.lmu.ifi.bio.watchdog.GUI.module;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.MessageType;
import de.lmu.ifi.bio.watchdog.GUI.css.CSSRessourceLoader;
import de.lmu.ifi.bio.watchdog.GUI.helper.AddButtonToTitledPane;
import de.lmu.ifi.bio.watchdog.GUI.helper.ErrorCheckerStore;
import de.lmu.ifi.bio.watchdog.GUI.helper.ScreenCenteredStage;
import de.lmu.ifi.bio.watchdog.GUI.helper.SuggestPopup;
import de.lmu.ifi.bio.watchdog.GUI.helper.TextFilter;
import de.lmu.ifi.bio.watchdog.GUI.interfaces.Validator;
import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.ValidateViewController;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import de.lmu.ifi.bio.watchdog.helper.ProcessBlock.ProcessBlock;
import de.lmu.ifi.bio.watchdog.task.TaskAction;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Class that shows the propertoies of workflow modules
 * @author kluge
 *
 */
public class ModulePropertiesController extends ValidateViewController {

	@FXML protected TextField name;
	@FXML protected TextField maxRunning;
	@FXML protected ToggleGroup confirm;
	@FXML protected ToggleGroup notify;
	@FXML protected ToggleGroup checkpoint;
	@FXML protected CheckBox appendOut;
	@FXML protected CheckBox appendErr;
	@FXML protected CheckBox enforceStdin;
	@FXML protected TextField stdout;
	@FXML protected TextField stderr;
	@FXML protected TextField stdin;
	@FXML protected TextField workingDir;
	@FXML private TitledPane requiredPane;
	@FXML private TitledPane optionalPane;
	@FXML private TitledPane actionPane;
	@FXML private TitledPane checkerPane;
	@FXML private Button discardButton;
	@FXML private TitledPane streamsPane;
	@FXML private BorderPane border;
	@FXML private ScrollPane scroll;
	@FXML private VBox root;
	@FXML private VBox basicContainer;
	@FXML private VBox actionVBox;
	@FXML private VBox checkerVBox;
	
	// buttons that are added in the titled pane
	private Button addAction = new Button();
	private Button addChecker = new Button();
	
	// some external variables used for data transfer
	private WorkflowModuleController setWorkflowModule;
	private String initialName;
	private boolean readOnly = false;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {					
		// load images
		this.discardButton.setGraphic(ImageLoader.getImage(ImageLoader.DELETE_SMALL));
		this.saveButton.setGraphic(ImageLoader.getImage(ImageLoader.SAVE_SMALL));
		this.saveButton.onActionProperty().set(e -> this.onSave());
		
		// add event handler
		this.discardButton.onActionProperty().set(event -> { this.close(); event.consume();});
		
		// add checker
		this.addValidateToControl(this.name, t -> this.hasUniqueName(this.name.getText()));
		this.name.textProperty().addListener(e -> this.validate());
		this.name.setTextFormatter(TextFilter.getTaskNameFormater());
		
		// expand correctly
		Platform.runLater(() -> { if(this.root.getScene() != null) this.border.prefHeightProperty().bind(this.root.getScene().heightProperty());});
		Platform.runLater(() -> { if(this.root.getScene() != null) this.root.prefWidthProperty().bind(this.root.getScene().widthProperty());});
		Platform.runLater(() -> { this.border.prefWidthProperty().bind(this.root.widthProperty());});
				
		// bit of a hack because calculating of distances needs components to be added to a scene...
		AddButtonToTitledPane.registerAddImageCall((e) -> AddButtonToTitledPane.addImage(this.actionPane, this.addAction, ImageLoader.getImage(ImageLoader.ADD_SMALL), event -> this.addAction(), false));
		AddButtonToTitledPane.registerAddImageCall((e) -> AddButtonToTitledPane.addImage(this.checkerPane, this.addChecker, ImageLoader.getImage(ImageLoader.ADD_SMALL), event -> this.addChecker(), false));
	}
	
	private boolean hasUniqueName(String name) {
		if(name == null || name.length() == 0) {
			this.addMessageToPrivateLog(MessageType.ERROR, "Task name can not be empty.");
			return false;
		}
		boolean valid = false;
		try { 
			Integer.parseInt(name);
			valid = true;
		}
		catch(Exception e) {
			if(name.matches("^[A-Za-z].*"))
				valid = true;
		}
		if(valid == false) {
			this.addMessageToPrivateLog(MessageType.WARNING, "A task with name can be numeric or must start with [A-Za-z].");
			return false;
		}
		if(name.equals(this.initialName) || !XMLDataStore.hasRegistedData(name, WorkflowModuleController.class))
			return true;
		else {
			this.addMessageToPrivateLog(MessageType.ERROR, "A task with name '"+name+"' exists already.");
			return false;
		}
	}
	
	@Override
	protected void saveData() {
		this.setWorkflowModule.onSave(this);
		super.saveData();
		this.close();
	}
	
	private void addAction() {
		this.addAction(null);
	}

	/**
	 * adds an new action
	 */
	private void addAction(TaskAction savedData) {
		// create the window
		Stage stage = new ScreenCenteredStage();
		stage.setTitle("Add new task action");
		stage.setResizable(true);
		stage.initModality(Modality.APPLICATION_MODAL);
		
		TaskActionWindow action = TaskActionWindow.getNewTaskAction(this.setWorkflowModule, this.getStatusConsole());
		Scene scene = new Scene(action);
		try { scene.getStylesheets().add(CSSRessourceLoader.getCSS("control.css")); } catch(Exception e) {}
		stage.setScene(scene);
		stage.show();
		stage.hide();

		AddButtonToTitledPane.initAddButtonsAsGUIisLoaded();
		
		// load the data, if any is there
		if(savedData != null) {
			action.loadData(savedData);
		}
		stage.showAndWait();
		
		// collect checker data and add it to GUI
		TaskAction data = action.getStoredData();
		if(data != null) {
			this.addAction2GUI(savedData, data);
		}
	}
	
	private void addChecker() {
		this.addChecker(null);
	}

	/**
	 * adds a new checker
	 */
	private void addChecker(ErrorCheckerStore savedData) {
		// create the window
		Stage stage = new ScreenCenteredStage();
		stage.setTitle("Add new checker");
		stage.setResizable(true);
		stage.initModality(Modality.APPLICATION_MODAL);
		
		CheckerProperty checker = CheckerProperty.getNewChecker(this.setWorkflowModule, this.getStatusConsole());
		Scene scene = new Scene(checker);
		try { scene.getStylesheets().add(CSSRessourceLoader.getCSS("control.css")); } catch(Exception e) {}
		stage.setScene(scene);
		stage.show();
		stage.hide();
		AddButtonToTitledPane.initAddButtonsAsGUIisLoaded();
		
		// load the data, if any is there
		if(savedData != null) {
			checker.loadData(savedData);
		}
		stage.showAndWait();
		
		// collect checker data and add it to GUI
		ErrorCheckerStore data = checker.getStoredData();
		if(data != null) {
			this.addChecker2GUI(savedData, data);
		}
	}
	
	private void addData2GUI(XMLDataStore originalData, XMLDataStore newData, Class<?> c, VBox vbox) {
		// test if box is already there
		HBox hAlreadyAdded = null;
		Label label = new Label("");
		if(c.equals(TaskAction.class)) {
			hAlreadyAdded = this.getActionBox((TaskAction) originalData);
			label = new Label(((TaskAction) newData).getName() + ": " + ((TaskAction) newData).getTarget());
		}
		else if(c.equals(ErrorCheckerStore.class)) {
			hAlreadyAdded = this.getCheckerBox((ErrorCheckerStore) originalData);
			label = new Label(((ErrorCheckerStore) newData).getFullClassName());
		}
		if(hAlreadyAdded == null) {
			// create H-box and set the user data
			HBox h = new HBox();
			h.setAlignment(Pos.CENTER_LEFT);
			h.setSpacing(5);
			h.setUserData(newData);
			
			// create content and add it
			Button view = new Button();
			Button delete = new Button();
			view.setGraphic(ImageLoader.getImage(ImageLoader.CONFIG_SMALL));
			delete.setGraphic(ImageLoader.getImage(ImageLoader.DELETE_SMALL));
			h.getChildren().add(view);
			h.getChildren().add(delete);
			h.getChildren().add(label);
			
			// add event handler
			delete.onActionProperty().set(z -> vbox.getChildren().remove(h));
			if(c.equals(ErrorCheckerStore.class))
				view.onActionProperty().set(z -> this.addChecker((ErrorCheckerStore) h.getUserData()));
			else
				view.onActionProperty().set(z -> this.addAction((TaskAction) h.getUserData()));
			
			// add it to the screen
			vbox.getChildren().add(h);
		}
		else {
			// update user data and label
			hAlreadyAdded.setUserData(newData);
			hAlreadyAdded.getChildren().remove(2); // remove the label
			hAlreadyAdded.getChildren().add(label); // add label again
		}
	}
	
	private void addChecker2GUI(ErrorCheckerStore origninalData, ErrorCheckerStore e) {
		this.addData2GUI(origninalData, e, ErrorCheckerStore.class, this.checkerVBox);
		
		// expand pane if required
		if(this.checkerVBox.getChildrenUnmodifiable().size() > 0) {
			this.checkerPane.setExpanded(true);
		}
	}
	
	private void addAction2GUI(TaskAction origninalData, TaskAction a) {
		this.addData2GUI(origninalData, a, TaskAction.class, this.actionVBox);
		
		// expand pane if required
		if(this.actionVBox.getChildrenUnmodifiable().size() > 0) {
			this.actionPane.setExpanded(true);
		}
	}
	
	private HBox getHBoxGeneric(XMLDataStore d, Class<?> c, VBox vbox) {
		if(d == null)
			return null;
		
		for(Node n : vbox.getChildrenUnmodifiable()) {
			if(n instanceof HBox && n.getUserData() != null && c.isInstance(d)) {
				if(n.getUserData().equals(d)) {
					return (HBox) n;
				}
			}
		}
		return null;
	}
	
	private HBox getCheckerBox(ErrorCheckerStore e) {
		return this.getHBoxGeneric(e, ErrorCheckerStore.class, this.checkerVBox);
	}
	
	private HBox getActionBox(TaskAction a) {
		return this.getHBoxGeneric(a, TaskAction.class, this.actionVBox);
	}
	
	public boolean validate() {
		return super.validate();	
	}
	
	public void addValidateToControl(Control c, Validator<Control> v) {
		super.addValidateToControl(c, null, v);
	}
	
	public ArrayList<ErrorCheckerStore> getActiveCheckers() {
		ArrayList<ErrorCheckerStore> a = new ArrayList<>();
		
		for(Node n : this.checkerVBox.getChildrenUnmodifiable()) {
			if(n instanceof HBox && n.getUserData() instanceof ErrorCheckerStore) {
				a.add((ErrorCheckerStore) n.getUserData());
			}
		}
		return a;
	}
	
	public ArrayList<TaskAction> getActiveTaskActions() {
		ArrayList<TaskAction> a = new ArrayList<>();
		
		for(Node n : this.actionVBox.getChildrenUnmodifiable()) {
			if(n instanceof HBox && n.getUserData() instanceof TaskAction) {
				a.add((TaskAction) n.getUserData());
			}
		}
		return a;
	}

	public void setGridPanes(GridPane requiredGrid, GridPane optionalGrid) {
		this.requiredPane.setContent(requiredGrid);
		this.optionalPane.setContent(optionalGrid);
	}

	public void setWorkflowModule(WorkflowModuleController module) {
		this.setWorkflowModule = module;
		
		// load the data if some is already set
		WorkflowModuleData data = this.setWorkflowModule.getSavedData();
		this.name.setText(data.name);
		this.initialName = data.name;
		
		if(data.simMaxRunning != -1) 
			this.maxRunning.setText(Integer.toString(data.simMaxRunning));
		this.notify.selectToggle(this.notify.getToggles().get(data.notify.getIndex4ToogleGroup()));
		this.confirm.selectToggle(this.confirm.getToggles().get(data.confirm.getIndex4ToogleGroup()));
		this.checkpoint.selectToggle(this.checkpoint.getToggles().get(data.checkpoint.getIndex4ToogleGroup()));
		this.enforceStdin.setSelected(data.enforceStdin);
		
		// streams
		this.appendErr.setSelected(data.appendErr);
		this.appendOut.setSelected(data.appendOut);
		this.stderr.setText(data.stdErr);
		this.stdout.setText(data.stdOut);
		this.stdin.setText(data.stdIn);
		this.workingDir.setText(data.workingDir);
		
		// add validate to streams stuff
		this.stderr.textProperty().addListener(e -> this.validate());
		this.stdout.textProperty().addListener(e -> this.validate());
		this.stdin.textProperty().addListener(e -> this.validate());
		this.workingDir.textProperty().addListener(e -> this.validate());
		
		this.addValidateToControl(this.stderr, f -> this.stderr.getText() == null || this.stderr.getText().isEmpty() || (!this.stderr.getText().isEmpty() && this.isAbsoluteFile(this.stderr, "Standard error must be an absolute file path.")));
		this.addValidateToControl(this.stdout, f -> this.stdout.getText() == null || this.stdout.getText().isEmpty() || (!this.stdout.getText().isEmpty() && this.isAbsoluteFile(this.stdout, "Standard output must be an absolute file path.")));
		this.addValidateToControl(this.stdin, f -> this.stdin.getText() == null || this.stdin.getText().isEmpty() || (!this.stdin.getText().isEmpty() && this.isAbsoluteFile(this.stdin, "Standard input must be an absolute file path.")));
		this.addValidateToControl(this.workingDir, f -> this.workingDir.getText() == null || this.workingDir.getText().isEmpty() || (!this.workingDir.getText().isEmpty() && this.isAbsoluteFolder(this.workingDir, "Working directory must be an absolute folder path.")));
		
		// add suggest vars to streams
		ProcessBlock b = (this.setWorkflowModule.hasProcessBlockProperty() ? this.setWorkflowModule.getProcessBlockProperty() : null);
		@SuppressWarnings("unused") SuggestPopup p1 = new SuggestPopup(this.stderr, this.setWorkflowModule.getGrid(), b, this.setWorkflowModule.getKey(), true);
		@SuppressWarnings("unused") SuggestPopup p2 = new SuggestPopup(this.stdout, this.setWorkflowModule.getGrid(), b, this.setWorkflowModule.getKey(), true);
		@SuppressWarnings("unused") SuggestPopup p3 = new SuggestPopup(this.stdin, this.setWorkflowModule.getGrid(), b, this.setWorkflowModule.getKey(), true);
		@SuppressWarnings("unused") SuggestPopup p4 = new SuggestPopup(this.workingDir, this.setWorkflowModule.getGrid(), b, this.setWorkflowModule.getKey(), true);
		
		// load checkers
		for(ErrorCheckerStore e : data.CHECKERS) {
			this.addChecker2GUI(null, e);
		}
		
		// load actions 
		for(TaskAction a : data.ACTIONS) {
			this.addAction2GUI(null, a);
		}
	}

	@Override
	public String getSaveName() {
		return "task";
	}

	@Override
	public XMLDataStore getStoredData() {
		return this.setWorkflowModule;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		this.discardButton.setVisible(!this.readOnly);
		this.saveButton.setVisible(!this.readOnly);
		this.addAction.setVisible(!this.readOnly);
		this.addChecker.setVisible(!this.readOnly);
	}
}