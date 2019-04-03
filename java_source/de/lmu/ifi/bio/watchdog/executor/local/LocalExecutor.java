package de.lmu.ifi.bio.watchdog.executor.local;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.ggf.drmaa.JobInfo;

import de.lmu.ifi.bio.watchdog.executor.ScheduledExecutor;
import de.lmu.ifi.bio.watchdog.executor.WatchdogThread;
import de.lmu.ifi.bio.watchdog.executor.remote.RemoteJobInfo;
import de.lmu.ifi.bio.watchdog.helper.SyncronizedLineWriter;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.task.TaskActionTime;
import de.lmu.ifi.bio.watchdog.task.TaskStatus;

/**
 * Can execute a task on the local host
 * @author Michael Kluge
 *
 */
public class LocalExecutor extends ScheduledExecutor<LocalExecutorInfo> {
	
	private static final String EXEC_NAME = "localhost";
	private static final Logger LOGGER = new Logger(LogLevel.INFO);
	private static LocalMonitorThread LOCAL_MONITOR = new LocalMonitorThread(); // not final any more caused by GUI
	private static int count_id = 0;
	private Process p;
	private BufferedInputStream inFile;
	private BufferedOutputStream outFile;
	private BufferedOutputStream errFile;
	private boolean wasStdinCompletelyWritten;

	/**
	 * Constructor
	 * @param task
	 * @param simulate
	 * @param execInfo
	 */
	public LocalExecutor(Task task, SyncronizedLineWriter log, LocalExecutorInfo execInfo) {
		super(task, log, execInfo);
		count_id++;
	}
	
	@Override
	public void execute() {
		// ignore tasks that should be executed on a slave
		if(this.TASK.willRunOnSlave())
			return;

		// check, if the local monitor thread is running and start it, if it is not
		if(LOCAL_MONITOR.isDead())
			LOCAL_MONITOR = new LocalMonitorThread();
		if(!LOCAL_MONITOR.isAlive() && !LOCAL_MONITOR.wasThreadStartedOnce()) {
			// add thread to run pool
			WatchdogThread.addUpdateThreadtoQue(LOCAL_MONITOR, true, true);
		}
		
		// add job to local monitor
		LOCAL_MONITOR.addTaskToMonitor(Integer.toString(count_id), this);
	}
		
	/**
	 * runs the actual command once the process is released
	 */
	@SuppressWarnings("unchecked")
	public void runCommand() {
		if(!this.wasProcessStarted()) {
			if(!this.TASK.performAction(TaskActionTime.BEFORE)) { 
				this.TASK.setJobInfo(new RemoteJobInfo(-1, true, true));
				return;
			}
		
			// ensure that all is ok with log file streams
			super.runCommand();
			this.outFile = new BufferedOutputStream(this.fos);
			this.errFile = new BufferedOutputStream(this.fer);
			
			LOGGER.info("Running '" + this.TASK.getBinaryCall() + "' with following arguments:");
			LOGGER.info(StringUtils.join(this.TASK.getArguments(), " "));
			try {
				// create folders, if they do not exist
				this.getWorkingDir(true);
				this.TASK.getStdOut(true);
				this.TASK.getStdErr(true);
								
				// try to set the hostname
				try { this.TASK.setHostname(InetAddress.getLocalHost().getHostName()); } 
				catch (UnknownHostException e) {}
							
				// run the command
				this.TASK.setStatus(TaskStatus.RUNNING);
				this.LOG.writeLog(this.TASK.getBinaryCall() + " " + StringUtils.join(this.TASK.getArguments()), this.getType(), this.TASK.getID(), EXECUTE);

				ProcessBuilder pb = new ProcessBuilder(this.getFinalCommand(true, false));
				// set the environment variables that should not be set by an external command
				if(this.hasInternalEnvVars())
					pb.environment().putAll(this.getInternalEnvVars());
				pb.directory(new File(this.getWorkingDir(true)));
				this.p = pb.start();
				
				if(this.TASK.getStdIn() != null)
					this.inFile = new BufferedInputStream(new FileInputStream(this.TASK.getStdIn()));
				else
					this.wasStdinCompletelyWritten = true;
		
				if(this.TASK.getStdOut(true) != null) {
					File fo = this.TASK.getStdOut(true);
					this.fos = new FileOutputStream(fo, this.TASK.isOutputAppended());
					// create the file if it is not there
					if(!fo.exists())
						fo.createNewFile();
					this.outFile = new BufferedOutputStream(this.fos);
				}
				if(this.TASK.getStdErr(true) != null) {
					File fe = this.TASK.getStdErr(true);
					this.fer = new FileOutputStream(fe, this.TASK.isErrorAppended());
					// create the file if it is not there
					if(!fe.exists())
						fe.createNewFile();
					this.errFile = new BufferedOutputStream(this.fer);
				}
			}
			catch(Exception e) {
				e.printStackTrace();
				this.TASK.addError(e.getMessage());
				this.TASK.setJobInfo(this.getJobInfo());
				// print error messages
				LOGGER.error("Failed to spawn new local process!");
				LOGGER.error("Command: '" + this.TASK.getBinaryCall() + "' with following arguments.");
				LOGGER.error(StringUtils.join(this.TASK.getArguments(), " "));
			}
		}
	}
	
