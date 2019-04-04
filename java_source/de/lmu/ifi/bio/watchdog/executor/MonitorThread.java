package de.lmu.ifi.bio.watchdog.executor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import de.lmu.ifi.bio.multithreading.StopableLoopThread;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.task.TaskStatus;

/**
 * Abstract monitor thread class to monitor executors
 * @author Michael Kluge
 *
 */
public abstract class MonitorThread<E extends Executor<?>> extends StopableLoopThread {
	protected static final Logger LOGGER = new Logger(LogLevel.INFO);
	public final LinkedHashMap<String, E> MONITOR_TASKS = new LinkedHashMap<>();
	private static boolean WAS_DETACH_MODE_SET = false;
	private static ArrayList<Task> DETACH_INFO = new ArrayList<>();
	
	// used for pausing / resume of scheduling
	private static boolean stopWasCalled = false;
	private static final Set<MonitorThread<? extends Executor<?>>> CREATED_MONITOR_THREADS = Collections.synchronizedSet(new LinkedHashSet<MonitorThread<? extends Executor<?>>>());
	private boolean isDead = false;
	private boolean isSchedulingPaused = false;
	private boolean detachMode = false;
	
	public MonitorThread(String name) {
		super(name);
		synchronized(CREATED_MONITOR_THREADS) { MonitorThread.CREATED_MONITOR_THREADS.add(this); }
	
		// ensure that all monitor threads have restart mode set
		this.setDetachMode(MonitorThread.WAS_DETACH_MODE_SET);
	}
	
	
	public void setPauseScheduling(boolean pause) {
		this.isSchedulingPaused = pause;
	}
	
	public boolean isSchedulingPaused() {
		return this.isSchedulingPaused;
	}
	
	public static void stopAllMonitorThreads(boolean nicely) {
		if(!stopWasCalled || !nicely) {
			stopWasCalled = true;
			// avoid modification while iterating
			HashSet<MonitorThread<? extends Executor<?>>> copy = new HashSet<>();
			synchronized(CREATED_MONITOR_THREADS) { copy.addAll(CREATED_MONITOR_THREADS); }
			// remove all that threads
			for(MonitorThread<? extends Executor<?>> mt : copy) {
				if(nicely) mt.requestStop(5, TimeUnit.SECONDS);
				else mt.requestForcedStop();
			}
		}
	}
	
	@Override
	public void requestStop(long timeout, TimeUnit u) {
		LOGGER.debug("Stop for thread named '"+this.getName()+"' requested.");
		this.isDead = true;
		super.requestStop(timeout, u);
	}
	
	@Override
	public void requestForcedStop() {
		LOGGER.debug("Forced stop for thread named '"+this.getName()+"' detected!");
		this.isDead = true;
		super.requestForcedStop();
	}

	public static void setPauseSchedulingOnAllMonitorThreads(boolean pause) {
		synchronized(CREATED_MONITOR_THREADS) { 
			for(MonitorThread<? extends Executor<?>> mt : CREATED_MONITOR_THREADS) {
				mt.setPauseScheduling(pause);
			}
		}
	}
	
	public static void setDetachModeOnAllMonitorThreads(boolean isDetachMode) {
		if(isDetachMode) {
			LOGGER.info("Detach of Watchdog was requested!");
		}
		MonitorThread.WAS_DETACH_MODE_SET = isDetachMode;
		synchronized(CREATED_MONITOR_THREADS) { 
			for(MonitorThread<? extends Executor<?>> mt : CREATED_MONITOR_THREADS) {
				mt.setDetachMode(isDetachMode);
			}
		}
	}
	
	public static boolean wasDetachModeOnAllMonitorThreads() {
		return MonitorThread.WAS_DETACH_MODE_SET;
	}
	
	private void setDetachMode(boolean isDetachMode) {
		this.detachMode = isDetachMode;
	}
	
	/**
	 * true, if watchdog is running in detach / attach mode
	 * @return
	 */
	public boolean isInDetachMode() {
		return this.detachMode;
	}
	

	public boolean isDead() {
		return this.isDead;
	}
	
	/**
	 * 
	 * @return
	 */
	protected synchronized LinkedHashMap<String, E> getMonitorTasks() {
		LinkedHashMap<String, E> ret = new LinkedHashMap<>();
		for(String s : this.MONITOR_TASKS.keySet()) {
			E e = this.MONITOR_TASKS.get(s);
			if(!e.getTask().isTaskIgnored())
				ret.put(s, e);
		}
		return ret;
	}
	
	/**
	 * removes a task from being monitored
	 * @param id
	 */
	protected synchronized void remove(String id) {
		this.MONITOR_TASKS.remove(id);
	}
	
	/**
	 * returns the type of the monitor
	 * @return
	 */
	public abstract String getType();
	

	@Override
	public int executeLoop() throws InterruptedException {
		this.monitorJobs();
		return 1;
	}
	
	@Override
	public void beforeLoop() {
		
	}
	
	@Override
	public void afterLoop() {
		// for resume mode, we want to save the running task ids and don't terminate all running processes		
		// stop all other running tasks
		for(String key : this.MONITOR_TASKS.keySet()) {
			E ex = this.MONITOR_TASKS.get(key);
			// get info for detach
			if(this.isInDetachMode() && ex.EXEC_INFO.isWatchdogDetachSupported()) {
				Task t = ex.getTask();
				t.setExternalExecutorID(key);
				DETACH_INFO.add(t);
			}
			 // stop execution if not detach mode
			else {
				ex.stopExecution();
			}
		}
		// do some cleaning
		synchronized(CREATED_MONITOR_THREADS) { CREATED_MONITOR_THREADS.remove(this); }
	}
	
	/**
	 * test, if some monitor threads are still running
	 * @return
	 */
	public static boolean hasRunningMonitorThreads() {
		if(CREATED_MONITOR_THREADS.size() > 0) {
			synchronized(CREATED_MONITOR_THREADS) { 
				for(MonitorThread<?> t : CREATED_MONITOR_THREADS) {
					// if wasThreadStartedOnce() is true, afterLoop was not run as otherwise the entry wouldn't be in the list!
					if(t.isAlive() || t.wasThreadStartedOnce()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static ArrayList<Task> getMappingsForDetachableTasksFromAllMonitorThreads() {
		return DETACH_INFO;
	}
	
	/**
	 * adds a task to be monitored
	 * @param id
	 * @param executor
	 */
	public synchronized void addTaskToMonitor(String id, E executor) {
		this.MONITOR_TASKS.put(id, executor);
	}
	
	/**
	 * monitors the jobs
	 * @return
	 */
	protected synchronized boolean monitorJobs() {
	    for(Iterator<E> iter = this.MONITOR_TASKS.values().iterator(); iter.hasNext();) {
	    	Executor<?> e = iter.next();
			Task t = e.getTask();
			if(t.getExecutionCounter() > 0 && !t.hasJobInfo()) {
				// check, if the task should be terminated
				if(t.isTerminationPending()) {
					e.stopExecution();
					t.setStatus(TaskStatus.TERMINATED);
					iter.remove();
					continue;
				}
			}
		}
		return true;
	}
}
