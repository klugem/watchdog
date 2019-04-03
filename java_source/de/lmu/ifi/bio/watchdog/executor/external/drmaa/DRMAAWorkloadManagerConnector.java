package de.lmu.ifi.bio.watchdog.executor.external.drmaa;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.apache.commons.lang3.RandomStringUtils;
import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.ExitTimeoutException;
import org.ggf.drmaa.InvalidJobException;
import org.ggf.drmaa.JobInfo;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;

import de.lmu.ifi.bio.watchdog.executor.external.ExternalScheduledExecutor;
import de.lmu.ifi.bio.watchdog.executor.external.ExternalWorkloadManagerConnector;
import de.lmu.ifi.bio.watchdog.executor.external.sge.SGEExecutor;
import de.lmu.ifi.bio.watchdog.helper.AbortedJobInfo;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.resume.AttachInfo;
import de.lmu.ifi.bio.watchdog.runner.XMLBasedWatchdogRunner;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.xmlParser.plugins.XMLParserPlugin;

public class DRMAAWorkloadManagerConnector extends ExternalWorkloadManagerConnector<DRMAAExecutor> {

	public static final String DRMAA_SESSION_NAME = "drmaa_session_name-ExternalScheduledExecutor";
	private static final String EXECUTOR_NAME = "cluster";
	private static final String SESSION = "session="; // required for contact string (at least for SGE DRMAA)
	private static final String WATCHDOG = "watchdog_drmaa_";
	private Session session;
	private String contactString = null;
	private boolean isInitComplete = false;
	
	public DRMAAWorkloadManagerConnector(Logger l) {
		super(l);
	}
	
	@Override
	public String submitJob(Task task, DRMAAExecutor executor) throws DrmaaException {
		JobTemplate job = this.getJobTemplate(task, executor);
		String id = this.session.runJob(job);
		this.deleteJobTemplate(job); // do some clean up
		return id;
	}
	
	@Override
	public void releaseJob(String id) throws DrmaaException {
		this.session.control(id, Session.RELEASE);
	}

	@Override
	public void holdJob(String id) throws DrmaaException {
		this.session.control(id, Session.HOLD);
	}

	@Override
	public void cancelJob(String id) throws DrmaaException {
		this.session.control(id, Session.TERMINATE);
	}

	@Override
	public boolean isJobRunning(String id) throws DrmaaException {
		return Session.RUNNING == this.session.getJobProgramStatus(id);
	}

	@Override
	public JobInfo getJobInfo(String id) throws DrmaaException {
		try { 
			JobInfo info = this.session.wait(id, Session.TIMEOUT_NO_WAIT);
			return info;
		} 
		catch(InvalidJobException e) { return new AbortedJobInfo(); }
		catch(ExitTimeoutException e) { }
		catch(Exception e) { e.printStackTrace(); }
		return null;
	}

	@Override
	public void init() {
		// check, if the a different "contact string" should be set
		String sessionName = null;
		if(AttachInfo.hasLoadedData(DRMAA_SESSION_NAME)) {
			sessionName = AttachInfo.getLoadedData(DRMAA_SESSION_NAME).toString();;
		}
		else {
			sessionName = WATCHDOG + RandomStringUtils.randomAlphanumeric(16);
		}
		
		// set the session name and make it persistent
		this.setContactString(sessionName);
		AttachInfo.setValue(DRMAA_SESSION_NAME, sessionName);
		
		// init the DRMAA grid with the default system
        try {
        	this.session = SessionFactory.getFactory().getSession();
        	this.session.init(this.contactString);
        	this.contactString = this.session.getContact();
        	this.LOGGER.info("Used DRMAA version: " + this.session.getDrmaaImplementation() + "; contact string: " + this.contactString);
			this.isInitComplete  = true;
		} catch (DrmaaException e) {
			e.printStackTrace();
			this.LOGGER.error("Can not communicate with the default grid system.");
			System.exit(1);
		}
	}

