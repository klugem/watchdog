package de.lmu.ifi.bio.watchdog.xmlParser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.multithreading.StopableLoopRunnable;
import de.lmu.ifi.bio.watchdog.errorChecker.ParameterReturnErrorChecker;
import de.lmu.ifi.bio.watchdog.errorChecker.WatchdogErrorCatcher;
import de.lmu.ifi.bio.watchdog.executor.MonitorThread;
import de.lmu.ifi.bio.watchdog.executor.WatchdogThread;
import de.lmu.ifi.bio.watchdog.helper.ActionType;
import de.lmu.ifi.bio.watchdog.helper.CheckerContainer;
import de.lmu.ifi.bio.watchdog.helper.Mailer;
import de.lmu.ifi.bio.watchdog.helper.returnType.ReturnType;
import de.lmu.ifi.bio.watchdog.interfaces.ErrorChecker;
import de.lmu.ifi.bio.watchdog.interfaces.SuccessChecker;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessMultiParam;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessReturnValueAdder;
import de.lmu.ifi.bio.watchdog.resume.ResumeInfo;
import de.lmu.ifi.bio.watchdog.resume.ResumeJobInfo;
import de.lmu.ifi.bio.watchdog.resume.WorkflowResumeLogger;
import de.lmu.ifi.bio.watchdog.runner.XMLBasedWatchdogRunner;
import de.lmu.ifi.bio.watchdog.task.StatusHandler;
import de.lmu.ifi.bio.watchdog.task.Task;

/**
 * Creates Tasks based on XMLTask definitions
 * @author Michael Kluge
 *
 */
public class XMLTask2TaskThread extends StopableLoopRunnable {

	private static final int SLEEP_MILIS = 500; // do not set it too low or otherwise the user might be able to modify the params of two tasks and the second will fail because schedulingWasPerformed() was not called between the two calls 
	public static final String TAB = "\t";
	private final WatchdogThread WATCHDOG;
	private final Mailer MAILER;
	private final Map<Integer, XMLTask> XML_TASKS = Collections.synchronizedMap(new LinkedHashMap<Integer, XMLTask>());
	private final HashMap<String, Pair<HashMap<String, ReturnType>, String>> RETURN_TYPE_INFO;
	private final ArrayList<StatusHandler> STATUS_HANDLER = new ArrayList<>();
	private boolean isSchedulingPaused = false;
	private int loopCalls = 1;
	private static final int LOOP_CHECK = 10;
	private final File XML_PATH;
	private final int MAIL_WAIT_TIME;
	
	/**
	 * Adds a new watchdog to which the tasks are added, if some are available
	 * @param watchdog
	 * @param mailer
	 * @param xmlTasks
	 * @param returnTypeInfo
	 */
	public XMLTask2TaskThread(WatchdogThread watchdog, ArrayList<XMLTask> xmlTasks, Mailer mailer, HashMap<String, Pair<HashMap<String, ReturnType>, String>> returnTypeInfo, File xmlPath, int mailWaitTime, HashMap<Integer, HashMap<String, ResumeInfo>> resumeInfo) {
		super("XMLTask2Task");
		this.WATCHDOG = watchdog;
		this.MAILER = mailer;
		this.RETURN_TYPE_INFO = returnTypeInfo;
		this.MAIL_WAIT_TIME = mailWaitTime;
		this.XML_PATH = xmlPath;
	
		
		// add the workflow resume logger task status update handler
		this.addTaskStatusHandler(new WorkflowResumeLogger(this.XML_PATH));

		for(XMLTask x : xmlTasks) {			
			 // check, if return parameter argument must be provided
			 if(this.RETURN_TYPE_INFO.containsKey(x.getTaskType()))
				 x.addReturnParameter(this.RETURN_TYPE_INFO.get(x.getTaskType()).getValue());
			 
			 // check, if there is some return info for that task
			 if(resumeInfo != null && resumeInfo.containsKey(x.getXMLID()))
				 x.addResumeInfo(resumeInfo.get(x.getXMLID()));
			
			// add the task to the scheduler queue
			this.addTask(x);
		}
		// free that memory
		if(resumeInfo != null) 
			resumeInfo.clear();
	}
	
