package de.lmu.ifi.bio.watchdog.GUI.module;

import java.util.ArrayList;

import de.lmu.ifi.bio.watchdog.GUI.WorkflowDesignerRunner;
import de.lmu.ifi.bio.watchdog.GUI.helper.ErrorCheckerStore;
import de.lmu.ifi.bio.watchdog.helper.ActionType;
import de.lmu.ifi.bio.watchdog.task.TaskAction;

public class WorkflowModuleData {

	public Integer id;
	public String name;
	public int simMaxRunning = -1;
	public ActionType notify = ActionType.DISABLED;
	public ActionType confirm = ActionType.DISABLED;
	public ActionType checkpoint = ActionType.DISABLED;
		
	// streams
	public boolean appendOut = false;
	public boolean appendErr = false;
	public boolean enforceStdin = true;
	public String stdOut;
	public String stdErr;
	public String workingDir = WorkflowDesignerRunner.DEFAULT_WORKDIR;
	public String stdIn;
	
	// error checker / actions
	public final ArrayList<ErrorCheckerStore> CHECKERS = new ArrayList<>();
	public final ArrayList<TaskAction> ACTIONS = new ArrayList<>();
	
	
	public void addErrorChecker(ErrorCheckerStore e) {
		this.CHECKERS.add(e);
	}
	
	public void addTaskAction(TaskAction a) {
		this.ACTIONS.add(a);
	}
}
