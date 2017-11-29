package de.lmu.ifi.bio.watchdog.executor.external.slurm;

import de.lmu.ifi.bio.watchdog.executor.external.ExternalScheduledExecutor;
import de.lmu.ifi.bio.watchdog.executor.external.ExternalScheduledMonitorThread;
import de.lmu.ifi.bio.watchdog.logger.Logger;

public class SlurmMonitorThread extends ExternalScheduledMonitorThread<SlurmExecutor> {
	
	private static ExternalScheduledMonitorThread<?> instance = null;
	
	/**
	 * hide constructor
	 */
	private SlurmMonitorThread() {
		super("SlurmMonitorThread");
		this.connector = getExternalWorkloadManagerConnector();
	}
	
	protected static SlurmWorkloadManagerConnector getExternalWorkloadManagerConnector() {
		return new SlurmWorkloadManagerConnector(new Logger());
	}
	
	@SuppressWarnings("unchecked")
	public static void updateMonitorThread() {
		SlurmExecutor.setExternalScheduledMonitorThread((ExternalScheduledMonitorThread<ExternalScheduledExecutor<?>>) getMonitorThreadInstance());
	}
	
	public static ExternalScheduledMonitorThread<?> getMonitorThreadInstance() {
		if(instance == null || instance.isDead())
			instance = new SlurmMonitorThread();
		
		return instance;
	}
}