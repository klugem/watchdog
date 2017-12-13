package de.lmu.ifi.bio.watchdog.slave;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.network.exception.ConnectionNotReady;
import de.lmu.ifi.bio.network.server.Server;
import de.lmu.ifi.bio.network.server.ServerConnectionHandler;
import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.executor.WatchdogThread;
import de.lmu.ifi.bio.watchdog.helper.ActionType;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.optionFormat.OptionFormat;
import de.lmu.ifi.bio.watchdog.optionFormat.ParamFormat;
import de.lmu.ifi.bio.watchdog.optionFormat.QuoteFormat;
import de.lmu.ifi.bio.watchdog.optionFormat.SpacingFormat;
import de.lmu.ifi.bio.watchdog.slave.clientEvents.IdentifyEventEH;
import de.lmu.ifi.bio.watchdog.slave.clientEvents.KillEvent;
import de.lmu.ifi.bio.watchdog.slave.clientEvents.StatusUpdateEventEH;
import de.lmu.ifi.bio.watchdog.slave.clientEvents.TaskExecutorEvent;
import de.lmu.ifi.bio.watchdog.slave.clientEvents.TaskFinishedEventEH;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.task.TaskStatus;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask;

/**
 * Master host for Watchdog's slave mode
 * @author Michael Kluge
 *
 */
public class Master extends Server {
	public static final int RUN_ID = Math.abs(new Random().nextInt());
	public static final String SLAVE_JAR = "jars" + File.separator + "watchdogSlave.jar";
	private static Master master;
	private static String host;
	private static int slaves = -1;
	private final ConcurrentHashMap<String, ServerConnectionHandler> SLAVES = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, LinkedHashMap<Task, LinkedHashSet<Integer>>> WAITING = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, XMLTask> XML = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, String> TASK2SLAVE = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Boolean> PENDING_SLAVE = new ConcurrentHashMap<String, Boolean>(); // hashset would be sufficient but is not implemented in java...
	private static final String SLAVE = "slave_";
	public static final long SLAVE_KILL_WAIT_TIME = 20000; // give the slave 20 sec. until it will be killed
	private static int slaveCounter = 1;
	private EndOfLifeChecker EOLC;
	private static final int NUMBER_OF_WORKING_THREADS = 4;
	
	/**
	 * Constructor, that is internally called, only when needed
	 * @param port
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	private Master(int port) throws IllegalArgumentException, IOException {
		super(port, NUMBER_OF_WORKING_THREADS);
	}
	
	@Override
	public int executeLoop() {
		// start the EOL checker
		if(this.EOLC == null) {
			this.EOLC = new EndOfLifeChecker();
			WatchdogThread.addUpdateThreadtoQue(this.EOLC, true);
		}
		super.executeLoop();
		return 1;
	}
	
	/**
	 * returns a so far unused slave id
	 * @return
	 */
	public synchronized static String getNewSlaveID() {
		return SLAVE + Master.slaveCounter++;
	}
	
	public static void stopServer() {
		if(Master.master != null) {
			Master.master.EOLC = null;
			Master.master.requestStop(5, TimeUnit.SECONDS);
			Master.master = null;
		}
	}

	/**
	 * Adds a new slave
	 * @param id
	 * @param t
	 * @param exec
	 * @param env
	 * @param depToKeep 
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 */
	public synchronized static XMLTask addSlave(String id, Task t, File watchdogBase, ExecutorInfo exec, Environment env, LinkedHashSet<Integer> depToKeep) throws IllegalArgumentException, IOException {
		// check if master server is running
		if(Master.master == null) {
			// start a new master at a random port
			Master.master = new Master(0);
			Master.host = InetAddress.getLocalHost().getHostName();
			WatchdogThread.addUpdateThreadtoQue(Master.master, true); // start the master
		}
		// mark the task to be on the queue
		t.setStatus(TaskStatus.WAITING_QUEUE);

		// check if a slave with that ID is already running
		if(!(Master.master.SLAVES.containsKey(id) || Master.master.PENDING_SLAVE.contains(id))) {
			// get name for new slave task!
			String taskName = "slave executor";
			
			if(t.isSingleSlaveModeForced()) {
				taskName = t.getName();
			}

			// start the new slave
			XMLTask slave = new XMLTask(Master.slaves--, taskName, exec.getPath2Java() + " -Xms256m -Xmx256m", taskName, "", new OptionFormat(ParamFormat.shortOnly, QuoteFormat.unquoted, SpacingFormat.blankSeperated, null), exec, env);
			//slave.addParameter("Xms", "256M", new OptionFormat(ParamFormat.shortOnly, QuoteFormat.unquoted, SpacingFormat.notSeparated), -1);
			//slave.addParameter("Xmx", "256M", new OptionFormat(ParamFormat.shortOnly, QuoteFormat.unquoted, SpacingFormat.notSeparated), -1); // TODO
			slave.addParameter("jar", watchdogBase.getAbsolutePath() + File.separator + SLAVE_JAR, null, -1);
			slave.addParameter("host", Master.host, null, -1);
			slave.addParameter("p", Integer.toString(Master.master.getPort()), null, -1);
			slave.addParameter("b", watchdogBase.getAbsolutePath(), null, -1);
			slave.addParameter("m", Integer.toString((exec.getMaxSlaveRunningTasks() == null ? 1 : exec.getMaxSlaveRunningTasks())), null, -1);
			slave.addParameter("i", id, new OptionFormat(ParamFormat.shortOnly, QuoteFormat.doubleQuoted, SpacingFormat.blankSeperated, null), -1);
			
			slave.setErrorStream(watchdogBase + File.separator + "tmp" + File.separator + slave + "." + RUN_ID + "." + id + ".err", false);
			slave.setOutputStream(watchdogBase + File.separator + "tmp" + File.separator + slave + "." + RUN_ID + "." + id + ".out", false);
			slave.setMaxRunning(3);
			slave.setNotify(ActionType.DISABLED);
			slave.setCheckpoint(ActionType.DISABLED);
			slave.setConfirmParam(ActionType.DISABLED);
			
			// save that task that it should be executed afterwards
			LinkedHashMap<Task, LinkedHashSet<Integer>> tasks = new LinkedHashMap<>();
			tasks.put(t, depToKeep);
			Master.master.WAITING.put(id, tasks);
			Master.master.XML.put(id, slave);
			Master.master.PENDING_SLAVE.put(id, false);
			return slave;
		}
		else {
			try {
				// check, if slave if running
				if(Master.master.SLAVES.containsKey(id)) {
					Master.registerTask(t.getID(), id);
					Master.master.SLAVES.get(id).send(new TaskExecutorEvent(t, depToKeep));
					System.out.println("task was sent to host " + t.getID());
				}
				// slave is pending...
				else {
					Master.master.WAITING.get(id).put(t, depToKeep);
				}
			} catch (ConnectionNotReady e) {
				// connection to slave failed
				System.err.println("Failed to send new job to slave with id '"+id+"'.");
				e.printStackTrace();
				System.exit(1);
			}
			return null;
		}
	}
	
