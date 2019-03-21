package de.lmu.ifi.bio.multithreading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.jcraft.jsch.Logger;

import de.lmu.ifi.bio.utils.interfaces.Logable;

/**
 * Thread run pool that monitors how long tasks have to wait and how long they take to be executed
 * @author kluge
 *
 */
public class RunPool implements Logable {
	
	private final ExecutorService POOL;
	private final int NUMBER_OF_THREADS;
	private final int NUMBER_OF_CONSTANTLY_RESERVED;
	
	private int constantRunningTasks = 0;
	private boolean first = false;
	
	public RunPool(int workerThreads, int constantlyRunningTasks) {
		this.NUMBER_OF_THREADS = workerThreads + constantlyRunningTasks;
		this.NUMBER_OF_CONSTANTLY_RESERVED = constantlyRunningTasks;
		this.POOL = Executors.newFixedThreadPool(this.NUMBER_OF_THREADS + 1); // we need one thread for an internal task
		
		LOGGER.info("Thread pool with " + workerThreads + " working threads and " + constantlyRunningTasks + " threads for constantly running threads was started.");
	}
	
	/**
	 * adds a new task
	 * @param r
	 * @param isConstantlyRunning
	 * @return
	 */
	public synchronized Future<?> addRunnable(MonitorRunnable r, boolean isConstantlyRunning) {	
		// add the monitor thread
		if(this.first)
			this.addRunnable(MonitorRunableExecutionTime.getInstance(), true);
		
		this.first = false;
		
		if(this.POOL.isTerminated() || this.POOL.isShutdown()) {
			return null;
		}
		
		// check if enough resources are available
		if(isConstantlyRunning && this.constantRunningTasks >= this.NUMBER_OF_CONSTANTLY_RESERVED) {
			LOGGER.error("Number of threads reserved for constantly running tasks exceeded ("+ this.NUMBER_OF_CONSTANTLY_RESERVED +")!");
			// provide a stack trace before exit
			LOGGER.printStackTrace();
			System.exit(1);
		}
		else if(isConstantlyRunning) {
			this.constantRunningTasks++;
			LOGGER.debug("Constantly running task added: " + r.getName());
		}
		
		// add the tasks
		Future<?> f = this.POOL.submit(r);
		MonitorRunableExecutionTime.addFutureToMonitor(f, r, isConstantlyRunning);
	
		LOGGER.debug("Runnable '" + r.getName() + "' was added to thread pool.");
		return f;
	}
	
	/**
	 * returns the number of jobs in the short queue
	 * @return
	 */
	public int getNumberOfShortRunningJobs() {
		return Math.min(0, MonitorRunableExecutionTime.getNumberOfNotFinishedTasks() - this.constantRunningTasks);
	}
	
	public boolean canAllConstantlyRunningTasksBeRestarted() {
		return MonitorRunableExecutionTime.canAllConstantlyRunningTasksBeRestarted();
	}
	
	/**
	 * does not accept any more tasks and terminate thread pool after all tasks are finished
	 */
	public void shutdown() {
		if(!this.POOL.isShutdown() && !this.POOL.isTerminated()) {
			LOGGER.info("Shutting down thread pool...");
			MonitorRunableExecutionTime.shutdown();
			this.POOL.shutdown();
		}
	}
	
	/** 
	 * forces to shutdown via interrupt
	 */
	public void shutdownNow() {
		if(!this.POOL.isShutdown() && !this.POOL.isTerminated()) {
			LOGGER.info("Fored shut down of thread pool!");
			this.POOL.shutdownNow();
		}
	}
}
