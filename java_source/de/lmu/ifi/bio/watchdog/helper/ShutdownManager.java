package de.lmu.ifi.bio.watchdog.helper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import de.lmu.ifi.bio.multithreading.MonitorRunnable;
import de.lmu.ifi.bio.watchdog.executor.Executor;
import de.lmu.ifi.bio.watchdog.executor.MonitorThread;
import de.lmu.ifi.bio.watchdog.task.Task;

/**
 * Shutdowns watchdog and ends all tasks, which are currently executed on executors
 * @author Michael Kluge
 *
 */
public class ShutdownManager extends MonitorRunnable {
	private final Set<Task> TASKS;
	private boolean wereTasksKilled = false;
	private static long OBSERVER_WAIT = 0;
	private static final ArrayList<MonitorThread<Executor<?>>> MONITORS = new ArrayList<>();
	
	public ShutdownManager(Set<Task> tasks) {
		super("ShutdownManager");
		this.TASKS = tasks;
	}
	
	/**
	 * registers a monitor
	 * @param monitor
	 */
	public static void register(MonitorThread<Executor<?>> monitor) {
		MONITORS.add(monitor);
		OBSERVER_WAIT = Math.max(OBSERVER_WAIT, monitor.getDefaultWaitTime());
	}
	
	@Override
	public void run() {
		super.run();
		if(!this.wereTasksKilled) {
			// end all the tasks which are currently running
			LinkedHashSet<Task> copy;
			synchronized(this.TASKS) { copy = new LinkedHashSet<>(this.TASKS); }
			for(Task t : copy) {
				t.destroy();
				this.TASKS.remove(t);
			}
			// give the observer some time to do their job
			try { Thread.sleep(OBSERVER_WAIT); } catch(Exception e) {}
			
			// stop the thread
			for(MonitorThread<Executor<?>> m : MONITORS) {
				m.interrupt();
			}
		}
		this.wereTasksKilled = true;
	}
	
	@Override
	public boolean canBeStoppedForDetach() {
		return true;
	}
}
