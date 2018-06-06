package de.lmu.ifi.bio.watchdog.GUI.properties;


import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import com.sun.javafx.scene.control.skin.CustomColorDialog;

import de.lmu.ifi.bio.watchdog.GUI.WorkflowDesignController;
import de.lmu.ifi.bio.watchdog.GUI.datastructure.ExtendedClipboardContent;
import de.lmu.ifi.bio.watchdog.GUI.helper.ScreenCenteredStage;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PropertyViewType;
import de.lmu.ifi.bio.watchdog.GUI.useraction.MovePropertyAction;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessBlock;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Controller for simple rectangle property
 * */
@SuppressWarnings("restriction")
public class PropertyController implements Initializable {

	@FXML private Rectangle rect;
	@FXML private Label number;
	@FXML private Pane movePane;
	@FXML private Arc arc;
	
	private PropertyData p;
	private PropertyLine parent;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.movePane.setOnDragDetected(event -> { if(this.p != null && this.p.hasXMLData()) this.startDrag(event); });
		this.movePane.setOnMouseReleased(e -> { if(this.p != null && this.p.hasXMLData()) this.selectColor(); });
	}
	

	private void selectColor() {
		// no changes in read-only mode
		if(WorkflowDesignController.isInExecutionMode())
			return;
		
		try {
			Stage stage = new ScreenCenteredStage();
			stage.setTitle("Select new color for property");
			stage.setResizable(true);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.focusedProperty().addListener(event -> stage.requestFocus());
			Pane root = new Pane();
			Scene scene = new Scene(root);
			stage.setScene(scene);
			
			// add the picker
			CustomColorDialog picker = new CustomColorDialog(scene.getWindow());
			picker.setPrefWidth(picker.getPrefWidth()+25); // adjust width problem
			picker.setCurrentColor(this.p.getColor());
			root.getChildren().add(picker);
			
			// set the event handler
			picker.setOnCancel(() -> stage.close());
			picker.setOnUse(() -> { this.setColor(this.getColorFromPicker(picker)); stage.close(); });
			picker.setOnSave(() -> { this.setColor(this.getColorFromPicker(picker)); stage.close(); });
			
			// show the window and wait until the user closed it
			stage.showAndWait();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * get the new color using the reflection API
	 * @param picker
	 * @return
	 */
	private Color getColorFromPicker(CustomColorDialog picker) {
		try {
			Class<? extends CustomColorDialog> clazz = picker.getClass();
			Method getCustomColor = clazz.getDeclaredMethod("getCustomColor");
			getCustomColor.setAccessible(true); // make it callable
			return (Color) getCustomColor.invoke(picker);	
		}
		catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void setProperty(PropertyData p, boolean internalUpdateCall) {
		this.p = p;
		if(this.p != null) {
			this.setColor(this.p.getColor(), internalUpdateCall);
			this.setNumber(this.p.getNumber(), internalUpdateCall);
		}
		else {
			this.setColor(null, internalUpdateCall);
			this.setNumber(null, internalUpdateCall);
		}
	}
	
	private void setColor(Color p) {
		this.setColor(p, false);
	}
	
	private void setColor(Color p, boolean internalUpdateCall) {
		if(p == null) {
			this.rect.setFill(Color.TRANSPARENT);
			if(this.p != null) this.p.setColor(Color.TRANSPARENT, internalUpdateCall);
		}
		else {
			this.rect.setFill(p);
			if(this.p != null) this.p.setColor(p, internalUpdateCall);
		}
		
		// update all properties with the same name (for append)
		if(this.p != null && this.p.hasXMLData()) {
			PropertyManager m = PropertyManager.getPropertyManager(PropertyViewType.getCorrespondingType(this.p.getXMLData().getStoreClassType()));
			m.updateAppendColor(this.p.getXMLData().getName(), p);
		}
		
		// this request comes from picker --> update color on manager
		if(this.p != null && this.p.hasXMLData()) {
			PropertyManager m = PropertyManager.getPropertyManager(PropertyViewType.getCorrespondingType(this.p.getXMLData().getStoreClassType()));
			m.updateUsedColor(this.parent, this.p.getNumber(), p);
		}
	}

	private void setNumber(Integer number, boolean internalUpdateCall) {
		if(number != null)
			this.number.setText(number.toString());
		else
			this.number.setText("");
		
		if(this.p != null) this.p.setNumber(number, internalUpdateCall);
	}
	
	/**
	 * is called, when the user starts to drag a property from the toolbar
	 * @param event
	 */
	private void startDrag(MouseEvent event) {
		// no drag&drop in read-only mode
		if(WorkflowDesignController.isInExecutionMode())
			return;
		
		Dragboard db = this.movePane.startDragAndDrop(TransferMode.LINK);
		ExtendedClipboardContent content = new ExtendedClipboardContent();
		int id = this.p.getID();
		PropertyLine parent = this.parent;
		// get the ID of the first in pair if it is a append processblock
		if(this.p != null && this.p.hasXMLData()) {
			PropertyManager m = PropertyManager.getPropertyManager(PropertyViewType.getCorrespondingType(this.p.getXMLData().getStoreClassType()));
			ArrayList<PropertyLine> same = m.getDataWithSameName4Append(this.p.getXMLData().getName());

			for(PropertyLine check : same) {
				if(check.hasDataStored() && check.getPropertyData().hasXMLData()) {
					XMLDataStore x = check.getPropertyData().getXMLData();
					if(x instanceof ProcessBlock && ((ProcessBlock) x).gui_append == false) {
						id = check.getPropertyData().getID();
						parent = check;
						break;
					}
				}
			}
		}
		
		// create the event
        content.putUserAction(new MovePropertyAction(id, this.movePane.getParent().getParent().getParent() instanceof AnchorPane, parent));
        db.setContent(content);
        event.consume();
	}

	public void hide(boolean hidden) {
		this.movePane.setVisible(!hidden);
		this.rect.setVisible(!hidden);
		this.arc.setVisible(hidden);
	}

	public void setDisplayNumber(Integer number, Color c) {
		if(number != null)
			this.number.setText(Integer.toString(number));
		this.rect.setFill(c);	
	}

	public void setParentLine(PropertyLine parent) {
		this.parent = parent;
	}

	public PropertyLine getParentLine() {
		return this.parent;
	}
}
