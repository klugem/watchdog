package de.lmu.ifi.bio.watchdog.GUI.layout;

import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.helper.TextFilter;
import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.ValidateViewController;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;

/**
 * Controller for module that is placed in the workflow
 * */
public class DependencyPropertyController extends ValidateViewController implements Initializable {

	private static final String GLOBAL = "global";
	private static final String SEPARATE = "separate";
	
	@FXML private ToggleGroup type;
	@FXML private RadioButton globalType;
	@FXML private RadioButton separateType;
	@FXML private TextField prefixLength;
	@FXML private TextField separator;
	@FXML private Button deleteButton;
	
	private Dependency dependency;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.saveButton.setGraphic(ImageLoader.getImage(ImageLoader.SAVE_SMALL));
		this.deleteButton.setGraphic(ImageLoader.getImage(ImageLoader.DELETE_SMALL));
		this.saveButton.onActionProperty().set(e -> this.onSave(this.getCondition())); // override default event handler
		this.deleteButton.onMouseClickedProperty().set(e -> this.deleteAsk(e));
		this.type.selectedToggleProperty().addListener(x -> this.changeType());
		
		// value restrictions
		this.prefixLength.setTextFormatter(TextFilter.getPositiveIntFormater());
		
		// add validator
		this.addValidateToControl(this.separator, "separator", (c) -> !this.isEmpty((TextField) c, "Prefix separator can not be empty."), SEPARATE);
		this.addValidateToControl(this.prefixLength, "prefixLength", (c) -> this.isInteger((TextField) c, "Prefix length must be an integer value."), SEPARATE);
		
		// add events
		this.separator.textProperty().addListener(x -> this.validate());
		this.prefixLength.textProperty().addListener(x -> this.validate());
	}
	
	private void deleteAsk(MouseEvent e) {
		this.dependency.deleteAsk(e); 
		if(this.dependency.isDeleted())
			this.close();
	}

	@Override
	protected void saveData() {
		// do not call super type 
		if(this.isGlobalType())
			this.dependency.setSeparateVariables(null, null);
		else
			this.dependency.setSeparateVariables(this.separator.getText(), Integer.parseInt(this.prefixLength.getText()));
	}
	
	private void changeType() {
		boolean global = this.isGlobalType();
		this.separator.setDisable(global);
		this.prefixLength.setDisable(global);
		
		// validate new condition
		this.validate();
	}
	
	private boolean isGlobalType() {
		return this.type.getSelectedToggle().equals(this.globalType);
	}
	
	private String getCondition() {
		if(this.isGlobalType())
			return GLOBAL;
		else
			return SEPARATE;
	}
	
	@Override
	public boolean validate() {
		return this.validate(this.getCondition());
	}
	
	public void setDependency(Dependency d) {
		this.dependency = d;
		
		// update GUI
		if(this.dependency.isSeparateDependency()) {
			this.separateType.setSelected(true);
			this.separator.setText(d.getSeparator());
			this.prefixLength.setText(Integer.toString(d.getPrefixLength()));
		}
		else
			this.globalType.setSelected(true);
		this.changeType();
	}

	/** not required in this case */
	@Override
	public String getSaveName() {return null; }
	@Override
	public XMLDataStore getStoredData() { return null; }
}
