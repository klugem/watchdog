package de.lmu.ifi.bio.watchdog.executor.external;

import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;

public abstract class ExternalExecutorInfo extends ExecutorInfo {

	private static final long serialVersionUID = 1111797787189864001L;

	public ExternalExecutorInfo(String type, String name, boolean isDefault, boolean isStick2Host, Integer maxSlaveRunning, String path2java, int maxRunning, String watchdogBaseDir, Environment environment, String workingDir, String shebang) {
		super(type, name, isDefault, isStick2Host, maxSlaveRunning, path2java, maxRunning, watchdogBaseDir, environment, workingDir, shebang);
	}
}