	public boolean isSchedulingPaused() {
		return this.isSchedulingPaused;
	}

	public void setPauseScheduling(boolean pause) {
		if(pause != this.isSchedulingPaused()) { 
			this.isSchedulingPaused = pause;
			MonitorThread.setPauseSchedulingOnAllMonitorThreads(pause);
		}
	}
	
	/**
	 * adds a XML task to a running XML2Task thread
	 * @param x
	 */
	public boolean addTask(XMLTask x) {
		if(this.XML_TASKS.containsKey(x.getXMLID()))
			return false;
		
		this.XML_TASKS.put(x.getXMLID(), x);
		return true;
	}

	/**
	 * tries to convert some XML tasks to normal tasks, if they do not depend on anything
	 * @return
	 */
	public synchronized boolean createAndAddTasks() {
		int newTasks = 0;
		// test if some the the xml tasks can be converted
		for(XMLTask x : new ArrayList<XMLTask>(this.XML_TASKS.values())) {
			// check, if this task is already fully processed
			if(x.hasXMLTaskSpawnedAllTasks() == false && x.isIgnoredBecauseOfIgnoredDependencies() == false) {
				// check, if some global dependencies can be resolved
				x.checkForGlobalResolvedDependencies();
				if(x.hasGlobalDependencies())
					continue;

				// check, if it is a processInput and
				if(x.getProcessBlock() instanceof ProcessReturnValueAdder)
					this.addDependingReturnModifingProcessBlocks(x);

				HashMap<String, Integer> nameMapping = null;
				if(x.getProcessBlock() instanceof ProcessMultiParam) {
					nameMapping = ((ProcessMultiParam) x.getProcessBlock()).getNameMapping(true);
					// no data is there yet --> process this task later
					if(nameMapping.size() == 0) {
						// check, if separate dependencies are finished for that task
						this.checkSeparateDep(x);
						continue;
					}
				}
				LinkedHashMap<String, ArrayList<String>> argumentLists = this.getArgumentLists(x);
				HashMap<String, String> completeArguments = new HashMap<>();
				if(x.getProcessBlock() != null)
					completeArguments.putAll(x.getProcessBlock().getValues());

				// check each of the files separately if there are any dependencies left
				for(String inputName : argumentLists.keySet()) {
					// if the task is blocked, ignore it
					if(x.isBlocked()) 
						continue;
					// check, if this task was already spawned or has any not resolved separate dependencies
					if(x.hasTaskAlreayBeenSpawned(inputName))
						continue;
					if(x.hasSeparateDependencies(inputName))
						continue;
					// check, if x is in reschedule mode --> the task itself must be in the reschedule side --> null is required for confirm of block tasks
					if(x.isRescheduled() && !(x.isRescheduled(inputName) || x.isRescheduled(null)))
						continue;
					
					// check, if it is in resume mode
					ResumeInfo resumeInfo = null;
					if(x.hasResumeInfo())
						resumeInfo = x.getResumeInfo(inputName);

					// check, if the user want to verify the parameters first
					if(resumeInfo == null && (x.getConfirmParam().isEnabled() || x.getConfirmParam().isSubtaskEnabled()) && !x.getConfirmParam().wasPerformed()) {
						this.MAILER.notifyParamConfirmation(x);
						x.setConfirmParam(ActionType.PERFORMED);
						x.block();
						continue;
					 }

					 // task can be scheduled
					 String completeRawargumentList = completeArguments.get(inputName);
					 Task t = new Task(x.getXMLID(), x.getTaskName(), x.getExecutor(), x.getBinaryCall(), x.getArguments(completeRawargumentList, nameMapping, true), null, null, null, inputName, x.getStdIn(completeRawargumentList), x.getStdOut(completeRawargumentList), x.getStdErr(completeRawargumentList), x.isOutputAppended(), x.isErrorAppended(), x.getWorkingDir(completeRawargumentList), x.getProcessBlock() !=null ? x.getProcessBlock().getClass() : null, nameMapping, x.getEnvironment(), x.getTaskActions(x.getXMLID()+"", completeRawargumentList, nameMapping), x.isSaveResourceUsageEnabled(), x.mightProcessblockContainFilenames());
					 t.setMaxRunning(x.getMaxRunning());
					 t.setProject(x.getProjectName());
					 t.addErrorChecker(new WatchdogErrorCatcher(t));
					 t.setForceSingleSlaveMode(x.isSingleSlaveModeForced());
				 
					 // set status handler if some are set
					 for(StatusHandler sh : this.STATUS_HANDLER) 
						 t.addStatusHandler(sh);

					// check, if the task should be executed on a slave
					if(x.getExecutor().isStick2Host()) {
						// check, is there is any task this task depends on that was already executed on a slave
						String slaveID = x.getSlaveIDOfDependencies(inputName);
						if(slaveID == null && x.getGlobalPrevSlaveId() != null)
							slaveID = x.getGlobalPrevSlaveId();
						
						// set preeceeding slave ID if one is there
						if(slaveID != null)
							t.setSlaveTaskID(slaveID);
					}

					 if(this.RETURN_TYPE_INFO.containsKey(x.getTaskType()))
						t.addErrorChecker(new ParameterReturnErrorChecker(t, this.RETURN_TYPE_INFO.get(x.getTaskType()).getKey(), x.getReturnParameterFile(completeRawargumentList)));
					 
					 // add custom error or success checkers
					 for(CheckerContainer c : x.CHECKER) {
						 boolean isErrorChecker = c.isErrorChecker();
						 Object checker = c.getChecker(t, completeRawargumentList, nameMapping, x.getProcessBlock(), x.getNumberOfSpawnedTasks());
						 
						 // typecast and store the checker!
						 if(isErrorChecker)
							 t.addErrorChecker((ErrorChecker) checker);
						 else
							 t.addSuccessChecker((SuccessChecker) checker);
					 }
						 
					 t.setNotify(x.getNotify());
					 t.setCheckpoint(x.getCheckpoint());
					 x.addExecutionTask(t);

					 // check, if resumeInfo is valid
					 if(resumeInfo != null && (!resumeInfo.isResumeInfoValid(x, t) || x.isDirty(true) || t.willRunOnSlave())) {
						 if(x.isDirty(true) || resumeInfo.isDirty())
							 LOGGER.warn("Resume info was not used for task " + t.getID() + " (subtask param: '"+inputName+"') as a dependency was re-executed.");
						 else if(t.willRunOnSlave())
							 LOGGER.warn("Resume info was not used for task " + t.getID() + " (subtask param: '"+inputName+"') as resume is not supported in slave mode.");
						 else
							 LOGGER.warn("Resume info was not used for task " + t.getID() + " (subtask param: '"+inputName+"') as parameter hash was not equal.");
						 
						 resumeInfo = null;
						 // mark to resume info for all tasks that depend on this one as dirty
						 x.flagResumeInfoAsDirty(t);
					 }

					 if(resumeInfo == null) {
						 this.WATCHDOG.addToQue(t);
					 }
					 // task was already executed successfully
					 else {
						 t.increaseExecutionCounter();
						 t.setJobInfo(new ResumeJobInfo());
						 if(resumeInfo.hasReturnParams()) {
							 t.setReturnParams(resumeInfo.getReturnParams());
						 }
					 }
					 newTasks++;
				}
				if(!x.isBlocked())
					this.checkSeparateDep(x);
				
				// delete, tasks which should be rescheduled.
				if(x.isRescheduled())
					x.clearRescheduled();
						
			}
			else {
				// check, if all tasks had been executed
				if(!x.hasRunningTasks())
					x.inform();	
			}
			x.schedulingWasPerformed();
		}
		return newTasks > 0;
	}
	
