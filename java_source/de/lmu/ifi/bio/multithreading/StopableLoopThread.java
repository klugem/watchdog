package de.lmu.ifi.bio.multithreading;

import java.util.concurrent.TimeUnit;

import de.lmu.ifi.bio.utils.interfaces.StopableLoop;


/**
 * Implements a thread that can be safely stopped without using deprecated methods
 * @author kluge
 *
 */
public abstract class StopableLoopThread extends Thread implements StopableLoop {

	private boolean runWasCalled = false;
	private boolean requestStop = false;
	private boolean forcedInterrupt = false;
	private static int counter = 0;
	/**
	 * thread constructor
	 * @param name name of the thread whereby a thread counter is added internally
	 */
	public StopableLoopThread(String name) {
		super(name + "_" + StopableLoopThread.counter++);
	}
	
	@Override
	public final void run() {
		this.runWasCalled = true;
		LOGGER.debug("Thread '" + this.getName() + "' was started.");
		this.beforeLoop();
		try {
			// run executeLoop until stop is requested
			while(!this.isStopRequested() && !this.isInterrupted()) {
				int factor = Math.max(this.executeLoop(), 1);
				
				if(this.isStopRequested() || this.isInterrupted())
					break;
				
				Thread.sleep(this.getDefaultWaitTime()*factor);
			}
			LOGGER.debug("Thread '" + this.getName() + "' was stopped after stop request.");
		}
		catch(InterruptedException e) {
			LOGGER.warn("Thread '" + this.getName() + "' was interrupped (forced: " + this.isForcedStopRequested() + ").");
		}
		finally {
			LOGGER.debug("After loop call of '"+ this.getClass().getCanonicalName() +"'.");
			this.afterLoop();
		}
	}

	/**
	 * must be called when executeLoop should be stopped
	 * @param timeout time after which the thread should be killed via interrupt
	 * @param u
	 */
	public void requestStop(long timeout, TimeUnit u) {
		this.requestStop = true;
	}
	
	/**
	 * can be called to stop the thread using interrupts
	 */
	public void requestForcedStop() {
		this.forcedInterrupt = true;
		this.interrupt();
	}
	
	/**
	 * true, if a stop is requested
	 * @return
	 */
	public boolean isStopRequested() {
		return this.requestStop;
	}
	
	/**
	 * true, if a forced stop is requested
	 * @return
	 */
	public boolean isForcedStopRequested() {
		return this.forcedInterrupt;
	}
	
	/**
	 * true, if run() method was called 
	 * @return
	 */
	public boolean wasThreadStartedOnce() {
		return this.runWasCalled;
	}
}