	@Override
	public void clean(HashMap<String, DRMAAExecutor> ids, boolean isInDetachMode) {
		// do clean-up!
		try {
			this.isInitComplete = false;
			// kill running jobs if required
			if(!isInDetachMode) {
				this.session.synchronize(Arrays.asList(Session.JOB_IDS_SESSION_ALL), 15, true);
			}
			else {
				for(Entry<String, DRMAAExecutor> job : ids.entrySet()) {
					if(!job.getValue().getExecutorInfo().isWatchdogDetachSupported())
						try { this.cancelJob(job.getKey()); } catch(Exception e) { e.printStackTrace(); }
				}
			}
			
			// and some more!
			this.session.exit();
			this.session = null;
		}
		catch(Exception e) { }
	}

	@Override
	public String getNameOfExecutionNode(String id, String watchdogBaseDir) {
		return ExternalScheduledExecutor.getHostname(id, watchdogBaseDir);
	}

	@Override
	public boolean isJobKnownInGridSystem(String id) {
		try {
			this.session.wait(id, Session.TIMEOUT_NO_WAIT);
		}
		catch(ExitTimeoutException et) { return true; }
		catch(InvalidJobException ei) { return false; }
		catch(Exception e) { e.printStackTrace(); }
		return false;
	}

	@Override
	public String getExecutorType() {
		return EXECUTOR_NAME;
	}
	
	private JobTemplate getJobTemplate(Task task, DRMAAExecutor ex) throws DrmaaException {
		DRMAAExecutorInfo execinfo = ex.getExecutorInfo();
		JobTemplate jt = this.session.createJobTemplate();
		// set the command to call
		jt.setRemoteCommand(ex.getFinalCommand(false, false)[0]); 
		
		// set arguments, if not a summary script is called
		if(ex.isSingleCommand())
			jt.setArgs(task.getArguments());
		
		// set on hold if required
		if(task.isTaskOnHold())
			jt.setJobSubmissionState(JobTemplate.HOLD_STATE);

		// set the name of the job
		jt.setJobName(task.getProjectShortCut() + " " + task.getName() + " " + task.getID());

		// set the working directory
		jt.setWorkingDirectory(ex.getWorkingDir(false));
		
		// set the environment variables that should not be set by an external command
		if(ex.hasInternalEnvVars()) {
			HashMap<String, String> env = ex.getInternalEnvVars();
			jt.setJobEnvironment(env);
		}

		// set stdout
		if(task.getStdOut(false) != null) {
			File out = task.getStdOut(true);
			jt.setOutputPath(":" + out.getAbsolutePath());
			if(!task.isOutputAppended())
				out.delete();
		}
		else
			jt.setOutputPath(":" + DEV_NULL);
		// set stderr
		if(task.getStdErr(false) != null) {
			File err = task.getStdErr(true);
			jt.setErrorPath(":" + err.getAbsolutePath());
			if(!task.isErrorAppended())
				err.delete();
		}
		else
			jt.setErrorPath(":" + DEV_NULL);
		// set stdin
		if(task.getStdIn() != null)
			jt.setInputPath(":" + task.getStdIn().getAbsolutePath());
		
		// add memory, slot and queue specification
		String additionalInfo = execinfo.getCommandsForGrid();
		if(additionalInfo != null && additionalInfo.length() > 0) {
			jt.setNativeSpecification(additionalInfo);
		}
		return jt;
	}
	
	private void deleteJobTemplate(JobTemplate job) {
		try { this.session.deleteJobTemplate(job); }
		catch(Exception e) {}
	}

	@Override
	public boolean isInitComplete() {
		return this.isInitComplete;
	}
	
	/**
	 * returns the contact string that was returned by the DRMAA system after init() of the connection
	 * @return
	 */
	public String getContactString() {
		if(this.isInitComplete())
			return this.session.getContact();
		else
			return null;
	}
	
	/**
	 * sets the contact string that is used to init the connection
	 * @param contactString
	 */
	public void setContactString(String contactString) {
		this.contactString = SESSION + contactString;
	}

	@Override
	protected void updateJobStatusCache() {}

	@Override
	public int getMaxWaitCyclesUntilJobsIsVisible() {
		return 0;
	}
}
