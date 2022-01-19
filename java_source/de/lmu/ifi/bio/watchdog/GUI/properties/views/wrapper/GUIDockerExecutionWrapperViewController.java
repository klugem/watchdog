package de.lmu.ifi.bio.watchdog.GUI.properties.views.wrapper;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;

import de.lmu.ifi.bio.watchdog.GUI.datastructure.Blacklist;
import de.lmu.ifi.bio.watchdog.GUI.datastructure.DeleteButton;
import de.lmu.ifi.bio.watchdog.GUI.datastructure.MountPath;
import de.lmu.ifi.bio.watchdog.GUI.helper.Inform;
import de.lmu.ifi.bio.watchdog.GUI.helper.InputRequest;
import de.lmu.ifi.bio.watchdog.GUI.helper.SuggestPopup;
import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginPropertyViewController;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PluginViewController;
import de.lmu.ifi.bio.watchdog.executionWrapper.container.DockerExecutionWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class GUIDockerExecutionWrapperViewController extends PluginViewController<DockerExecutionWrapper> {
	
	@FXML private TextField path2docker;
	@FXML private TextField image;
	@FXML private TextField execKeyword;
	@FXML private TextField addCallParams;
	@FXML private CheckBox disableAutodetectMount;
	@FXML private CheckBox loadModuleSpecificImage;
	@FXML private CheckBox disableTerminateWrapper;
	
	// for mount points
	@FXML private TableView<MountPath> mounts;
	@FXML private TableView<Blacklist> blacklist;
	@FXML private TextField newMount;
	@FXML private TextField newBlacklist;
	@FXML private Button addMount;
	@FXML private Button addBlacklist;
	
	private  Predicate<TextField> absolutePathChecker;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);
		
		// create columns
		TableColumn<Blacklist, String> pattern = new TableColumn<>("pattern");
		TableColumn<Blacklist, Button> delPattern = new TableColumn<>("");
		TableColumn<MountPath, String> host = new TableColumn<>("host");
		TableColumn<MountPath, String> container = new TableColumn<>("container");
		TableColumn<MountPath, Button> delMount = new TableColumn<>("");
		
		// set getter methods
		pattern.setCellValueFactory(new PropertyValueFactory<>("pattern"));
		delPattern.setCellValueFactory(new PropertyValueFactory<>("button"));
		host.setCellValueFactory(new PropertyValueFactory<>("host"));
		container.setCellValueFactory(new PropertyValueFactory<>("container"));
		delMount.setCellValueFactory(new PropertyValueFactory<>("button"));
		
		// activate default value
		this.loadModuleSpecificImage.setSelected(true);
		this.disableTerminateWrapper.setSelected(false);
		
		// set column size
		this.mounts.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		this.blacklist.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		host.setMaxWidth(1f * Integer.MAX_VALUE * 57);
		container.setMaxWidth(1f * Integer.MAX_VALUE * 30);
		delMount.setMaxWidth(1f * Integer.MAX_VALUE * 13);
		pattern.setMaxWidth(1f * Integer.MAX_VALUE * 87);
		delPattern.setMaxWidth(1f * Integer.MAX_VALUE * 13);
	
		this.mounts.getColumns().add(host);
		this.mounts.getColumns().add(container);
		this.mounts.getColumns().add(delMount);
		this.blacklist.getColumns().add(pattern);
		this.blacklist.getColumns().add(delPattern);
		
		// add button events
		this.addMount.setOnMouseClicked(x -> this.addMountFromGUI());
		this.addBlacklist.setOnMouseClicked(x -> this.addBlackListFromGUI());
	}
	
	@Override
	public void addPropertyViewControllerToValidate(PluginPropertyViewController<DockerExecutionWrapper> propertyViewController, String condition) {
		this.execKeyword.setText(DockerExecutionWrapper.DEFAULT_EXEC_KEYWORD);
		
		// add checker
		propertyViewController.addValidateToControl(this.path2docker, "virtualizer binary path", f -> propertyViewController.isAbsoluteFile((TextField) f, "An absolute path to a virtualizer binary must be given."), condition);
		propertyViewController.addValidateToControl(this.image, "image name", f -> !propertyViewController.isEmpty((TextField) f, "Name of the image can not be empty."), condition);
		propertyViewController.addValidateToControl(this.execKeyword, "execute keyword", f -> !propertyViewController.isEmpty((TextField) f, "Keyword to start container command can not be empty."), condition);

		// add event handler for GUI validation
		this.path2docker.textProperty().addListener(event -> propertyViewController.validate());
		this.image.textProperty().addListener(event -> propertyViewController.validate());
		this.execKeyword.textProperty().addListener(event -> propertyViewController.validate());
		
		// add event handler on mount stuff
		this.absolutePathChecker = (x -> propertyViewController.isAbsoluteFolder(x, "An absolute path to a folder must be given."));
		this.newMount.textProperty().addListener(x -> this.validateInput(this.newMount, this.addMount, this.absolutePathChecker));
		this.newBlacklist.textProperty().addListener(x -> this.validateInput(this.newBlacklist, this.addBlacklist, (y -> !propertyViewController.isEmpty((TextField) y, "Blacklist pattern or path can not be empty."))));
		
		// call change handler
		this.newMount.setText("");
		this.newBlacklist.setText("");
		
		// add suggest constants support
		@SuppressWarnings("unused") SuggestPopup p1 = new SuggestPopup(this.path2docker);
		@SuppressWarnings("unused") SuggestPopup p2 = new SuggestPopup(this.image);
		@SuppressWarnings("unused") SuggestPopup p3 = new SuggestPopup(this.execKeyword);
		@SuppressWarnings("unused") SuggestPopup p4 = new SuggestPopup(this.addCallParams);
		@SuppressWarnings("unused") SuggestPopup p5 = new SuggestPopup(this.newMount);
		@SuppressWarnings("unused") SuggestPopup p6 = new SuggestPopup(this.newBlacklist);				
	}
	
	private void validateInput(TextField f, Button b, Predicate<TextField> checker) {
		boolean ret = checker.test(f);
		if(b != null)
			b.setDisable(!ret);
	}
	
	private void validateInput(TextField f, InputRequest<?> i, Predicate<TextField> checker) {
		boolean ret = checker.test(f);
		if(i != null)
			i.setDisable(!ret);
	}

	private void addMountFromGUI() {
		String host = this.newMount.getText();
		String container = null;
		if(!host.isEmpty()) {
			Optional<ButtonType> confirm = Inform.confirm("Do you want to mount the path under a different name within the container?");
			if(confirm.get() == ButtonType.OK) {
				TextField t = new TextField();
				InputRequest<TextField> input = new InputRequest<>("Path", "Container mount path", t);
				t.textProperty().addListener(x -> this.validateInput(t, input, this.absolutePathChecker));
				t.setText(host);
				Optional<String> result = input.showAndWait();
				if(result.isPresent())
					container = input.getInput();
			}
			
			this.addMount(host, container);
			this.newMount.setText("");
		}
	}
	
	private void addMount(String host, String container) {
		Button delete = new Button("", ImageLoader.getImage(ImageLoader.DELETE_SMALL));
		MountPath m = new MountPath(host, container, delete);
		delete.setOnMouseClicked(x -> this.deleteFromTable(this.mounts, m));
		this.mounts.getItems().add(m);
	}
	
	private void addBlackListFromGUI() {
		String bl = this.newBlacklist.getText();
		if(!bl.isEmpty()) {
			this.addBlackList(bl);			
			this.newBlacklist.setText("");
		}
	}
	
	private void addBlackList(String bl) {
		Button delete = new Button("", ImageLoader.getImage(ImageLoader.DELETE_SMALL));
		Blacklist b = new Blacklist(bl, delete);
		delete.setOnMouseClicked(x -> this.deleteFromTable(this.blacklist, b));
		this.blacklist.getItems().add(b);
	}
	
	private void deleteFromTable(TableView<?> t, DeleteButton b) {
		Optional<ButtonType> confirm = Inform.confirm("Do you want to remove '"+b.toString()+"'?");
		if(confirm.get() == ButtonType.OK) {
			t.getItems().remove(b);
		}
	}

	@Override
	public void setHandlerForGUIColoring() {}

	@Override
	public DockerExecutionWrapper getXMLPluginObject(Object[] data) {
		// cast the data
		String name = (String) data[0];
		HashMap<String, String> mounts = this.getMounts();
		ArrayList<String> blacklist = this.getBlacklist();
		return new DockerExecutionWrapper(name, null, this.path2docker.getText(), this.image.getText(), this.execKeyword.getText(), this.addCallParams.getText(), this.disableAutodetectMount.isSelected(), mounts, blacklist, new ArrayList<String>(), this.loadModuleSpecificImage.isSelected(), this.disableTerminateWrapper.isSelected());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void loadData(Object[] data) {
		this.path2docker.setText((String) data[0]);
		this.image.setText((String) data[1]);
		this.execKeyword.setText((String) data[2]);
		this.addCallParams.setText((String) data[3]);
		this.disableAutodetectMount.setSelected((Boolean) data[4]);
	
		HashMap<String, String> mounts = ((HashMap<String, String>) data[5]);
		ArrayList<String> blacklist = ((ArrayList<String>) data[6]);
		this.loadMounts(mounts);
		this.loadBlacklist(blacklist);
		
		this.loadModuleSpecificImage.setSelected((Boolean) data[7]);
		this.disableTerminateWrapper.setSelected((Boolean) data[8]);
	}
	
	private HashMap<String, String> getMounts() {
		HashMap<String, String> res = new HashMap<>();
		for(MountPath m : this.mounts.getItems()) {
			res.put(m.getHost(), m.getContainer());
		}
		return res;
	}

	private ArrayList<String> getBlacklist() {
		ArrayList<String> res = new ArrayList<>();
		for(Blacklist bl : this.blacklist.getItems()) {
			res.add(bl.getPattern());
		}
		return res;
	}
	
	private void loadBlacklist(ArrayList<String> blacklist) {
		for(String bl : blacklist)
			this.addBlackList(bl);
	}

	private void loadMounts(HashMap<String, String> mounts) {
		for(String host : mounts.keySet())
			this.addMount(host, mounts.get(host));
	}
}
