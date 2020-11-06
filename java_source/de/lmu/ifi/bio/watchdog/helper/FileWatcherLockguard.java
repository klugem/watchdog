package de.lmu.ifi.bio.watchdog.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobInfo;

import de.lmu.ifi.bio.multithreading.StopableLoopRunnable;
import de.lmu.ifi.bio.watchdog.executor.WatchdogThread;
import de.lmu.ifi.bio.watchdog.resume.ResumeJobInfo;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.task.TaskStatusUpdate;


/**
 * I don't use WatchService as many different directories might be of interest --> many single threads --> quite much overhead
 * instead I try to implement that by myself
 * @author kluge
 *
 */
public class FileWatcherLockguard extends StopableLoopRunnable {
	private final Map<String, Long> INSERT_TIME = new ConcurrentHashMap<>();
	private final Map<Task, ArrayList<File>> MONITOR = new ConcurrentHashMap<>();
	
	private static FileWatcherLockguard lock = null;
	private static final int CREATE_DIFF_MILLI = 5000; // was not modified for at least 5 seconds
	private static final int WAIT_TIME_MILLI = 1000;
	private static final int MAX_WAIT_TIME_MILLI = 30 * 1000; // wait max. 30 seconds for a file to apear
	
	/**
	 * singleton class
	 */
	private FileWatcherLockguard() {
		super("FileWatcherLockguard");
	}

	@Override
	public int executeLoop() throws InterruptedException {
		// do not create any object if nothing needs to be done
		if(this.MONITOR.size() == 0)
			return 1;
		
		// loop over all files and test them
		String id;
		File f;
		ArrayList<File> a;
		Entry<Task, ArrayList<File>> e;
		long curTime = System.currentTimeMillis();
		for(Iterator<Entry<Task, ArrayList<File>>> it = this.MONITOR.entrySet().iterator(); it.hasNext(); ) {
			e = it.next();
			a = e.getValue();
			for(Iterator<File> itf = a.iterator(); itf.hasNext(); ) {
				f = itf.next();
				// test if file exists, can be read and was last changed after a specific time
				if(f.exists() && f.canRead()) {
					if(((curTime-f.lastModified()) >= FileWatcherLockguard.CREATE_DIFF_MILLI))
						itf.remove();
				}
				// test if max time is gone
				else {
					id = e.getKey().getID();
					if((curTime - this.INSERT_TIME.get(id)) >= FileWatcherLockguard.MAX_WAIT_TIME_MILLI) {
						// don't wait any longer for this task
						a.clear();
						LOGGER.warn("Maximal wait time for file '" +f.getAbsolutePath()+ "' expired for task with id '" +id+ "'.");
						break;
					}
				}
			}
			// process it and remove it from monitor list
			if(a.size() == 0) {
				this.performUpdate(e.getKey());
				it.remove();
				this.INSERT_TIME.remove(e.getKey().getID());
			}
		}
		return 1;
	}

	@Override
	public void afterLoop() {
		if(FileWatcherLockguard.lock != null) 
			this.MONITOR.clear();
		
		FileWatcherLockguard.lock = null;
	}

	@Override
	public long getDefaultWaitTime() {
		return FileWatcherLockguard.WAIT_TIME_MILLI;
	}
	
	@Override
	public boolean canBeStoppedForDetach() {
		return this.MONITOR.size() == 0;
	}
	
	/**
	 * adds a new task that should be monitored
	 * @param t
	 */
	private void addToMonitor(Task t, JobInfo status) { 
		// out and error streams don't have to be monitored if task was created in resume mode
		if(status instanceof ResumeJobInfo == false) {
			ArrayList<File> files = new ArrayList<>();
			if(t.getStdOut(false) != null) files.add(t.getStdOut(false));
			if(t.getStdErr(false) != null) files.add(t.getStdErr(false));
			
			int exitCode = 0;
			try { exitCode = t.getJobInfo().getExitStatus(); } catch(DrmaaException e) {}
			
			// if task has failed we don't enforce that
			if(exitCode == 0 && t.hasVersionQueryInfoFile())
				files.add(t.getVersionQueryInfoFile());
			
			// add it to monitor if required or perform update instantly if possible
			if(files.size() > 0) {
				this.MONITOR.put(t, files);
				this.INSERT_TIME.put(t.getID(), System.currentTimeMillis());
			}
			else
				this.performUpdate(t);
		}
		else
			this.performUpdate(t);
	}
		
	/**
	 * creates the taskStatus update object and submits it for scheduling
	 * @param t
	 */
	private void performUpdate(Task t) {
		WatchdogThread.addUpdateThreadtoQue(new TaskStatusUpdate(t), false);
	}
	
	/*********************** static methods ***********************/
	
	public static void addTask(Task t, JobInfo status) {
		// ensure that watcher is running
		if(FileWatcherLockguard.lock == null) {
			FileWatcherLockguard.lock = new FileWatcherLockguard();
			WatchdogThread.addUpdateThreadtoQue(FileWatcherLockguard.lock, true);
		}
		// add the task 
		FileWatcherLockguard.lock.addToMonitor(t, status);
	}

	@Override
	public void beforeLoop() {
	
	}
}
