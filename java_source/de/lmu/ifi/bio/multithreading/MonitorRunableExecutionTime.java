package de.lmu.ifi.bio.multithreading;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Singleton class that can monitor how long a runable was executed
 * @author kluge
 *
 */
public class MonitorRunableExecutionTime extends StopableLoopRunnable {
	
	private static int WAIT_TIME_MS = 250;
	private static MonitorRunableExecutionTime monitor = new MonitorRunableExecutionTime();
	
	private final Map<MonitorRunnable, Future<?>> FUTURES = new ConcurrentHashMap<MonitorRunnable, Future<?>>();
	private final HashSet<MonitorRunnable> IS_CONSTANTLY_RUNNING = new HashSet<>(); // sync is done over FUTURE
	
	/**
	 * hide constructor as it is a singleton class
	 */
	private MonitorRunableExecutionTime() {
		super("MonitorRunableExecutionTime");
	}

	@Override
	public int executeLoop() throws InterruptedException {
		Entry<MonitorRunnable, Future<?>> e = null;
		MonitorRunnable r = null;
		Future<?> f = null;
		// iterate over all things to monitor
		for(Iterator<Entry<MonitorRunnable, Future<?>>> it = MonitorRunableExecutionTime.monitor.FUTURES.entrySet().iterator(); it.hasNext(); ) {
			e = it.next();
			r = e.getKey();
			f = e.getValue();
			if(f.isCancelled()) {
				print(r, true);
				it.remove();
				this.IS_CONSTANTLY_RUNNING.remove(r);
			}
			else if(f.isDone()) {
				print(r, false);
				it.remove();
				this.IS_CONSTANTLY_RUNNING.remove(r);
			}
		}
		// default wait time in any case
		return 1;
	}
	
	private int getNumberOfNotFinishedOrWaitingTasks() {
		return this.FUTURES.size();
	}
	
	private void shutdownTasks() {
		Entry<MonitorRunnable, Future<?>> e = null;
		MonitorRunnable r = null;
		for(Iterator<Entry<MonitorRunnable, Future<?>>> it = MonitorRunableExecutionTime.monitor.FUTURES.entrySet().iterator(); it.hasNext(); ) {
			e = it.next();
			r = e.getKey();
			if(r instanceof StopableLoopRunnable)
				((StopableLoopRunnable) r).requestStop(250, TimeUnit.MILLISECONDS);
		}
		this.requestStop(25, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Once a Runnable was finished, calculate times and print messages to Logger 
	 * @param r
	 * @param wasCancelled
	 */
	private void print(MonitorRunnable r, boolean wasCancelled) {
		long create = r.getCreationTime();
		long start = r.getExecutionStart();
		long current = System.currentTimeMillis();
		
		long execSeconds = TimeUnit.MILLISECONDS.toSeconds(current-start);
		long waitSeconds = TimeUnit.MILLISECONDS.toSeconds(start-create);
		
		String m = "Runnable '" + r.getName() + "' finished after " + execSeconds + " seconds of run-time and " + waitSeconds + " of wait time in until thread pool was ready.";
		if(!wasCancelled)
			LOGGER.debug(m);
		else
			LOGGER.error(m);
	}

	@Override
	public long getDefaultWaitTime() {
		return MonitorRunableExecutionTime.WAIT_TIME_MS;
	}
	
	@Override
	public void requestStop(long timeout, TimeUnit u) {
		super.requestStop(timeout, u);
	}
	
	@Override
	public void requestForcedStop() {
		super.requestForcedStop();
	}
	
	/*********************************** STATIC FUNCTIONS ********************************************/
	
	/**
	 * Returns the singleton instance in order to start it
	 * @return
	 */
	public static MonitorRunableExecutionTime getInstance() {
		return MonitorRunableExecutionTime.monitor;
	}
	
	/**
	 * Adds a runnable that should be monitored
	 * @param f
	 * @param r
	 */
	public static void addFutureToMonitor(Future<?> f, MonitorRunnable r, boolean isConstantlyRunning) {
		MonitorRunableExecutionTime.monitor.FUTURES.put(r, f);
		if(isConstantlyRunning)
			MonitorRunableExecutionTime.monitor.IS_CONSTANTLY_RUNNING.add(r);
	}

	@Override
	public void afterLoop() {
		MonitorRunableExecutionTime.monitor.FUTURES.clear();
		MonitorRunableExecutionTime.monitor.IS_CONSTANTLY_RUNNING.clear();
		MonitorRunableExecutionTime.monitor = null;
		MonitorRunableExecutionTime.monitor = new MonitorRunableExecutionTime();
	}

	public static void shutdown() {
		MonitorRunableExecutionTime.monitor.shutdownTasks();
	}

	@Override
	public void beforeLoop() {}

	public static int getNumberOfNotFinishedTasks() {
		return MonitorRunableExecutionTime.monitor.getNumberOfNotFinishedOrWaitingTasks();
	}
	
	/**
	 * test, if all constantly running tasks can be restarted (detached)
	 * @return
	 */
	public static boolean canAllConstantlyRunningTasksBeRestarted() {
		for(MonitorRunnable r : MonitorRunableExecutionTime.monitor.IS_CONSTANTLY_RUNNING) {
			if(!r.canBeStoppedForDetach()) {
				LOGGER.debug("Thread " + r.getName() + " can not be restarted for detach.");
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean canBeStoppedForDetach() {
		return true;
	}
}
