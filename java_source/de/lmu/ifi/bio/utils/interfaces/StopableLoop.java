package de.lmu.ifi.bio.utils.interfaces;

import java.util.concurrent.TimeUnit;

public interface StopableLoop extends Logable {
	
	/**
	 * function that is executed until the thread is stopped
	 * @return indicates how long the thread should wait until the next call is made (is multiplied with getDefaultWaitTime())
	 */
	public abstract int executeLoop() throws InterruptedException;
	
	/**
	 * is executed before the loop is left in any case
	 */
	public abstract void beforeLoop();
	
	/**
	 * is executed after the loop is left in any case
	 */
	public abstract void afterLoop();
		
	/**
	 * default wait time in ms after executeLoop() loop was called;
	 * the value is multiplied with the value returned by executeLoop()
	 * if executeLoop() returns a positive value
	 * @return
	 */
	public abstract long getDefaultWaitTime();

	/**
	 * must be called when executeLoop should be stopped
	 * @param timeout time after which the thread should be killed via interrupt
	 * @param u
	 */
	public void requestStop(long timeout, TimeUnit u);
	
	/**
	 * can be called to stop by force
	 */
	public abstract void requestForcedStop();
	
	/**
	 * true, if a stop is requested
	 * @return
	 */
	public abstract boolean isStopRequested();
	
	/**
	 * true, if a stop is requested
	 * @return
	 */
	public abstract boolean isForcedStopRequested();
}
