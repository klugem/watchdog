package de.lmu.ifi.bio.watchdog.xmlParser.plugins.executorParser;

import de.lmu.ifi.bio.watchdog.executor.Executor;
import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.SyncronizedLineWriter;
import de.lmu.ifi.bio.watchdog.task.Task;

public class DefaultExecutorInfo extends ExecutorInfo {

	private static final long serialVersionUID = 1985752316536563754L;

	/**
	 * Constructor
	 * @param name
	 * @param isDefault
	 * @param isStick2Host
	 * @param maxSlaveRunning
	 * @param path2java
	 * @param maxRunning
	 * @param watchdogBaseDir
	 * @param environment
	 * @param workingDir
	 */
	public DefaultExecutorInfo(String name, boolean isDefault, boolean isStick2Host, int maxSlaveRunning, String path2java, int maxRunning, String watchdogBaseDir, Environment environment, String workingDir) {
		super(null, name, isDefault, isStick2Host, maxSlaveRunning, path2java, maxRunning, watchdogBaseDir, environment, workingDir);
	}

	@Override
	public String toXML() {
		// we can not save the default executor info
		throw new IllegalArgumentException("DefaultExecutorInfo class is only placeholder for mandatory arguments of executors!");
	}

	@Override
	public Executor<?> getExecutorForTask(Task t, SyncronizedLineWriter logFile) {
		// just dummy class for mandatory values --> do not create a Executor
		throw new IllegalArgumentException("DefaultExecutorInfo class is only placeholder for mandatory arguments of executors!");
	}

	@Override
	public Object[] getDataToLoadOnGUI() { return null; }

	@Override
	public boolean isWatchdogRestartSupported() {
		return false;
	}
}