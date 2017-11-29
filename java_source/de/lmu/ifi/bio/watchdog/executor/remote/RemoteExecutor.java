package de.lmu.ifi.bio.watchdog.executor.remote;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;

import de.lmu.ifi.bio.watchdog.executor.ScheduledExecutor;
import de.lmu.ifi.bio.watchdog.helper.SyncronizedLineWriter;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.task.TaskStatus;

/**
 * Executes a command via ssh on a remote host
 * @author Michael Kluge
 *
 */
public class RemoteExecutor extends ScheduledExecutor<RemoteExecutorInfo> {

	private static final String CD = "cd ";
	private static final String EXEC_NAME = "remote";
	private static final Logger LOGGER = new Logger(LogLevel.DEBUG);
	private static RemoteMonitorThread REMOTE_MONITOR = new RemoteMonitorThread();
	private static final String EXEC = "exec";
	private Session session;
	private ChannelExec channel;
	private static final String DEV_NULL;
	private static int count_id = 0;
	
	// detect /dev/null
	static {
		String os = System.getProperty("os.name").toLowerCase();
		if(os.contains("win"))
			DEV_NULL = "NUL";
		else
			DEV_NULL = "/dev/null";	
	}
	
	/**
	 * Constructor
	 * @param task
	 * @param log
	 * @param auth
	 * @param execinfo
	 */
	public RemoteExecutor(Task task, SyncronizedLineWriter log, RemoteExecutorInfo execinfo) {
		super(task, log, execinfo);
		count_id++;
	}

	@Override
	public void execute() {	
		// ignore tasks that should be executed on a slave
		if(this.TASK.willRunOnSlave())
			return;
		
		if(REMOTE_MONITOR.isDead())
			REMOTE_MONITOR = new RemoteMonitorThread();
		// check, if the local monitor thread is running and start it, if it is not
		if(!REMOTE_MONITOR.isAlive()) {
			REMOTE_MONITOR.start();
		}
		
		// add job to remote monitor
		REMOTE_MONITOR.addTaskToMonitor(Integer.toString(count_id), this);
	}
	
	/**
	 * Casting of the executor info
	 * @return
	 */
	private RemoteExecutorInfo getExecInfo() {
		return (RemoteExecutorInfo) this.EXEC_INFO;
	}

	@Override
	public void runCommand() {
		super.runCommand();
		
		// select a free host
		boolean first = true;
		while(first || (!this.getExecInfo().hasFreeHost())) {
			this.session = this.EXEC_INFO.getAuth().getSSHSession(this.getExecInfo().getUser(), this.getExecInfo().getFreeHost(), this.getExecInfo().getPort(), this.getExecInfo().isStrictHostCheckingEnabled());
			// wait until next try
			if(first)
				try { Thread.sleep(1000); } catch(Exception e) {}
			first = false;
		}
		
		String host = this.session.getHost();
		// try to connect to host
		try {
			this.session.connect();
			LOGGER.info("Established remote connetion to host '"+host+"'.");
		}
		catch(Exception e) {
			e.printStackTrace();
			LOGGER.error("Could not connect to host '" + host + "' with user '" + this.session.getUserName() + "' on port '" + this.session.getPort() + "' .");
			this.getTask().setJobInfo(new RemoteJobInfo(1, true, true));
			// do not use this host for further tasks
			this.getExecInfo().removeHost(host);
			return;
		}
		// try to execute the command
		try {
			// check, if working directory should be changed
			if(this.getWorkingDir(false) == null)
				this.addPreCommand(CD + this.getWorkingDir(false));
			
			this.getExecInfo().increaseRunningCounter(host);
			this.channel = (ChannelExec) this.session.openChannel(EXEC);
			this.channel.setPty(true);
			this.channel.setCommand(this.getFinalJoinedCommand()); // set remote command
			// send env variables
			HashMap<String, String> env = this.getEnvironmentVariables();
			for(String name : env.keySet()) {
				this.channel.setEnv(name, env.get(name));
			}
			
			// set stdout
			if(this.TASK.getStdOut(false) != null) {
				this.fos = new FileOutputStream(this.TASK.getStdOut(false), this.TASK.isOutputAppended());
				this.channel.setOutputStream(new BufferedOutputStream(this.fos));
			}
			else
				this.channel.setOutputStream(new BufferedOutputStream(new FileOutputStream(DEV_NULL)));
			// set stderr
			if(this.TASK.getStdErr(false) != null) {
				this.fer = new FileOutputStream(this.TASK.getStdErr(false), this.TASK.isErrorAppended());
				this.channel.setErrStream(new BufferedOutputStream(this.fer));
			}
			else
				this.channel.setErrStream(new BufferedOutputStream(new FileOutputStream(DEV_NULL)));
			// set stdin
			if(this.TASK.getStdIn() != null)
				this.channel.setInputStream(new FileInputStream(this.TASK.getStdIn()));
						
			// execute the command
			this.TASK.setHostname(host);
			this.TASK.setStatus(TaskStatus.RUNNING);
			this.channel.connect();
		}
		catch(Exception e) {
			// ensure that connection is closed
			try {
				if(this.channel != null)
					this.channel.disconnect();
				
				this.session.disconnect();
			} catch(Exception ee) {}
			this.TASK.addError(e.getMessage());
			this.getTask().setJobInfo(new RemoteJobInfo(1, true, true));
			this.getExecInfo().decreaseRunningCounter(host);
			e.printStackTrace();
			LOGGER.error("Failed to execute the command on the remote host.");
		}
	}
	
	/**
	 * send the signal, that processing was ended because input stream contains no more messages
	 */
	protected void signalEndOfProcessing() {
		// create status object and set it
		this.getTask().setJobInfo(new RemoteJobInfo(this.channel.getExitStatus(), true, false));
		// end connection
		this.stopExecution();
	}
	
	@Override
	public void stopExecution() {
		if(this.session.isConnected()) {
			try { 
				this.fos.write(3);
				this.fos.flush();
				this.channel.disconnect();
				this.session.disconnect(); 
				} catch(Exception e) { e.printStackTrace();}
				finally {
					this.getExecInfo().decreaseRunningCounter(this.session.getHost());
					this.channel = null;
					this.session = null;
				}
		}
		super.stopExecution();
	}

	@Override
	public String getType() {
		return EXEC_NAME;
	}

	@Override
	public String getID() {
		return this.getType() + (this.session != null ? " (" + this.session.getHost() + ")" : "");
	}

	/**
	 * 
	 * @return
	 */
	protected InputStream getInputStream() {
		try { return this.channel.getInputStream(); } catch(Exception e) {}
		return null;
	}
	
	/**
	 * 
	 * @return
	 */
	protected boolean isEOFReached() {
		if(this.channel != null )
			return this.channel.isEOF();
		return false;
	}
}
