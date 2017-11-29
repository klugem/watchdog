package de.lmu.ifi.bio.watchdog.GUI.module;

import java.util.HashMap;

import de.lmu.ifi.bio.watchdog.task.StatusHandler;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask;
/**
 * Warning: Used classes are not Serializable! Do not even try. ;)
 * @author kluge
 *
 */
public class GUIStatusUpdateHandler implements StatusHandler {
	private final HashMap<String, WorkflowModule> MODULES = new HashMap<>();

	public GUIStatusUpdateHandler(HashMap<String, WorkflowModule> activeModules) {
		for(WorkflowModule m : activeModules.values())
			this.MODULES.put(m.getName(), m);
	}

	@Override
	public void handle(Task task) {
		// find the correct module
		if(this.MODULES.containsKey(task.getName())) {
			WorkflowModule m = this.MODULES.get(task.getName());
			m.setStatus(task.getStatus(), XMLTask.getXMLTask(task.getTaskID()), !task.getCheckpoint().isDisabled());
		}
	}
}
