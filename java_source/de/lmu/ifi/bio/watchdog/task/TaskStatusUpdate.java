package de.lmu.ifi.bio.watchdog.task;

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.ggf.drmaa.DrmaaException;

import de.lmu.ifi.bio.multithreading.StopableLoopRunnable;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.interfaces.ErrorChecker;
import de.lmu.ifi.bio.watchdog.interfaces.SuccessChecker;
import de.lmu.ifi.bio.watchdog.resume.ResumeJobInfo;
import de.lmu.ifi.bio.watchdog.slave.Master;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask;

/**
 * Updates a task once it is finished but can wait for some suff without blocking the processing of the rest of the tasks
 * @author Michael Kluge
 *
 */
public class TaskStatusUpdate extends StopableLoopRunnable implements Serializable {

	private static final long serialVersionUID = 4765081705257646851L;
	private final Task T;
	private static char NEWLINE = '\n';
	private static char TAB = '\t';
	
	public TaskStatusUpdate(Task t) {
		super("TaskStatusUpdate_" + t.getID());
		this.T = t;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public int executeLoop() throws InterruptedException {
		try {
			// check, if some info is already there
			if(this.T.info != null) {
				boolean isResume = this.T.info instanceof ResumeJobInfo;
				// check if it is resume job
				if(isResume) {
					this.T.LOGGER.debug("Task with ID " + this.T.getID() + " was not executed as Watchdog was started in resume mode.");
					this.T.setStatus(TaskStatus.FINISHED);
					
					// TODO: remove from slave if slave resume support will be implemented
					this.T.setTaskStatusUpdateFinished();
					return 1;
				}
				this.T.setStatus(TaskStatus.STATUS_CHECK);
				
				// job failed because of some syntax error or f.e. stdout file could not be created
				if(this.T.info.wasAborted()) {
					this.T.LOGGER.error("Task with ID " + this.T.getID() + " was aborted.");
					
					// check, if it was because of an before action
					if(StringUtils.join(this.T.getErrors()).contains(TaskActionTime.BEFORE.toString()))
						this.T.setStatus(TaskStatus.BEFORE_ACTION_FAILED);
					else
						this.T.setStatus(TaskStatus.FAILED_SYNTAX);
				}
				// job ended with some exit status
				else if(this.T.info.hasExited()) {
					this.T.exitStatus=this.T.info.getExitStatus();
					if((this.T.getTaskID() > 0 || this.T.exitStatus != 0))
						this.T.LOGGER.debug("Task with ID " + this.T.getID() + " exited with status " + this.T.exitStatus + ".");
				
					// check, if exit status was zero
					if(this.T.exitStatus == 0) {
						
						// check, if the job should be blocked
						if(this.T.checkpoint != null && !this.T.checkpoint.isDisabled())
							this.T.blockTask();
							
						boolean ok = true;
						// check for errors
						for(ErrorChecker eCheck : this.T.ERROR_CHECKER) {
							if(eCheck.hasTaskFailed()) {
								this.T.ERRORS.addAll(eCheck.getErrorMessages());
								this.T.setStatus(TaskStatus.FAILED_ERROR_CHECK);
								this.T.LOGGER.error("Task with ID " + this.T.getID() + " failed because an error checker found some errors.");
								ok = false;
							}
						}
						// check for success
						for(SuccessChecker sCheck : this.T.SUCCESS_CHECKER) {
							if(!this.T.getStatus().equals(TaskStatus.FAILED_SUCCESS_CHECK) && sCheck.hasTaskSucceeded()) {
								this.T.LOGGER.info("Task with ID " + this.T.getID() + " was successfull because the success checker told so.");
							}
							else {
								this.T.setStatus(TaskStatus.FAILED_SUCCESS_CHECK);
								ok = false;
								break;
							}
						}
						
						// if all is ok, set status to ok
						if(ok)
							if(!this.T.hasErrors())
								this.T.setStatus(TaskStatus.FINISHED);
							else 
								this.T.setStatus(TaskStatus.FAILED);
						
						if(TaskStatus.FAILED_SUCCESS_CHECK.equals(this.T.getStatus())) {
							// print command of this.T task;
							this.T.LOGGER.error("Command failed: '" + this.T.getBinaryCall() + "' with following arguments.");
							this.T.LOGGER.error(StringUtils.join(this.T.getArguments(), " "));
							this.T.LOGGER.error("Command failed caused by success checker!");
							this.T.setStatus(TaskStatus.FAILED);
						}
					}
					else {
						// let the error checker do it's work to get some error messages.
						for(ErrorChecker eCheck : this.T.ERROR_CHECKER) {
							if(eCheck.hasTaskFailed())
								this.T.ERRORS.addAll(eCheck.getErrorMessages());
						}
						// print command of this.T task;
						this.T.LOGGER.error("Command failed: '" + this.T.getBinaryCall() + "' with following arguments.");
						this.T.LOGGER.error(StringUtils.join(this.T.getArguments(), " "));
						this.T.setStatus(TaskStatus.FAILED);
					}
					
					// save the used resources
					for(Object key : this.T.info.getResourceUsage().keySet()) {
						try {
							this.T.USED_RESOURCES.put(key.toString(), Double.parseDouble(this.T.info.getResourceUsage().get(key).toString()));
						}
						catch(NullPointerException e) {}
						catch(NumberFormatException e) {}
					}
					// save resources to file if wished
					File resFile = this.T.getSaveResFilename();
					if(resFile != null) {
						StringBuilder b = new StringBuilder("resources used by task ");
						b.append(this.T.getID()); 
						b.append(" - ");
						b.append(this.T.getName());
						b.append(":");
						b.append(NEWLINE);
						for(String key : this.T.USED_RESOURCES.keySet()) {
							b.append(key);
							b.append(TAB);
							b.append(this.T.USED_RESOURCES.get(key));
							b.append(NEWLINE);
						}
						Functions.write(resFile.toPath(), b.toString());
					}
				}
				else if(this.T.info.hasSignaled()){
					this.T.setStatus(TaskStatus.KILLED);
					this.T.terminationSignal = this.T.info.getTerminatingSignal();
					this.T.LOGGER.error("Task with ID " + this.T.getID() + " recieved a termination signal.");
				}
				else {
					this.T.setStatus(TaskStatus.KILLED);
					this.T.LOGGER.error("Task with ID " + this.T.getID() + " was killed.");
				}			
				if(!this.T.hasTaskFinished() && !this.T.isBlocked()) {
					this.T.LOGGER.error("Failed command: " + this.T.getBinaryCall() + " " + StringUtils.join(this.T.getArguments(), " "));
				}
				
				// perform after action
				this.T.performAction(TaskActionTime.AFTER);
				
				// perform actions fail and success
				if(this.T.hasTaskFinished() || this.T.isBlocked()) {
					this.T.performAction(TaskActionTime.SUCCESS);
					
					// check, if any of these actions failed and status is ok so far... 
					if(StringUtils.join(this.T.getErrors()).contains(TaskAction.AERROR))
						this.T.setStatus(TaskStatus.AFTER_ACTION_FAILED);
				}
				else
					this.T.performAction(TaskActionTime.FAIL);

				// remove it from the slave, if required!
				TaskStatus s = this.T.getStatus();
				if(s.isGUIFinished()) {
					XMLTask.deleteSlaveID(this.T);
					Master.unregisterTask(this.T);
				}
								
				// inform the user
				if(Task.mailer != null && (this.T.hasErrors() || (this.T.notify != null && ((this.T.notify.isSubtaskEnabled() && this.T.hasGroupFileName()) || (this.T.notify.isEnabled() && !this.T.hasGroupFileName()))) || (!TaskStatus.FINISHED.equals(this.T.getStatus()) && !this.T.notify.wasPerformed()) || (this.T.isBlocked && (this.T.getCheckpoint().isSubtaskEnabled() || this.T.getCheckpoint().isEnabled() && !this.T.hasGroupFileName())))) {
					// do not send any mails regarding slave executors
					if(this.T.getTaskID() >= 0) {
						Task.mailer.inform(this.T);
					}
				}
				this.T.setTaskStatusUpdateFinished();
			}
		}
		catch(DrmaaException e) { e.printStackTrace(); }
		finally {
			this.requestStop(10, TimeUnit.MILLISECONDS);
		}
		return 1;
	}

	@Override
	public void afterLoop() {}

	@Override
	public long getDefaultWaitTime() {
		return 1;
	}

	@Override
	public void beforeLoop() {
	
	}

	@Override
	public boolean canBeStoppedForDetach() {
		return false;
	}
}
