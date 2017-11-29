package de.lmu.ifi.bio.watchdog.GUI.properties;

import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.WorkflowDesignController;
import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.StatusConsole;
import de.lmu.ifi.bio.watchdog.GUI.css.CSSRessourceLoader;
import de.lmu.ifi.bio.watchdog.GUI.event.DeletePropertyEvent;
import de.lmu.ifi.bio.watchdog.GUI.helper.ScreenCenteredStage;
import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PropertyView;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PropertyViewFactory;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Controller for module that is placed in the workflow
 * */
public class PropertyLineController implements Initializable {

	@FXML private Button config;
	@FXML private Button delete;
	@FXML private Label label;
	@FXML private AnchorPane propLine;
	
	private PropertyManagerController manager;
	private PropertyLine line;
	private PropertyViewFactory viewFactory;
	private PropertyData prop;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {		
		// set images
		this.config.setGraphic(ImageLoader.getImage(ImageLoader.CONFIG_SMALL));
		this.delete.setGraphic(ImageLoader.getImage(ImageLoader.DELETE_SMALL));
		
		// set event handler
		this.config.setOnAction(event -> this.config());
		this.delete.setOnAction(event -> this.delete());
	}
	
	
	public void delete() {
		Parent target = this.propLine.getParent().getParent();
		DeletePropertyEvent event = new DeletePropertyEvent(this.prop.getNumber());
		target.fireEvent(event);
	}
	
	public void config() {
		// add new status message tab for this
		String tabName = "Status of new " + this.viewFactory.getType().getPropertyName();
		try {
			StatusConsole c = StatusConsole.getStatusConsole(tabName);
			WorkflowDesignController.addNewTab(c);
			
			// get property view
			PropertyView view = this.viewFactory.getView(this.prop);
			view.setStatusConsole(c);
			Stage stage = new ScreenCenteredStage();
			stage.setTitle("Propertymanager");
			stage.setResizable(false);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.focusedProperty().addListener(event -> stage.requestFocus());
			Scene scene = new Scene(view);

			// add CSS
			scene.getStylesheets().add(CSSRessourceLoader.getCSS("control.css"));
			stage.setScene(scene);
			
			// try to load some data
			if(this.prop.hasXMLData())
				view.loadData(this.prop.getXMLData());	
			
			stage.showAndWait();
			// test, if user clicked on save
			if(view.getStoredData() != null)  {
				this.prop.setXMLData(view.getStoredData());
			
				// update the label
				this.setLabel(this.prop.getXMLData().getName());
				
				// update color and number if duplicate
				this.manager.updateLabel(this.line, null);
			}
			// remove the console
			WorkflowDesignController.removeTab(tabName);
		}
		catch(Exception e) {
			e.printStackTrace();
			WorkflowDesignController.removeTab(tabName);  // remove the console
		}
	}
	
	/**
	 * true, if some XMLData is stored
	 * @return
	 */
	public boolean hasDataStored() {
		return this.prop.hasXMLData();
	}
	
	public XMLDataStore getStoredData() {
		if(this.hasDataStored())
			return this.prop.getXMLData();
		return null;
	}
	
	public void setStoredData() {
		
	}

	public void setLabel(String label) {
		this.label.setText(label);
	}

	public void setProperty(PropertyData prop) {	
		this.prop = prop;
		Property p = Property.getProperty(this.line, prop.getColor(), prop.getNumber(), true);
		p.setPropertyData(prop, p.getParentLine());
		// replace it at GUI
		this.propLine.getChildren().set(0, p);
	}
	
	public void setViewFactory(PropertyViewFactory factory) {	
		this.viewFactory = factory;
	}

	protected PropertyData getProperty() {
		return this.prop;
	}

	public void setDisplayNumber(int number, Color c) {
		this.prop.setDisplayNumber(number, c);
	}

	public void setManager(PropertyManagerController m) {
		this.manager = m;
	}


	public void setPropertyLine(PropertyLine line) {
		this.line = line;
	}

	public void resetCustomLabelAndColor() {
		this.prop.setDisplayNumber(this.prop.getNumber(), this.prop.getColor());
	}
}