	@Override
	public void register(ServerConnectionHandler c) {
		// register the status update event handler
		c.registerEventHandler(new StatusUpdateEventEH());
		
		// register the ID event handler
		c.registerEventHandler(new IdentifyEventEH(this.SLAVES, this.WAITING, this.PENDING_SLAVE));
		
		// register the status update event handler
		c.registerEventHandler(new TaskFinishedEventEH());
	}
	
	/**
	 * sends the termination event to the slave with that ID
	 * @param id
	 */
	public static void killSlave(String id) {
		try {
			// try to find the correct connection
			if(Master.master.SLAVES.containsKey(id)) {
				// delete all tasks running on that slave
				for(String taskID : Master.master.TASK2SLAVE.keySet()) {
					if(Master.master.TASK2SLAVE.get(taskID) == id)  {
						Task t = Task.getTask(taskID);
						XMLTask.deleteSlaveID(t);
						Master.unregisterTask(t);
						// terminate tasks that are running
						if(t != null && t.isTaskRunning())
							t.terminateTask();
					}
				}
				Master.master.SLAVES.get(id).send(new KillEvent());
				// end the connection afterwards
				Master.master.SLAVES.get(id).disconnect();
			}
		}
		// something went wrong --> remove it in any case because connection is corrupted
		catch(Exception e) {}
		Master.master.SLAVES.remove(id);
		
		Master.master.XML.remove(id);
		Master.master.PENDING_SLAVE.remove(id);
	}

	/**
	 * registers a task at a slave
	 * @param taskId
	 * @param slaveID
	 */
	public synchronized static void registerTask(String taskID, String slaveID) {
		Master.master.TASK2SLAVE.put(taskID, slaveID);
	}
	
	/**
	 * unregister a task from a slave
	 * @param t
	 */
	public synchronized static void unregisterTask(Task task) {
		if(Master.master != null) {
			String sID = Master.master.TASK2SLAVE.remove(task.getID());
			// check, if the slave is needed anymore!
			if(sID != null)
				Master.master.EOLC.addSlaveID2Check(sID, task.isSingleSlaveModeForced());
		}
	}
	 
	/**
	 * retuns the slave that is processing the task with that ID or null if it is not registered
	 * @param taskID
	 * @return
	 */
	public static ServerConnectionHandler getExecutingSlave(String taskID) {
		if(Master.master == null)
			return null;
		
		if(Master.master.TASK2SLAVE.containsKey(taskID))
			return Master.master.SLAVES.get(Master.master.TASK2SLAVE.get(taskID));
		else
			return null;
	}
	
	
	/**
	 * tests, if a slave with that ID is needed for any more tasks that had not been finished yet or will be spawned later
	 * @param slaveID
	 * @return
	 */
	public static boolean isSlaveNeededAnyMore(String slaveID) {
		// check, if there are any jobs left with that slave ID
		for(XMLTask x : XMLTask.getXMLTasks().values()) {
			if(x.getSlaveIDS().values().contains(slaveID))
				return true;
		}
		return false;
	}
	
	@Override
	public void afterLoop() {
		super.afterLoop();
		// ensure that the server is gone!
		Master.master = null;
	}

}
