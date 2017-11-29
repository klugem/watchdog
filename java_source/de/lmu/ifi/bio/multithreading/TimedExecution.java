package de.lmu.ifi.bio.multithreading;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.lmu.ifi.bio.watchdog.helper.DefaultRunnableExecutor;
import de.lmu.ifi.bio.watchdog.interfaces.RunnableExecutor;

/**
 * Does execute runables after a specific time passed
 * [WARNING] Does not garantee that a runable is executed at a specific time (only that is executed afterwards)
 * Works most exact when only small tasks are used!
 * FOR short running update stuff only!
 * @author kluge
 *
 */
public class TimedExecution extends StopableLoopThread {
	
	private static RunnableExecutor rex = new DefaultRunnableExecutor();
	private long currentTime = -1;
	private final HashMap<Long, Runnable> JOBS = new HashMap<>();
	private static TimedExecution SINGLETON;
	private final Map<String, Long> NAMES_TO_TIME = Collections.synchronizedMap(new LinkedHashMap<String, Long>());
	private boolean dead = false;
	
	// create timed execution thread
	static {
		SINGLETON = new TimedExecution();
	}
	
	/**
	 * in order to avoid import from javafx.* packages in non GUI version
	 * @param ex
	 */
	public static void setRunnableExecutor(RunnableExecutor ex) {
		rex = ex;
	}
	
	private TimedExecution() {
		super("TimedExecution");
	}
	
	private static void startIt() {
		if(!SINGLETON.isAlive() && SINGLETON.isNotDead()) {
			SINGLETON.start();
			Runtime.getRuntime().addShutdownHook(new Thread(() -> stopNow()));
		}
	}
	
	/**
	 * can be used when only one instance should be there of a runnable
	 * @param r
	 * @param wait
	 * @param u
	 * @param name
	 */
	public static void addRunableNamed(Runnable r, long wait, TimeUnit u, String name) {
		// remove old job that was not executed yet
		if(SINGLETON.NAMES_TO_TIME.containsKey(name)) {
			long startAtOld = SINGLETON.NAMES_TO_TIME.get(name);
			SINGLETON.JOBS.remove(startAtOld);
		}
		
		long startAt = addRunable(r, wait, u);
		SINGLETON.NAMES_TO_TIME.put(name, startAt);
	}

	/**
	 * adds a new runnable that is executed after a specific time
	 * @param r
	 * @param wait
	 * @param u
	 */
	public static long addRunable(Runnable r, long wait, TimeUnit u) {
		startIt();
		long startAt = System.currentTimeMillis() + u.toMillis(wait);
		
		while(SINGLETON.JOBS.containsKey(startAt)) {
			startAt++;
		}
		SINGLETON.JOBS.put(startAt, r);
		return startAt;
	}

	@Override
	public int executeLoop() throws InterruptedException {
		// update the time
		this.currentTime = System.currentTimeMillis();
		
		for(Iterator<Long> it = this.JOBS.keySet().iterator(); it.hasNext(); ) {
			long l = it.next();
			// schedule job for execution
			if(l <= this.currentTime) {
				// if can not be added, start without pool
				Runnable r = this.JOBS.get(l);
				it.remove();
				rex.run(r);
			}
		}
		return 1;
	}

	@Override
	public void beforeLoop() {}

	@Override
	public void afterLoop() {
		// clean up
		this.JOBS.clear();
	}

	@Override
	public long getDefaultWaitTime() {
		return 25;
	}

	public static void stopNow() {
		if(SINGLETON != null && SINGLETON.isAlive()) {
			SINGLETON.afterLoop();
			SINGLETON.dead = true;
			SINGLETON.requestStop(1, TimeUnit.SECONDS);
			SINGLETON = null;
		}
	}
	
	private boolean isNotDead() {
		return !this.dead;
	}
}
