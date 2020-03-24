package de.lmu.ifi.bio.watchdog.executionWrapper;

import java.io.File;
import java.util.ArrayList;

import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import de.lmu.ifi.bio.watchdog.interfaces.XMLPlugin;
import de.lmu.ifi.bio.watchdog.task.Task;

/**
 * Base class for execution wrappers
 * @author Michael Kluge
 *
 */
public abstract class ExecutionWrapper implements XMLDataStore, XMLPlugin {

	private static final long serialVersionUID = -3586587989270382929L;
	
	public static final String PLUGIN_FOLDER = "core_lib" + File.separator + "plugins";
	private final String NAME;
	private final String WATCHDOG_BASE_DIR;
	private String color;
	
	public ExecutionWrapper(String name, String watchdogBaseDir) {
		this.NAME = name;
		this.WATCHDOG_BASE_DIR = watchdogBaseDir;
	}
	
	/**
	 * true, if a specific OS is supported
	 * @param os
	 * @return
	 */
	public abstract boolean doesSupportOS(OS os);
	
	/**
	 * if false, the task command is wrapped (e.g. virtualizers)
	 * if true, before and after scripts are used (e.g. for package managers)
	 * @return
	 */
	public abstract boolean isPackageManager();
	
	/**
	 * tests, if a execution wrapper can be applied on a specific task
	 */
	public abstract boolean canBeAppliedOnTask(Task t);
	
	/**
	 * commands that are added as before commands
	 * @return
	 */
	public ArrayList<String> getInitCommands(Task t) { return null; }
	
	/**
	 * commands that are added as after commands
	 * @return
	 */
	public ArrayList<String> getCleanupCommands(Task t) { return null; }
	
	@Override
	public String getName() {
		return this.NAME;
	}
	
	@Override
	public void setColor(String c) {
		this.color = c;
	}
	@Override
	public String getColor() {
		return this.color;
	}
	
	@Override
	public Class<? extends XMLDataStore> getStoreClassType() {
		return ExecutionWrapper.class;
	}
	
	@Override
	public void onDeleteProperty() {}

	/**
	 * Base path to plugin script folder
	 * @return
	 */
	public String getPluginScriptFolder() {
		return new File(this.WATCHDOG_BASE_DIR + File.separator + PLUGIN_FOLDER).getAbsolutePath();
	}
}