	/**
	 * checks, if all separate dependencies of a task are finished --> no new subtasks will be spawned
	 * @param x
	 */
	private void checkSeparateDep(XMLTask x) {
		boolean allSeparateDependenciesAreReady = true;
		// check if some separate dependencies are not completely finished --> some new jobs might be spawned later
		for(int xmlID : x.getSeparateDependencies()) {
			XMLTask sepDep = XMLTask.getXMLTask(xmlID);
			if(!sepDep.isCompleteTaskReady()) {
				allSeparateDependenciesAreReady = false;
				return;
			}
		}
		// mark XML task as finished if all tasks were spawned
		if(allSeparateDependenciesAreReady) { // if this is a problem implement more complex check, that includes ignored tasks
			x.endCheckingForNewTasks();
		}
	}

	
	/**
	  * Creates a list of argument lists based on a xml task
	  * @param x
	  * @return
	  */
	public LinkedHashMap<String, ArrayList<String>> getArgumentLists(XMLTask x) {
		LinkedHashMap<String, ArrayList<String>> list = new LinkedHashMap<>();
		// single file mode
		if(x.getProcessBlock() == null) {
			list.put("", Task.parseArguments(x.getArguments()));
		}
		// process group block! :-)
		else {
			HashMap<String, String> v = x.getProcessBlock().getValues();
			// check, which type of process block it is
			if(x.getProcessBlock() instanceof ProcessMultiParam) {
				for(String input : v.keySet()) {
					list.put(input, Task.parseArguments(x.getArguments(v.get(input), ((ProcessMultiParam) x.getProcessBlock()).getNameMapping())));
				}
			}
			else {
				for(String input : v.keySet()) {
					list.put(input, Task.parseArguments(x.getArguments(input, null)));
				}
			}
		}
		return list;
	}
	
