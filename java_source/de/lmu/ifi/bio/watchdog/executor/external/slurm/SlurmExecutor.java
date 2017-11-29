package de.lmu.ifi.bio.watchdog.executor.external.slurm;

import de.lmu.ifi.bio.watchdog.executor.external.ExternalScheduledExecutor;
import de.lmu.ifi.bio.watchdog.executor.external.ExternalScheduledMonitorThread;
import de.lmu.ifi.bio.watchdog.helper.SyncronizedLineWriter;
import de.lmu.ifi.bio.watchdog.task.Task;

public class SlurmExecutor extends ExternalScheduledExecutor<SlurmExecutorInfo> {

	private static ExternalScheduledMonitorThread<ExternalScheduledExecutor<?>> monitor;

	public SlurmExecutor(Task task, SyncronizedLineWriter log, int retryCount, SlurmExecutorInfo execInfo) {
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
