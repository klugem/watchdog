package de.lmu.ifi.bio.watchdog.GUI.module;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.StatusConsole;
import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import de.lmu.ifi.bio.watchdog.task.TaskAction;
import javafx.scene.layout.Pane;

public class TaskActionWindow extends Pane {
	
	private TaskActionWindowController controller;
	
	/** hide constructor */
	private TaskActionWindow() {}
	
	public TaskAction getStoredData() {
		return (TaskAction) this.controller.getStoredData();
	}
	
	public void loadData(TaskAction data) {
		this.controller.loadData(data);
	}

	public static TaskActionWindow getNewTaskAction(WorkflowModuleController module, StatusConsole console) {
		try {
			FXMLRessourceLoader<TaskActionWindow, TaskActionWindowController> l = new FXMLRessourceLoader<>("TaskActionWindow.fxml", new TaskActionWindow());
			Pair<TaskActionWindow, TaskActionWindowController> p = l.getNodeAndController();
			TaskActionWindow m = p.getKey();
			m.controller = p.getValue();
			
			// set properties
			m.controller.setStatusConsole(console);
			m.controller.setWorkflowModule(module);
			return m;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