	/**
	 * adds some values to processInput blocks, if finished tasks depend on that
	 * @param x
	 */
	public boolean addDependingReturnModifingProcessBlocks(XMLTask x) {
		if(!((x.getProcessBlock() instanceof ProcessReturnValueAdder)))
			return false;
			
		boolean retVal = false;
		boolean noSeparate = true;
		ProcessReturnValueAdder input = (ProcessReturnValueAdder) x.getProcessBlock();
		int numberOfSeparateDep = x.getSeparateDependencies().size(); 
		int numberOfGlobalDep = x.getOriginalGlobalDependencies().size();
		HashMap<String, ArrayList<Task>> finishedDep = new HashMap<>();
		
		// run through all separate dependencies
		for(int dep : x.getSeparateDependencies()) {
			noSeparate = false;
			XMLTask d = this.XML_TASKS.get(dep);
			// run through the spawned tasks
			for(Task t : d.getExecutionTasks().values()) {
				// task has been finished and is not blocked
				if(t.hasTaskFinishedWithoutBlockingInfo()) {

					// ensure that the key is there
					if(!finishedDep.containsKey(t.getGroupFileName()))
						finishedDep.put(t.getGroupFileName(), new ArrayList<Task>());
										
					// add the task
					finishedDep.get(t.getGroupFileName()).add(t);
				}
			}
		}	
		
		HashMap<Integer, HashMap<String, ArrayList<String>>> globalRet = new HashMap<>();
		// add variables from global dependencies
		for(int dep : x.getOriginalGlobalDependencies()) {
			XMLTask d = this.XML_TASKS.get(dep);
			HashMap<String, ArrayList<String>> ret = new HashMap<>();
			globalRet.put(dep, ret);
			for(Task t : d.getExecutionTasks().values()) {
				if(t.hasTaskFinishedWithoutBlockingInfo()) {
					HashMap<String, String> r = t.getReturnParams();
					for(String key : r.keySet()) {
						if(!ret.containsKey(key))
							ret.put(key, new ArrayList<String>());
						ret.get(key).add(r.get(key));
					}
					
					// add the global dependency, but only, if it does not depend on any other separate task
					if(noSeparate) {
						// add the task
						finishedDep.put("", new ArrayList<Task>());
						finishedDep.get("").add(t);
					}
				}
			}
		}

		// check, which of them have all required separate dependencies finished
		for(String groupName : finishedDep.keySet()) {
			if(!input.hasBlock(groupName) && ((!noSeparate && finishedDep.get(groupName).size() == numberOfSeparateDep) || (noSeparate &&  (finishedDep.get(groupName).size()) == numberOfGlobalDep))) {
				TreeMap<String, String> retValues = new TreeMap<>();
				
				// add global values
				for(int dep : globalRet.keySet()) {
					// add the joined values
					HashMap<String, ArrayList<String>> ret = globalRet.get(dep);
					for(String key : ret.keySet()) {
						retValues.put(key, input.joinGlobalReturnValues(ret.get(key)));
					}
				}
				
				// collect all the variables from the local dependencies
				for(Task t : finishedDep.get(groupName)) {
					retValues.putAll(t.getReturnParams());
				}
				
				// add the input
				input.addBlock(groupName, retValues);
				retVal = true;
			}
		}
		return retVal;
	}

