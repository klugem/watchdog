package de.lmu.ifi.bio.watchdog.GUI.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import de.lmu.ifi.bio.watchdog.GUI.datastructure.Module;
import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import de.lmu.ifi.bio.watchdog.GUI.helper.ParamValue;
import de.lmu.ifi.bio.watchdog.GUI.layout.Dependency;
import de.lmu.ifi.bio.watchdog.GUI.layout.RasteredGridPane;
import de.lmu.ifi.bio.watchdog.GUI.properties.PropertyLine;
import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import de.lmu.ifi.bio.watchdog.helper.ProcessBlock.ProcessBlock;
import de.lmu.ifi.bio.watchdog.task.TaskStatus;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask;
import javafx.scene.layout.Pane;
import javafx.util.Pair;

public class WorkflowModule extends Pane implements XMLDataStore, Cloneable {
	
	private static final long serialVersionUID = -6903058313778260008L;
	private WorkflowModuleController controller;
	private static int biggestID = 1;

	/** hide constructor */
	private WorkflowModule() {}
	
	public static WorkflowModule getModule(Module moduleData, int x, int y, RasteredGridPane grid) {
		try {
			FXMLRessourceLoader<WorkflowModule, WorkflowModuleController> l = new FXMLRessourceLoader<>("WorkflowModule.fxml", new WorkflowModule());
			Pair<WorkflowModule, WorkflowModuleController> p = l.getNodeAndController();
			WorkflowModule m = p.getKey();
			m.controller = p.getValue();
			
			// set properties
			m.controller.setModuleData(moduleData);
			m.updateCoordinates(x, y);
			m.controller.setGrid(grid);
			return m;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public Object clone() {
		WorkflowModule clone = getModule(this.controller.getModuleData(), this.getPosition().getKey(), this.getPosition().getValue(), this.controller.getGrid());
		clone.controller.copyProperties(this.controller);
		return clone;
	}
	
	public String getLabel() {
		return this.controller.getLabel();
	}
	
	public void updateCoordinates(int x, int y) {
		this.controller.setCoordinates(x, y);
	}
	
	public HashMap<Dependency, WorkflowModule> getDependencies(boolean incoming) {
		return this.controller.getDependencies(incoming);
	}
	
	public String getKey() {
		return this.controller.getKey();
	}
	
	/*** checker, if properties are there ***/
	public boolean hasEnvironmentProperty() {
		return this.controller.hasEnvironmentProperty();
	}
	public boolean hasExecutorProperty() {
		return this.controller.hasExecutorProperty();
	}
	public boolean hasProcessBlockProperty() {
		return this.controller.hasProcessBlockProperty();
	}
	/*** getter of properties ***/
	public Environment getEnvironmentProperty() {
		return this.controller.getEnvironmentProperty();
	}
	public ExecutorInfo getExecutorProperty() {
		return this.controller.getExecutorProperty();
	}
	public ProcessBlock getProcessBlockProperty() {
		return this.controller.getProcessBlockProperty();
	}
	public Pair<Integer, Integer> getPosition() {
		return this.controller.getPosition();
	}
	protected void onSave(ModulePropertiesController gui) {
		this.controller.onSave(gui);
	}

	@Override
	public String toXML() {
		return this.controller.toXML();
	}

	@Override
	public String getName() {
		return this.controller.getName();
	}
	
	public Module getModule() {
		return this.controller.getModuleData();
	}

	public void setLoadedParameters(LinkedHashMap<String, ParamValue> params) {
		this.controller.setLoadedParameters(params);
	}
	
	public void setLoadedProperties(ArrayList<PropertyLine> props) {
		for(PropertyLine p : props) {
			this.controller.setPropertyData(p.getPropertyData(), p);
		}
	}

	public WorkflowModuleData getSavedData() {
		return this.controller.getSavedData();
	}
	
	public void setStatus(TaskStatus t, XMLTask x, boolean requiresRelease) {
		this.controller.setStatus(t, x, requiresRelease);
	}
	
	@Override
	public void setColor(String c) {}
	@Override
	public String getColor() {return null;}

	public void unregisterData() {
		XMLDataStore.unregisterData(this.controller);
		this.controller.finalize();
		this.controller = null;
	}

	public void wasSavedToFile() {
		this.controller.wasSavedToFile();
	}
	
	@Override
	public void onDeleteProperty() {}

	public static void resetStartID() {
		biggestID = 1;
	}
	
	public static int getNextID() {
		return biggestID++;
	}

	public void resetID() {
		this.controller.resetID();
	}
}
