package de.lmu.ifi.bio.watchdog.GUI.module;

import java.util.ArrayList;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import de.lmu.ifi.bio.watchdog.GUI.interfaces.Validator;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

public class ModuleProperties extends Pane {
	
	protected ModulePropertiesController controller;

	/** hide constructor */
	private ModuleProperties() {}

	public static ModuleProperties getModuleProperties(GridPane requiredGrid, GridPane optionalGrid, WorkflowModuleController module) {
		try {
			FXMLRessourceLoader<ModuleProperties, ModulePropertiesController> l = new FXMLRessourceLoader<>("ModuleProperties.fxml", new ModuleProperties());
			Pair<ModuleProperties, ModulePropertiesController> p = l.getNodeAndController();
			ModuleProperties m = p.getKey();
			m.controller = p.getValue();
			
			// set properties
			m.controller.setGridPanes(requiredGrid, optionalGrid);
			m.controller.setWorkflowModule(module);
			return m;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean validate() {
		return this.controller.validate();
	}

	public void addValidateToControl(Control c, Validator<Control> v) {
		this.controller.addValidateToControl(c, v);
	}

	public void setReadOnly(boolean readOnly) {
		if(readOnly) {
			this.controller.setReadOnly(true);
			// collect all children that should be made readonly
			for(Node n : getReadOnlyChilds(this))
				n.setDisable(true);
		}
	}
	
	private static ArrayList<Node> getReadOnlyChilds(Node root) {
		ArrayList<Node> l = new ArrayList<>();
		getReadOnlyChildsHelper(root, l);
		return l;
	}
	
	private static void getReadOnlyChildsHelper(Node root, ArrayList<Node> l) {
		if(root == null)
			return;
		// collect results
		if(root instanceof TextField || root instanceof CheckBox || root instanceof RadioButton || root instanceof Button) {
			l.add(root);
		}
		// recursive calls
		else if(root instanceof TabPane || root instanceof TitledPane || root instanceof ScrollPane) {
			if(root instanceof TabPane) {
				for(Tab t : ((TabPane) root).getTabs())
					getReadOnlyChildsHelper(t.getContent(), l);
			}
			else if(root instanceof TitledPane)
				getReadOnlyChildsHelper(((TitledPane) root).getContent(), l);
			else if(root instanceof ScrollPane)
				getReadOnlyChildsHelper(((ScrollPane) root).getContent(), l);
		}
		else if(root instanceof Parent) {
			Parent p = (Parent) root;
			for(Node n : p.getChildrenUnmodifiable())
				getReadOnlyChildsHelper(n, l);
		}
	}
}
