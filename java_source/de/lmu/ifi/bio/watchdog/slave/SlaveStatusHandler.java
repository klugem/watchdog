package de.lmu.ifi.bio.watchdog.slave;

import java.io.File;
import java.io.Serializable;
import java.net.InetAddress;

import org.ggf.drmaa.JobInfo;

import de.lmu.ifi.bio.network.client.Client;
import de.lmu.ifi.bio.network.exception.ConnectionNotReady;
import de.lmu.ifi.bio.watchdog.helper.SerJobInfo;
import de.lmu.ifi.bio.watchdog.helper.TransferFile;
import de.lmu.ifi.bio.watchdog.slave.clientEvents.StatusUpdateEvent;
import de.lmu.ifi.bio.watchdog.slave.clientEvents.TaskFinishedEvent;
import de.lmu.ifi.bio.watchdog.task.StatusHandler;
import de.lmu.ifi.bio.watchdog.task.Task;

/**
 * Sends updates of the task status back to the master
 * @author Michael Kluge
 *
 */
public class SlaveStatusHandler implements StatusHandler, Serializable {
	private static final long serialVersionUID = 1666685398082850204L;
	private static Client client;
	public static StatusHandler handler;
	private final String HOST;
	
	/**
	 * Constructor
	 * @param host
	 */
	public SlaveStatusHandler(String host) {
		this.HOST = host;
	}
	
	/**
	 * Init the status handler that it can use the network connection
	 * @param c
	 */
	public static void initStatusHandler(Client c) {
		SlaveStatusHandler.client = c;
		String host = null;
		try {
			host = InetAddress.getLocalHost().getHostName();
		}
		catch(Exception e) {}
		SlaveStatusHandler.handler = new SlaveStatusHandler(host);
	}

	/**
	 * sends a new event to the server
	 * @param task
	 * @throws ConnectionNotReady 
	 */
	public void handle(Task task) {
		try {
			// copy log files to the master and send him the job info
			if(!task.getStatus().isStatusCheck()) { // wait until status check if finished!
				if(task.hasJobInfo()) { 
					File err = TransferFile.copyLogFileToTmpBase(task.getStdErr(false), true, task.isErrorAppended());
					File out = TransferFile.copyLogFileToTmpBase(task.getStdOut(false), false, task.isOutputAppended());
					JobInfo info = new SerJobInfo(task.getJobInfo());
					SlaveStatusHandler.client.getEventSocket().send(new TaskFinishedEvent(task, task.getID(), task.getSlaveID(), info, err, out, task.getErrors()));
				}
				// send status updates to master
				else {
					SlaveStatusHandler.client.getEventSocket().send(new StatusUpdateEvent(task.getID(), task.getStatus(), this.HOST));	
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			System.err.println("Status handler was not able to send status update of task with ID '"+task.getTaskID()+"' to master.");
		}
	}
}
