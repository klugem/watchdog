package de.lmu.ifi.bio.watchdog.GUI.helper;

import java.io.File;
import java.util.concurrent.TimeUnit;

import de.lmu.ifi.bio.multithreading.StopableLoopThread;
import de.lmu.ifi.bio.watchdog.executor.MonitorThread;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask2TaskThread;

public class FinishedCheckerThread extends StopableLoopThread {
	
	private final XMLTask2TaskThread X;
	private final Runnable CALL;
	public final static int SLEEP = 3000; // check every 3s if all tasks are finished!
	private String attachInfo;
	private File resumeFile;
	
	public FinishedCheckerThread(Runnable onceFinished, XMLTask2TaskThread x, String attachInfo, File resumeFile) {
		super("FinishedChecker");
		this.CALL = onceFinished;
		this.X = x;
		this.resumeFile = resumeFile;
		this.attachInfo = attachInfo;
	}

	@Override
	public int executeLoop() throws InterruptedException {
		if(!this.isInterrupted() && this.X.hasUnfinishedTasks() && !MonitorThread.wasDetachModeOnAllMonitorThreads()) {
			return 1;
		}
		if(MonitorThread.wasDetachModeOnAllMonitorThreads()) {
			// do not end program
			boolean wasDetachPerformed = this.X.processAllTasksAndBlock(false);
			if(wasDetachPerformed) {
				this.X.writeReattchFile(this.attachInfo, this.resumeFile);
			}
			this.X.requestStop(5, TimeUnit.SECONDS);
		}
		// only make the call, if it was not interrupted
		if(!this.isInterrupted())
			this.CALL.run();

		return 1;
	}

	@Override
	public void beforeLoop() {}

	@Override
	public void afterLoop() {}

	@Override
	public long getDefaultWaitTime() {
		return SLEEP;
	}
}
