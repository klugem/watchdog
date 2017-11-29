package de.lmu.ifi.bio.watchdog.executor.external.drmaa;

import de.lmu.ifi.bio.watchdog.executor.external.ExternalScheduledExecutor;
import de.lmu.ifi.bio.watchdog.executor.external.ExternalScheduledMonitorThread;
import de.lmu.ifi.bio.watchdog.logger.Logger;

public class DRMAAMonitorThread extends ExternalScheduledMonitorThread<DRMAAExecutor> {
	
	private static ExternalScheduledMonitorThread<?> instance = null;
	
	/**
	 * hide constructor
	 */
	private DRMAAMonitorThread() {
		super("DRMAAMonitorThread");
		this.connector = getExternalWorkloadManagerConnector();
	}
	
	protected static DRMAAWorkloadManagerConnector getExternalWorkloadManagerConnector() {
		return new DRMAAWorkloadManagerConnector(new Logger());
	}
	
	@SuppressWarnings("unchecked")
	public static void updateMonitorThread() {
		DRMAAExecutor.setExternalScheduledMonitorThread((ExternalScheduledMonitorThread<ExternalScheduledExecutor<?>>) getMonitorThreadInstance());
	}
	
	public static ExternalScheduledMonitorThread<?> getMonitorThreadInstance() {
		if(instance == null || instance.isDead())
			instance = new DRMAAMonitorThread();
		
		return instance;
	}
}