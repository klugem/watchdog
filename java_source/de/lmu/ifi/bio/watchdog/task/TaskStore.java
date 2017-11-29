package de.lmu.ifi.bio.watchdog.task;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores some info of tasks (exported from Tasks in order not to send this stuff over network)
 * not a pretty design but a working one
 * @author kluge
 *
 */
public class TaskStore {
	private static final Map<Integer, Integer> GLOBAL_TASK_IDS = new ConcurrentHashMap<>();
	private static final Map<String, Task> TASKS = new ConcurrentHashMap<>(); 
	private static final Map<Integer, ArrayList<String>> CACHE = new ConcurrentHashMap<>();
	
	/**
	 * cleans all the data stored in this class
	 */
	public static void clean() {
		TASKS.clear();
		CACHE.clear();
		GLOBAL_TASK_IDS.clear();
	}
	
	public synchronized static void addTask(Task task) {
		TASKS.put(task.getID(), task);
	}

	public synchronized static void globalPut(int taskID, int subTaskID) {
		GLOBAL_TASK_IDS.put(taskID, subTaskID);
	}

	public synchronized static int globalGet(int taskID) {
		return GLOBAL_TASK_IDS.get(taskID);
	}

	public synchronized static boolean globalContainsKey(int taskID) {
		return GLOBAL_TASK_IDS.containsKey(taskID);
	}

	public synchronized static boolean cacheContainsKey(int hashCode) {
		return CACHE.containsKey(hashCode);
	}

	public synchronized static ArrayList<String> cacheGet(int hashCode) {
		return CACHE.get(hashCode);
	}

	public synchronized static void cachePut(int hashCode, ArrayList<String> ret) {
		CACHE.put(hashCode, ret);
	}

	public synchronized static void taskRemove(String id) {
		TASKS.remove(id);
	}
	
	public synchronized static Task taskGet(String id) {
		return TASKS.get(id);
	}

	public synchronized static boolean taskContainsKey(String taskID) {
		return TASKS.containsKey(taskID);
	}

	public synchronized static void taskPut(String id, Task task) {
		TASKS.put(id, task);
	}

	public synchronized static Map<String, Task> getTasksToRead() {
		return TASKS;
	}
	
	public static int getNumberOfUnfinishedTasks() {
		int un = 0;
		for(Task t : TASKS.values()) {
			if(!t.hasTaskFinishedWithoutBlockingInfo() && t.getExecutionCounter() > 0) 
				un++;
		}
		return un;
	}
}
