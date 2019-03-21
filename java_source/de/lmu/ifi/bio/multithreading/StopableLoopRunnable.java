package de.lmu.ifi.bio.multithreading;

import java.util.concurrent.TimeUnit;

import de.lmu.ifi.bio.utils.interfaces.StopableLoop;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;

public abstract class StopableLoopRunnable extends MonitorRunnable implements StopableLoop {

	private boolean requestStop = false;
	private boolean forcedInterrupt = false;
	private boolean wasStopped = false;
	
	public StopableLoopRunnable(String name) {
		super(name);
	}
	
	@Override
	public final void run() {
		LOGGER.debug("LoopRunnable '" + this.getName() + "' was started.");
		this.beforeLoop();
		try {
			// run executeLoop until stop is requested
			while(!this.isStopRequested()) {
				int factor = Math.max(this.executeLoop(), 1);
				
				if(this.isStopRequested()) {
					this.wasStopped = true;
					break;
				}
				
				Thread.sleep(this.getDefaultWaitTime()*factor);
			}
			LOGGER.debug("LoopRunnable '" + this.getName() + "' was stopped after stop request.");
		}
		catch(InterruptedException e) {
			LOGGER.debug("LoopRunnable '" + this.getName() + "' was interrupped (forced: " + this.forcedInterrupt + ").");
		}
		catch(Exception e) {
			LOGGER.error("Some error occured during loop...see below:");
			e.printStackTrace();
		}
		finally {
			this.wasStopped = true;
			this.afterLoop();
			super.run();
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
		Thread.currentThread().interrupt();
	}
	
	/**
	 * true, if a stop is requested
	 * @return
	 */
	public boolean isStopRequested() {
		return this.requestStop;
	}
	
	/**
	 * check, if the thread was stopped
	 * @return
	 */
	public boolean wasStopped() {
		return this.wasStopped;
	}
	
	/**
	 * true, if a forced stop is requested
	 * @return
	 */
	public boolean isForcedStopRequested() {
		return this.forcedInterrupt;
	}
}