	/**
	 * true, if process was started
	 * @return
	 */
	public boolean wasProcessStarted() {
		return this.p != null;
	}

	/**
	 * true, if process is still running
	 * @return
	 */
	public boolean isProcessStillRuning() {
		return this.p != null && this.p.isAlive();
	}
	
	
	/**
	 * get the stream which is used as stdin file
	 * @return
	 */
	public BufferedInputStream getStdinFile() {
		return this.inFile;
	}
	
	/**
	 * get the stream to the stdout file
	 * @return
	 */
	public BufferedOutputStream getStdoutFile() {
		return this.outFile;
	}
	
	/**
	 * get the stream to the stderr file
	 * @return
	 */
	public BufferedOutputStream getStderrFile() {
		return this.errFile;
	}
	
	/**
	 * gets the stdout stream or null if no process is running
	 * @return
	 */
	public InputStream getStdoutStream() {
		if(this.p == null)
			return null;
		return this.p.getInputStream();
	}
	 
	/**
	 * gets the stderr stream or null if no process is running
	 * @return
	 */
	public InputStream getStderrStream() {
		if(this.p == null)
			return null;
		return this.p.getErrorStream();
	}
	
	/**
	 * gets the stdin stream or null if no process is running
	 * @return
	 */
	public OutputStream getStdinStream() {
		if(this.p == null)
			return null;
		return this.p.getOutputStream();
	}
	
	/**
	 * exit code of that process or null if process is still running
	 * @return
	 */
	public Integer getExitCode() {
		if(this.isProcessStillRuning())
			return null;
		return this.p.exitValue();
	}

	@Override
	public void stopExecution() {
		try {
			this.p.getInputStream().close();
			this.p.getOutputStream().close();
			this.p.getErrorStream().close();
			this.p.waitFor(5, TimeUnit.SECONDS);
			this.p.destroy();
			
			// remove it from the running job list
			super.stopExecution();
		}
		catch(Exception e) { }
		finally {
			this.p = null;
		}
	}

	/**
	 * creates a job info object based on the process of this executor
	 * return value is null, if not valid at the calling time
	 * @return
	 */
	public JobInfo getJobInfo() {
		if(this.p != null && this.p.isAlive())
			return null;
		
		return new LocalJobInfo(this.p);
	}
	
	/**
	 * sets the stdin flag completely written to true
	 */
	public void setStdinWasCompletelyWritten() {
		this.wasStdinCompletelyWritten = true;
	}

	/**
	 * true, if the stdin file was completely written to to process
	 * @return
	 */
	public boolean wasStdinCompletelyWritten() {
		return this.wasStdinCompletelyWritten;
	}
	
	@Override
	public String getID() {
		try {
			return this.getType() + " (" + InetAddress.getLocalHost().getHostName() + ")";
		}
		catch(Exception e) {
			return this.getType();
		}
	}

	@Override
	public String getType() {
		return EXEC_NAME;
	}
}