	/**
	 * Checks, if there are some unfinished tasks
	 * @return
	 */
	public synchronized boolean hasUnfinishedTasks() {
		for(XMLTask x : new ArrayList<XMLTask>(this.XML_TASKS.values())) {
			if(!x.isProcessingOfTaskFinished()) {
				return true;
			}
		}
		return false;
	}

	public void addTaskStatusHandler(StatusHandler sh) {
		this.STATUS_HANDLER.add(sh);
	}

	@Override
	public int executeLoop() throws InterruptedException {		
		// check, if we are done
		if(this.loopCalls % LOOP_CHECK == 0) {
			this.loopCalls = 0;

			if(!this.hasUnfinishedTasks()) {
				this.requestStop(5, TimeUnit.SECONDS);
				return 1;
			}
		}
		this.loopCalls++;
		
		// check, if new tasks can be scheduled
		if(!this.isSchedulingPaused()) {
			this.createAndAddTasks();
			return 1;
		}
		return 5;
	}

	@Override
	public void afterLoop() {
		
	}

	@Override
	public long getDefaultWaitTime() {
		return SLEEP_MILIS;
	}

	@Override
	public void beforeLoop() {
		if(Task.isMailSet()) {
			if(!Task.getMailer().hello(this.XML_PATH.getAbsolutePath())) {
				LOGGER.error("Failed to use the mail server to send mails."+System.lineSeparator()+"Either use -mailConfig to configure the mail server or don't use the mail attribute of <task> in order to disable mail notifications.");
				System.exit(1);
			}
		}
		else {
			LOGGER.warn(XMLBasedWatchdogRunner.LOG_SEP);
			LOGGER.warn("[WARN] No mail adress was set! You will not receive any messages on failure or success via mail.");
			LOGGER.warn(Mailer.getHelloTxt(this.XML_PATH.getAbsolutePath()).replaceAll("\n+", "\n"));

			// wait only if a value greater than zero is given
			int wait = this.MAIL_WAIT_TIME * 1000;
			if(wait > 0) {
				LOGGER.warn("[WARN] Waiting "+this.MAIL_WAIT_TIME+"s...");
				LOGGER.warn(XMLBasedWatchdogRunner.LOG_SEP);
				try { Thread.sleep(wait); } catch(Exception e) {}
			}
			else 
				LOGGER.warn(XMLBasedWatchdogRunner.LOG_SEP);				
		}
	}
}
