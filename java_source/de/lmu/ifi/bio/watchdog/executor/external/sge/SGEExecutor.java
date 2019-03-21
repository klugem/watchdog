package de.lmu.ifi.bio.watchdog.executor.external.sge;

import de.lmu.ifi.bio.watchdog.executor.external.ExternalScheduledExecutor;
import de.lmu.ifi.bio.watchdog.executor.external.ExternalScheduledMonitorThread;
import de.lmu.ifi.bio.watchdog.helper.SyncronizedLineWriter;
import de.lmu.ifi.bio.watchdog.task.Task;

public class SGEExecutor extends ExternalScheduledExecutor<SGEExecutorInfo> {

	private static ExternalScheduledMonitorThread<ExternalScheduledExecutor<?>> monitor;

	public SGEExecutor(Task task, SyncronizedLineWriter log, int retryCount, SGEExecutorInfo execInfo) {
		super(task, log, retryCount, execInfo);
	}
	
	/**
	 * shadows method of parent class
	 * @param thread
	 */
	public static void setExternalScheduledMonitorThread(ExternalScheduledMonitorThread<ExternalScheduledExecutor<?>> thread) {
		monitor = thread;
	}

	@Override
	public ExternalScheduledMonitorThread<ExternalScheduledExecutor<?>> getMonitor() {
		return monitor;
	}
}
