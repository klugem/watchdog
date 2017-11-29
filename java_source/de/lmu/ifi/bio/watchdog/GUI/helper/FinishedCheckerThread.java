package de.lmu.ifi.bio.watchdog.GUI.helper;

import de.lmu.ifi.bio.multithreading.StopableLoopThread;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask2TaskThread;

public class FinishedCheckerThread extends StopableLoopThread {
	
	private final XMLTask2TaskThread X;
	private final Runnable CALL;
	public final static int SLEEP = 3000; // check every 3s if all tasks are finished!
	
	public FinishedCheckerThread(Runnable onceFinished, XMLTask2TaskThread x) {
		super("FinishedChecker");
		this.CALL = onceFinished;
		this.X = x;
	}

	@Override
	public int executeLoop() throws InterruptedException {
		if(!this.isInterrupted() && this.X.hasUnfinishedTasks()) {
			return 1;
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
