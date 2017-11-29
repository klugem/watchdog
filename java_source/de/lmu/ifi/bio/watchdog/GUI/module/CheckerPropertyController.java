package de.lmu.ifi.bio.watchdog.GUI.module;

import java.io.File;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.ResourceBundle;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.GUI.helper.AddButtonToTitledPane;
import de.lmu.ifi.bio.watchdog.GUI.helper.SuggestPopup;
import de.lmu.ifi.bio.watchdog.GUI.layout.InsertableGridPane;
import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.ValidateViewController;
import de.lmu.ifi.bio.watchdog.helper.ErrorCheckerStore;
import de.lmu.ifi.bio.watchdog.helper.ErrorCheckerType;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import de.lmu.ifi.bio.watchdog.helper.returnType.BooleanReturnType;
import de.lmu.ifi.bio.watchdog.helper.returnType.DoubleReturnType;
import de.lmu.ifi.bio.watchdog.helper.returnType.IntegerReturnType;
import de.lmu.ifi.bio.watchdog.helper.returnType.ReturnType;
import de.lmu.ifi.bio.watchdog.helper.returnType.StringReturnType;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessBlock;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class CheckerPropertyController extends ValidateViewController {

	@FXML protected ToggleGroup type;
	@FXML protected Button fileButton;
	@FXML protected TextField fullClassName;
	@FXML protected TextField path2file;
	@FXML protected TitledPane paramPane;
	@FXML protected Button cancelButton;
	@FXML protected RadioButton radioErr;
	@FXML protected RadioButton radioSuc;
	@FXML private BorderPane border;
	@FXML private ScrollPane scroll;
	@FXML private VBox root;
		
	private Button addParam = new Button();
	private final InsertableGridPane PARAM_GRID = new InsertableGridPane();
	private final LinkedHashMap<Integer, Pair<ChoiceBox<ReturnType>, TextField>> PARAMS = new LinkedHashMap<>();
	private static int uniqParamID = 1;
	private static final LinkedHashMap<Integer, ReturnType> RETURN_TYPES = new LinkedHashMap<>();
	
	// some external variables used for data transfer
	private WorkflowModuleController setWorkflowModule;
	private ErrorCheckerStore errorCheckerStore;
	
	static {
		RETURN_TYPES.put(1, StringReturnType.TYPE);
		RETURN_TYPES.put(2, IntegerReturnType.TYPE);
		RETURN_TYPES.put(3, DoubleReturnType.TYPE);
		RETURN_TYPES.put(4, BooleanReturnType.TYPE);
	}
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// load stuff from parent class
		super.initialize(location, resources);
		
		this.paramPane.setExpanded(false);
		this.paramPane.setContent(this.PARAM_GRID);
		// bit of a hack because calculating of distances needs components to be added to a scene...
		AddButtonToTitledPane.registerAddImageCall((e) -> AddButtonToTitledPane.addImage(this.paramPane, this.addParam, ImageLoader.getImage(ImageLoader.ADD_SMALL), event -> this.addParam(), false));
		
		// add radio button values
		this.radioErr.setUserData(ErrorCheckerType.ERROR);
		this.radioSuc.setUserData(ErrorCheckerType.SUCCESS);
		
		this.cancelButton.setGraphic(ImageLoader.getImage(ImageLoader.DELETE_SMALL));
		this.cancelButton.onActionProperty().set(event -> { this.close(); event.consume();});
		this.fileButton.onActionProperty().set(event -> { this.selectClassFile(); event.consume(); });
		
		// add the validation stuff
		this.addValidateToControl(this.fullClassName, null, z -> this.type.getSelectedToggle() != null);
		this.addValidateToControl(this.fullClassName, "fullClassName", f -> !this.isEmpty((TextField) f, "Full class name can not be an empty value."));
		this.addValidateToControl(this.path2file, "path2file", f -> this.isAbsoluteFile((TextField) f, "A compiled class file must be selected."));
		
		this.type.selectedToggleProperty().addListener(e -> this.validate());
		this.path2file.textProperty().addListener(e -> this.validate());
		this.fullClassName.textProperty().addListener(e -> this.validate());
		this.validate();
		
		// expand correctly
		Platform.runLater(() -> { if(this.root.getScene() != null) this.border.prefHeightProperty().bind(this.root.getScene().heightProperty());});
		Platform.runLater(() -> { if(this.root.getScene() != null) this.root.prefWidthProperty().bind(this.root.getScene().widthProperty());});
		Platform.runLater(() -> { this.border.prefWidthProperty().bind(this.root.widthProperty());});
	}

	public void loadData(ErrorCheckerStore data) {
		if(data != null) {
			this.isDataLoaded = true;

			// set settings
			this.fullClassName.setText(data.getFullClassName());
			this.path2file.setText(data.getPathToClassFile());
			if(data.getType().equals(ErrorCheckerType.SUCCESS)) 
				this.radioSuc.setSelected(true);
			else 
				this.radioErr.setSelected(true);
			
			// load the parameters
			for(Pair<ReturnType, String> d : data.getArguments()) {
				this.addParam(d.getKey(), d.getValue());
			}
			
			// save the loaded data 
			this.errorCheckerStore = data;
			this.isDataLoaded = false;
		}
	}
	
	private void addParam(ReturnType selectedType, String setValue) {
		int newUniqueID = uniqParamID;
		int index = this.PARAMS.size();
		ChoiceBox<ReturnType> type = getChoiceBox();
		type.getProperties().put(InsertableGridPane.UNIQUE_ID, newUniqueID);
		TextField value = new TextField();
		ProcessBlock b = this.setWorkflowModule.hasProcessBlockProperty() ? this.setWorkflowModule.getProcessBlockProperty() : null;
		@SuppressWarnings("unused") SuggestPopup p1 = new SuggestPopup(value, this.setWorkflowModule.getGrid(), b, this.setWorkflowModule.getKey(), false);
		value.textProperty().addListener(t -> this.validate());
		type.valueProperty().addListener(t -> this.validate());
		this.addValidateToControl(value, null, v -> this.validateAndColorValueType(0, type, (TextField) v));
		Button delete = new Button();
		delete.setGraphic(ImageLoader.getImage(ImageLoader.DELETE_SMALL));
		delete.setOnAction(event -> {this.deleteParam(newUniqueID); event.consume();});
		this.PARAM_GRID.insertRow(index, type, value, delete);
		this.PARAMS.put(newUniqueID, Pair.of(type, value));
		uniqParamID++;
		
		// set the values if required
		if(setValue != null && !setValue.equals("")) {
			this.isDataLoaded = true;
			value.setText(setValue);
			type.getSelectionModel().select(selectedType);
			this.isDataLoaded = false;
		}
		
		// ensure than pane is expanded
		this.paramPane.setExpanded(true);
				
		return;
	}
	
	private void addParam() {
		this.addParam(null, null);
	}


	private boolean validateAndColorValueType(int index, ChoiceBox<ReturnType> type, TextField v) {
		ReturnType t = RETURN_TYPES.get(index);
		return validateValueType(t, type, v);
	}

	private static boolean validateValueType(ReturnType r, ChoiceBox<ReturnType> type, TextField t) {
		String text = t.getText();
		// try to find best hit
		if(r == null && type.getSelectionModel().getSelectedItem() == null) {
			if(BooleanReturnType.TYPE.checkType(text)) {
				type.getSelectionModel().select(BooleanReturnType.TYPE);
				return true;
			}
			else if(IntegerReturnType.TYPE.checkType(text)) {
				type.getSelectionModel().select(IntegerReturnType.TYPE);
				return true;
			}
			else if(DoubleReturnType.TYPE.checkType(text)) {
				type.getSelectionModel().select(DoubleReturnType.TYPE);
				return true;
			}
			else if(StringReturnType.TYPE.checkType(text)) {
				type.getSelectionModel().select(StringReturnType.TYPE);
				return true;
			}
		}
		// test if types match
		else {
			if(r == null)
				r = type.getSelectionModel().getSelectedItem();
			return r.checkType(text);
		}
		return false;
	}

	private void deleteParam(int uniqparamid) {
		int index = this.PARAM_GRID.getRowNumber(Integer.toString(uniqparamid));
		this.PARAM_GRID.deleteRow(index);
		this.PARAMS.remove(uniqparamid);
	}

	public void setWorkflowModule(WorkflowModuleController module) {
		this.setWorkflowModule = module;
	}
	
	@Override
	protected void saveData() {
		LinkedHashSet<Pair<ReturnType, String>> args = new LinkedHashSet<>();
		// collect arguments
		for(Pair<ChoiceBox<ReturnType>, TextField> d : this.PARAMS.values()) {
			ChoiceBox<ReturnType> c = d.getKey();
			TextField t = d.getValue();
			args.add(Pair.of(c.getSelectionModel().getSelectedItem(), t.getText()));
		}
		// create the object
		this.errorCheckerStore = new ErrorCheckerStore(this.fullClassName.getText(), this.path2file.getText(), (ErrorCheckerType) this.type.getSelectedToggle().getUserData(), args);
		
		super.saveData();
	}
	
	private static ChoiceBox<ReturnType> getChoiceBox() {
		ChoiceBox<ReturnType> type = new ChoiceBox<>();
		type.getItems().addAll(RETURN_TYPES.values());
		return type;
	}
	
	private void selectClassFile() {
		FileChooser c = new FileChooser();
		c.getExtensionFilters().add(new ExtensionFilter("compiled java class (*.class)", "*.class"));
		File f = c.showOpenDialog(this.saveButton.getScene().getWindow());
		if(f != null)
			this.path2file.setText(f.getAbsolutePath());
	}
	
	@Override
	public String getSaveName() {
		return this.fullClassName.getText();
	}

	@Override
	public XMLDataStore getStoredData() {
		return this.errorCheckerStore;
	}
}