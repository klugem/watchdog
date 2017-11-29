package de.lmu.ifi.bio.watchdog.executor;

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
	protected static final Logger LOGGER = new Logger(LogLevel.DEBUG);
	private final LinkedHashMap<String, E> MONITOR_TASKS = new LinkedHashMap<>();
	
	// used for pausing / resume of scheduling
	private static boolean stopWasCalled = false;
	private static final Set<MonitorThread<? extends Executor<?>>> CREATED_MONITOR_THREADS = Collections.synchronizedSet(new LinkedHashSet<MonitorThread<? extends Executor<?>>>());
	private boolean isSchedulingPaused = false;
	private boolean isDead = false;
	
	public MonitorThread(String name) {
		super(name);
		CREATED_MONITOR_THREADS.add(this);
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
			copy.addAll(CREATED_MONITOR_THREADS);
			// remove all that threads
			for(MonitorThread<? extends Executor<?>> mt : copy) {
				if(nicely) mt.requestStop(5, TimeUnit.SECONDS);
				else mt.requestForcedStop();
			}
			CREATED_MONITOR_THREADS.clear();
		}
	}
	
	@Override
	public void requestStop(long timeout, TimeUnit u) {
		CREATED_MONITOR_THREADS.remove(this);
		this.isDead = true;
		super.requestStop(timeout, u);
	}
	
	@Override
	public void requestForcedStop() {
		CREATED_MONITOR_THREADS.remove(this);
		this.isDead = true;
		super.requestForcedStop();
	}

	public static void setPauseSchedulingOnAllMonitorThreads(boolean pause) {
		for(MonitorThread<? extends Executor<?>> mt : CREATED_MONITOR_THREADS) {
			mt.setPauseScheduling(pause);
		}
	}
	
	public boolean isDead() {
		return this.isDead;
	}
	
	/**
	 * 
	 * @return
	 */
	protected LinkedHashMap<String, E> getMonitorTasks() {
		LinkedHashMap<String, E> ret = new LinkedHashMap<>();
		for(String s  : this.MONITOR_TASKS.keySet()) {
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
	protected void remove(String id) {
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
		// stop all running tasks
		for(E ex : this.MONITOR_TASKS.values())
			ex.stopExecution();
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
	protected boolean monitorJobs() {
	    for(Iterator<E> iter = this.MONITOR_TASKS.values().iterator(); iter.hasNext();) {
	    	Executor<?> e = iter.next();
			Task t = e.getTask();
			if(t.getExecutionCounter() > 0 && !t.hasJobInfo()) {
				// check, if the task should be terminated
				if(t.isTerminationPending()) {
					this.MONITOR_TASKS.remove(e);
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
