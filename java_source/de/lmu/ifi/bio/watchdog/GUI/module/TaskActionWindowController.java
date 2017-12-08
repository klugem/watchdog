package de.lmu.ifi.bio.watchdog.GUI.module;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.helper.SuggestPopup;
import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.ValidateViewController;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessBlock;
import de.lmu.ifi.bio.watchdog.task.TaskAction;
import de.lmu.ifi.bio.watchdog.task.TaskActionTime;
import de.lmu.ifi.bio.watchdog.task.actions.CopyTaskAction;
import de.lmu.ifi.bio.watchdog.task.actions.CreateTaskAction;
import de.lmu.ifi.bio.watchdog.task.actions.DeleteTaskAction;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class TaskActionWindowController extends ValidateViewController {

	@FXML protected ComboBox<Class<?>> type;
	@FXML private BorderPane border;
	@FXML private ScrollPane scroll;
	@FXML private VBox root;
	
	@FXML private ChoiceBox<TaskAction> action;
	@FXML private ChoiceBox<TaskActionTime> time;
	@FXML private CheckBox uncouple;
	@FXML private Button cancelButton;
	
	// grids to hide
	@FXML private GridPane createFile_grid;
	@FXML private GridPane createFolder_grid;
	@FXML private GridPane copyFile_grid;
	@FXML private GridPane copyFolder_grid;
	@FXML private GridPane deleteFile_grid;
	@FXML private GridPane deleteFolder_grid;
	
	// create file / folder
	@FXML private CheckBox createFile_override;
	@FXML private CheckBox createFile_parent;
	@FXML private TextField createFile_file;
	@FXML private CheckBox createFolder_override;
	@FXML private CheckBox createFolder_parent;
	@FXML private TextField createFolder_folder;
	
	// copy file / folder
	@FXML private TextField copyFile_source;
	@FXML private TextField copyFile_destination;
	@FXML private CheckBox copyFile_override;
	@FXML private CheckBox copyFile_parent;
	@FXML private CheckBox copyFile_delete;
	@FXML private TextField copyFolder_source;
	@FXML private TextField copyFolder_destination;
	@FXML private CheckBox copyFolder_override;
	@FXML private CheckBox copyFolder_parent;
	@FXML private CheckBox copyFolder_delete;
	
	// delete file / folder
	@FXML private TextField delete_file;
	@FXML private TextField delete_folder;
	
	// some external variables used for data transfer
	private WorkflowModuleController setWorkflowModule;
	private TaskAction taskActionStore;
	private GridPane currentGridPane = null;
	
	private final static CreateTaskAction SWITCH_CREATE_FILE = new CreateTaskAction(null, false, false, true, null, false);
	private final static CreateTaskAction SWITCH_CREATE_FOLDER = new CreateTaskAction(null, false, false, false, null, false);
	private final static CopyTaskAction SWITCH_COPY_FILE = new CopyTaskAction(null, null, false, false, false, true, null, false);
	private final static CopyTaskAction SWITCH_COPY_FOLDER = new CopyTaskAction(null, null, false, false, false, false, null, false);
	private final static DeleteTaskAction SWITCH_DELETE_FILE = new DeleteTaskAction(null, true, null, false);
	private final static DeleteTaskAction SWITCH_DELETE_FOLDER = new DeleteTaskAction(null, false, null, false);
	
	private final LinkedHashMap<TaskAction, GridPane> HIDE = new LinkedHashMap<>();
	private final HashMap<GridPane, TaskAction> PANE2ACTION = new HashMap<>();
	
	@SuppressWarnings("unchecked")
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// load stuff from parent class
		super.initialize(location, resources);
		
		// init drop down stuff
		ObservableList<TaskActionTime> t = FXCollections.observableArrayList();
		t.addAll(TaskActionTime.values());
		this.time.setItems(t);
		
		ObservableList<TaskAction> a= FXCollections.observableArrayList();
		this.action.setItems(a);
		
		// add grids to hide
		this.HIDE.put(SWITCH_CREATE_FILE, createFile_grid);
		this.HIDE.put(SWITCH_CREATE_FOLDER, createFolder_grid);
		this.HIDE.put(SWITCH_COPY_FILE, copyFile_grid);
		this.HIDE.put(SWITCH_COPY_FOLDER, copyFolder_grid);
		this.HIDE.put(SWITCH_DELETE_FILE, deleteFile_grid);
		this.HIDE.put(SWITCH_DELETE_FOLDER, deleteFolder_grid);
		
		// add indices and drop down buttons
		for(TaskAction c : this.HIDE.keySet()) {
			a.add(c);
			this.PANE2ACTION.put(this.HIDE.get(c), c);
		}

		// add event handler to action
		this.action.getSelectionModel().selectedItemProperty().addListener((x, y, z) -> this.onActionChange());
		this.time.getSelectionModel().selectedItemProperty().addListener((x, y, z) -> this.validate());
		Platform.runLater(() -> this.onActionChange()); // trigger it once to hide all the stuff
		
		// customize cancel button
		this.cancelButton.setGraphic(ImageLoader.getImage(ImageLoader.DELETE_SMALL));
		this.cancelButton.onActionProperty().set(event -> { this.close(); event.consume();});
		
		// add validation stuff
		this.addValidateToControl(this.action, "action", c -> ((ChoiceBox<TaskAction>) c).getSelectionModel().getSelectedItem() != null);
		this.addValidateToControl(this.time, "time", c -> ((ChoiceBox<TaskAction>) c).getSelectionModel().getSelectedItem() != null);
		this.addValidateToControl(this.createFile_file, "createFile_file", f -> !this.isEmpty((TextField) f, "An absolute file path or an URI must be given."), SWITCH_CREATE_FILE.toString());
		this.addValidateToControl(this.createFolder_folder, "createFolder_folder", f -> !this.isEmpty((TextField) f, "An absolute folder path or an URI must be given."), SWITCH_CREATE_FOLDER.toString());
		this.addValidateToControl(this.copyFile_source, "copyFile_source", f -> !this.isEmpty((TextField) f, "An absolute path or an URI to a file to copy must be given."), SWITCH_COPY_FILE.toString());
		this.addValidateToControl(this.copyFile_destination, "copyFile_destination", f -> !this.isEmpty((TextField) f, "An absolute path or an URI to the destination file must be given."), SWITCH_COPY_FILE.toString());
		this.addValidateToControl(this.copyFolder_source, "copyFolder_source", f -> !this.isEmpty((TextField) f, "An absolute path or an URI to a folder to copy must be given."), SWITCH_COPY_FOLDER.toString());
		this.addValidateToControl(this.copyFolder_destination, "copyFolder_destination", f -> !this.isEmpty((TextField) f, "An absolute path or an URI to the destination folder must be given."), SWITCH_COPY_FOLDER.toString());
		this.addValidateToControl(this.delete_file, "delete_file", f -> !this.isEmpty((TextField) f, "An absolute file path or an URI must be given."), SWITCH_DELETE_FILE.toString());
		this.addValidateToControl(this.delete_folder, "delete_folder", f -> !this.isEmpty((TextField) f, "An absolute folder or an URI path must be given."), SWITCH_DELETE_FOLDER.toString());
		
		// add handlers that should call validate on change
		this.createFile_file.textProperty().addListener(x -> this.validate());
		this.createFolder_folder.textProperty().addListener(x -> this.validate());
		this.copyFile_source.textProperty().addListener(x -> this.validate());
		this.copyFile_destination.textProperty().addListener(x -> this.validate());
		this.copyFolder_source.textProperty().addListener(x -> this.validate());
		this.copyFolder_destination.textProperty().addListener(x -> this.validate());
		this.delete_file.textProperty().addListener(x -> this.validate());
		this.delete_folder.textProperty().addListener(x -> this.validate());
		
		this.validate();
		// expand correctly
		Platform.runLater(() -> { if(this.root.getScene() != null) this.border.prefHeightProperty().bind(this.root.getScene().heightProperty());});
		Platform.runLater(() -> { if(this.root.getScene() != null) this.root.prefWidthProperty().bind(this.root.getScene().widthProperty());});
		Platform.runLater(() -> { this.border.prefWidthProperty().bind(this.root.widthProperty());});
	}

	public void loadData(TaskAction data) {
		if(data != null) {
			this.isDataLoaded = true;

			// set settings
			this.uncouple.setSelected(data.isUncoupledFromExecutor());
			this.time.getSelectionModel().select(data.getActionTime());
			if(data instanceof CreateTaskAction) {
				CreateTaskAction c = (CreateTaskAction) data;
				if(c.isFileType()) {
					this.createFile_file.setText(c.getPath());
					this.createFile_override.setSelected(c.isOverride());
					this.createFile_parent.setSelected(c.isCreateParent());
					this.setActivePane(this.createFile_grid);
				}
				else {
					this.createFolder_folder.setText(c.getPath());
					this.createFolder_override.setSelected(c.isOverride());
					this.createFolder_parent.setSelected(c.isCreateParent());
					this.setActivePane(this.createFolder_grid);
				}
			}
			else if(data instanceof CopyTaskAction) {
				CopyTaskAction cc = (CopyTaskAction) data;
				if(cc.isFileType()) {
					this.copyFile_source.setText(cc.getSrc());
					this.copyFile_destination.setText(cc.getDest());
					this.copyFile_override.setSelected(cc.isOverride());
					this.copyFile_parent.setSelected(cc.isCreateParent());
					this.copyFile_delete.setSelected(cc.isCreateParent());
					this.setActivePane(this.copyFile_grid);
				}
				else {
					this.copyFolder_source.setText(cc.getSrc());
					this.copyFolder_destination.setText(cc.getDest());
					this.copyFolder_override.setSelected(cc.isOverride());
					this.copyFolder_parent.setSelected(cc.isCreateParent());
					this.copyFolder_delete.setSelected(cc.isCreateParent());
					this.setActivePane(this.copyFolder_grid);
				}
			}
			else if(data instanceof DeleteTaskAction) {
				DeleteTaskAction d= (DeleteTaskAction) data;
				if(d.isFileType()) {
					this.delete_file.setText(d.getPath());
					this.setActivePane(this.deleteFile_grid);
				}
				else {
					this.delete_folder.setText(d.getPath());
					this.setActivePane(this.deleteFolder_grid);
				}
			}
			
			// save the loaded data 
			this.taskActionStore = data;
			this.isDataLoaded = false;
		}
	}
	
	private void setActivePane(GridPane active) {
		active.setVisible(true);
		TaskAction t = this.PANE2ACTION.get(active);
		if(t != null) {
			this.action.getSelectionModel().select(t);
			this.currentGridPane = active;
			this.validate();
		}
	}

	private GridPane getActiveGrid() {
		TaskAction t = this.getActiveAction();
		return this.HIDE.get(t);
	}
	
	private TaskAction getActiveAction() {
		return this.action.getSelectionModel().getSelectedItem();
	}
	
	protected boolean validate(String condition) {
		TaskAction t = this.getActiveAction();
		return super.validate(t != null ? t.toString() : null);
	}
	
	private void onActionChange() {
		GridPane active = this.getActiveGrid();
		
		if(this.currentGridPane == null || !this.currentGridPane.equals(active)) {
			// hide all
			for(GridPane g : this.HIDE.values())
				g.setVisible(false);
			
			// activate active one ;)
			if(active != null) {
				this.setActivePane(active);
				this.validate();
			}
		}
	}

	public void setWorkflowModule(WorkflowModuleController module) {
		this.setWorkflowModule = module;
		
		// suggest stuff
		ProcessBlock b = this.setWorkflowModule.hasProcessBlockProperty() ? this.setWorkflowModule.getProcessBlockProperty() : null;
		@SuppressWarnings("unused") SuggestPopup p1 = new SuggestPopup(this.createFile_file, this.setWorkflowModule.getGrid(), b, this.setWorkflowModule.getKey(), true);
		@SuppressWarnings("unused") SuggestPopup p2 = new SuggestPopup(this.createFolder_folder, this.setWorkflowModule.getGrid(), b, this.setWorkflowModule.getKey(), true);
		@SuppressWarnings("unused") SuggestPopup p3 = new SuggestPopup(this.copyFile_source, this.setWorkflowModule.getGrid(), b, this.setWorkflowModule.getKey(), true);
		@SuppressWarnings("unused") SuggestPopup p4 = new SuggestPopup(this.copyFile_destination, this.setWorkflowModule.getGrid(), b, this.setWorkflowModule.getKey(), true);
		@SuppressWarnings("unused") SuggestPopup p5 = new SuggestPopup(this.copyFolder_source, this.setWorkflowModule.getGrid(), b, this.setWorkflowModule.getKey(), true);
		@SuppressWarnings("unused") SuggestPopup p6 = new SuggestPopup(this.copyFolder_destination, this.setWorkflowModule.getGrid(), b, this.setWorkflowModule.getKey(), true);
		@SuppressWarnings("unused") SuggestPopup p7 = new SuggestPopup(this.delete_file, this.setWorkflowModule.getGrid(), b, this.setWorkflowModule.getKey(), true);
		@SuppressWarnings("unused") SuggestPopup p8 = new SuggestPopup(this.delete_folder, this.setWorkflowModule.getGrid(), b, this.setWorkflowModule.getKey(), true);
	}
	
	@Override
	protected void saveData() {
		TaskAction select = this.PANE2ACTION.get(this.getActiveGrid());
		if(select != null) {
			if(select instanceof CreateTaskAction) {
				CreateTaskAction c = (CreateTaskAction) select;
				if(c.isFileType())
					this.taskActionStore = new CreateTaskAction(this.createFile_file.getText(), this.createFile_override.isSelected(), this.createFile_parent.isSelected(), true, this.time.getSelectionModel().getSelectedItem(), this.uncouple.isSelected());
				else
					this.taskActionStore = new CreateTaskAction(this.createFolder_folder.getText(), this.createFolder_override.isSelected(), this.createFolder_parent.isSelected(), false, this.time.getSelectionModel().getSelectedItem(), this.uncouple.isSelected());
			}
			else if(select instanceof CopyTaskAction) {
				CopyTaskAction cc = (CopyTaskAction) select;
				if(cc.isFileType())
					this.taskActionStore = new CopyTaskAction(this.copyFile_source.getText(), this.copyFile_destination.getText(), this.copyFile_override.isSelected(), this.copyFile_parent.isSelected(), this.copyFile_delete.isSelected(), true, this.time.getSelectionModel().getSelectedItem(), this.uncouple.isSelected());						
				else
					this.taskActionStore = new CopyTaskAction(this.copyFolder_source.getText(), this.copyFolder_destination.getText(), this.copyFolder_override.isSelected(), this.copyFolder_parent.isSelected(), this.copyFolder_delete.isSelected(), false, this.time.getSelectionModel().getSelectedItem(), this.uncouple.isSelected());						
			}
			else if(select instanceof DeleteTaskAction) {
				DeleteTaskAction d= (DeleteTaskAction) select;
				if(d.isFileType())
					this.taskActionStore = new DeleteTaskAction(this.delete_file.getText(), true, this.time.getSelectionModel().getSelectedItem(), this.uncouple.isSelected());
				else 
					this.taskActionStore = new DeleteTaskAction(this.delete_folder.getText(), false, this.time.getSelectionModel().getSelectedItem(), this.uncouple.isSelected());
			}
			super.saveData();
		}
	}
	
	@Override
	public String getSaveName() {
		return this.taskActionStore.getName();
	}

	@Override
	public XMLDataStore getStoredData() {
		return this.taskActionStore;
	}
}