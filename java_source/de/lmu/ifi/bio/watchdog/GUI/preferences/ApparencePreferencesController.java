package de.lmu.ifi.bio.watchdog.GUI.preferences;

import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.WorkflowDesignerRunner;
import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.MessageType;
import de.lmu.ifi.bio.watchdog.GUI.css.GUIFormat;
import de.lmu.ifi.bio.watchdog.GUI.helper.PreferencesStore;
import de.lmu.ifi.bio.watchdog.GUI.helper.TextFilter;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Screen;

public class ApparencePreferencesController extends AbstractPreferencesController {

	@FXML private CheckBox fullScreen;
	@FXML private CheckBox gridDefault;
	@FXML private TextField width;
	@FXML private TextField height;
		
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// add event handlers
		this.width.textProperty().addListener(e -> this.validate());
		this.height.textProperty().addListener(e -> this.validate());
		
		// value restrictions
		this.height.setTextFormatter(TextFilter.getPositiveIntFormater());
		this.width.setTextFormatter(TextFilter.getPositiveIntFormater());
		
		// add validate stuff
		Rectangle2D screen = Screen.getPrimary().getBounds();
		this.addValidateToControl(this.width, "width", f -> this.isValidValue((TextField) f, WorkflowDesignerRunner.MIN_WIDTH, (int) screen.getWidth()));
		this.addValidateToControl(this.height, "height", f -> this.isValidValue((TextField) f, WorkflowDesignerRunner.MIN_HEIGHT, (int) screen.getHeight()));
		this.addValidateToControl(this.fullScreen, "fullScreen", null);
		this.addValidateToControl(this.gridDefault, "gridDefault", null);
		
		this.fullScreen.selectedProperty().addListener(x -> this.validate());
		this.gridDefault.selectedProperty().addListener(x -> this.validate());
		
		// get initial coloring
		super.initialize(location, resources);
	}
	
	private boolean isValidValue(TextField f,  int minValue, int maxValue) {
		boolean ret = true;
		if(f == null || f.getText() == null)
			ret = false;
		int v = -1;
		try { v = Integer.parseInt(f.getText()); } catch(Exception e) {}
		if(v < minValue || v > maxValue) {
			this.addMessageToPrivateLog(MessageType.ERROR, "Value '"+v+"' must be between '"+minValue+"' and '"+maxValue+"'.");
			ret = false;
		}
		GUIFormat.colorTextField(f, ret);
		return ret;
	}
	
	@Override
	public void onLoad() {
		this.height.setText(Integer.toString(PreferencesStore.getHeight()));
		this.width.setText(Integer.toString(PreferencesStore.getWidth()));
		this.fullScreen.setSelected(PreferencesStore.isFullScreenMode());
		this.gridDefault.setSelected(PreferencesStore.isGridDisplayedByDefault());
		super.onLoad();
	}

	@Override
	public void onSave() {
		PreferencesStore.setDisplayGridByDefault(this.gridDefault.isSelected());
		PreferencesStore.setFullScreenMode(this.fullScreen.isSelected());
		PreferencesStore.setWidth(Integer.parseInt(this.width.getText()));
		PreferencesStore.setHeight(Integer.parseInt(this.height.getText()));
		super.onSave();
	}
}