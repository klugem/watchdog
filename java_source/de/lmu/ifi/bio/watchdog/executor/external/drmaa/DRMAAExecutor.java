package de.lmu.ifi.bio.watchdog.executor.external.drmaa;

import de.lmu.ifi.bio.watchdog.executor.external.ExternalScheduledExecutor;
import de.lmu.ifi.bio.watchdog.executor.external.ExternalScheduledMonitorThread;
import de.lmu.ifi.bio.watchdog.helper.SyncronizedLineWriter;
import de.lmu.ifi.bio.watchdog.task.Task;

public class DRMAAExecutor extends ExternalScheduledExecutor<DRMAAExecutorInfo> {
	
	private static ExternalScheduledMonitorThread<ExternalScheduledExecutor<?>> monitor;

	public DRMAAExecutor(Task task, SyncronizedLineWriter log, int retryCount, DRMAAExecutorInfo execInfo) {
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
