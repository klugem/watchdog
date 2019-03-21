package de.lmu.ifi.bio.watchdog.executor.external.sge;

import de.lmu.ifi.bio.watchdog.executor.external.ExternalScheduledExecutor;
import de.lmu.ifi.bio.watchdog.executor.external.ExternalScheduledMonitorThread;
import de.lmu.ifi.bio.watchdog.logger.Logger;

public class SGEMonitorThread extends ExternalScheduledMonitorThread<SGEExecutor> {
	
	private static ExternalScheduledMonitorThread<?> instance = null;
	
	/**
	 * hide constructor
	 */
	private SGEMonitorThread() {
		super("SGEMonitorThread");
		this.connector = getExternalWorkloadManagerConnector();
	}
	
	protected static SGEWorkloadManagerConnector getExternalWorkloadManagerConnector() {
		return new SGEWorkloadManagerConnector(new Logger());
	}
	
	@SuppressWarnings("unchecked")
	public static void updateMonitorThread() {
		SGEExecutor.setExternalScheduledMonitorThread((ExternalScheduledMonitorThread<ExternalScheduledExecutor<?>>) getMonitorThreadInstance());
	}
	
	public static ExternalScheduledMonitorThread<?> getMonitorThreadInstance() {
		if(instance == null || instance.isDead())
			instance = new SGEMonitorThread();
		
		return instance;
	}
}