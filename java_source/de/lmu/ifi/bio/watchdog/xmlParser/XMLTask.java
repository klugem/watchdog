package de.lmu.ifi.bio.watchdog.xmlParser;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.executor.Executor;
import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.executor.HTTPListenerThread;
import de.lmu.ifi.bio.watchdog.executor.WatchdogThread;
import de.lmu.ifi.bio.watchdog.helper.ActionType;
import de.lmu.ifi.bio.watchdog.helper.CheckerContainer;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.ErrorCheckerStore;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.helper.GUIInfo;
import de.lmu.ifi.bio.watchdog.helper.ReplaceSpecialConstructs;
import de.lmu.ifi.bio.watchdog.helper.returnType.ReturnType;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.optionFormat.OptionFormat;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessBlock;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessMultiParam;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.task.TaskAction;
import de.lmu.ifi.bio.watchdog.task.TaskStatus;
import de.lmu.ifi.bio.watchdog.task.actions.CopyTaskAction;
import de.lmu.ifi.bio.watchdog.task.actions.CreateTaskAction;
import de.lmu.ifi.bio.watchdog.task.actions.DeleteTaskAction;

/**
 * XML representation of a task
 * @author Michael Kluge
 *
 */
public class XMLTask {
	
	public static final String DEFAULT_PARAM_SEP = ",";	
	public static final String INTERNAL_PARAM_SEP = "@-%-§-%-@";
	private final static String RETURN_PARAM = "returnParam";
	private static final ConcurrentHashMap<Integer, XMLTask> XML_TASKS = new ConcurrentHashMap<>(); // stores all XML Tasks which were created via the constructor
	
	private static final Logger LOGGER = new Logger(LogLevel.INFO);
	private final ArrayList<TaskAction> TASK_ACTIONS = new ArrayList<>();
	private final int XML_ID;
	private final int VERSION;
	private final String BIN_NAME;
	private final String TASK_TYPE;
	private final String NAME;
	private final String PROJECT_NAME;
	private final ProcessBlock PROCESS_BLOCK;
	private final ExecutorInfo EXECUTOR_INFO;
	private final OptionFormat OPTION_FORMAT;
	private final HashMap<Integer, Integer> DEPENDS = new HashMap<>();
	private final LinkedHashSet<Integer> DEPENDS_SEP_KEEP = new LinkedHashSet<>();
	private final HashMap<Integer, String> DEPENDS_SEP = new HashMap<>();
	private final HashSet<Integer> NEEDED_DEPENDENCIES_4_VARS = new HashSet<>();
	private final HashMap<Integer, HashSet<String>> AVAIL_RETURN_PARAMS = new HashMap<>();
	private final LinkedHashMap<String, String> PARAM = new LinkedHashMap<>();
	private final HashSet<String> NO_GUI_LOAD_PARAM = new HashSet<>();
	private final HashMap<String, Boolean> PARAM_IS_FLAG = new HashMap<>();
	private final HashMap<String, OptionFormat> PARAM_FORMATER = new HashMap<>();
	private final ConcurrentHashMap<String, Task> SPAWNED_TASKS = new ConcurrentHashMap<>();
	private final LinkedHashMap<String, String> PARAM_GLOBAL = new LinkedHashMap<>();
	private final HashMap<String, Boolean> PARAM_IS_FLAG_GLOBAL = new HashMap<>();
	private final HashMap<String, ReturnType> RETURN_PARAMS = new HashMap<>();
	private final HashMap<String, OptionFormat> PARAM_FORMATER_GLOBAL = new HashMap<>();
	private final HashMap<String, LinkedHashMap<String, Pair<Pair<String, String>, String>>> CACHE = new HashMap<>();
	private final HashMap<String, File> RETURN_FILE_NAME = new HashMap<>();
	protected final HashSet<CheckerContainer> CHECKER = new HashSet<>();
	private final TreeSet<Integer> ORIGINAL_GLOBAL_DEPENDENCIES = new TreeSet<>();
	private final HashSet<String> RESCHEDULE = new HashSet<>();
	private boolean hasXMLTaskSpawnedAllTasks = false;
	private int maxRunning = -1;
	private String stdInString = null;
	private String stdOutString = null;
	private String stdErrString = null;
	private String workingDirectoryString = null;
	private boolean stdOutAppend = false;
	private boolean stdErrAppend = false;
	private boolean disableExistenceCheckStdin = false;
	private ActionType notify;
	private ActionType notifyBackup;
	private ActionType checkpoint;	
	private ActionType confirmParam;	
	private boolean blockUntilSchedule = false;
	private boolean block = false;
	private String returnParamName = null;
	protected Environment env;
	private String prev_global_slave_id = null;
	private final HashMap<String, String> SLAVE_IDS = new HashMap<>();
	private boolean forceSingleSlaveMode = false;
	private boolean isIgnored = false;
	private GUIInfo guiInfo = null;
	private boolean setSaveResourceUsage = false;
	private String moduleVersionParameterSetName = null;
	
	/**
	 * Constructor
	 * @param xmlID
	 * @param binName
	 * @param name
	 * @param projectName
	 * @param optionFormater
	 * @param executorInfo
	 * @param environment
	 */
	public XMLTask(int xmlID, String taskType, int version, String binName, String name, String projectName, OptionFormat optionFormater, ExecutorInfo executorInfo, Environment environment) {
		this.XML_ID = xmlID;
		this.VERSION = version;
		this.TASK_TYPE = taskType + XMLParser.VERSION_SEP + this.VERSION;
		this.NAME = name;
		this.PROJECT_NAME = projectName;
		this.BIN_NAME = binName;
		this.OPTION_FORMAT = optionFormater;
		this.EXECUTOR_INFO = executorInfo;
		this.PROCESS_BLOCK = null;
		this.setEnvironment(environment);
		// store the task in the static variable
		XML_TASKS.put(this.getXMLID(), this);
	}
	
	/**
	 * Constructor
	 * @param xmlID
	 * @param binName
	 * @param name
	 * @param projectName
	 * @param optionFormater
	 * @param executorInfo
	 * @param processBlock might be null
	 */
	public XMLTask(int xmlID, String taskType, int version, String binName, String name, String projectName, OptionFormat optionFormater, ExecutorInfo executorInfo, ProcessBlock processBlock) {
		this.XML_ID = xmlID;
		this.VERSION = version;
		this.TASK_TYPE = taskType + XMLParser.VERSION_SEP + this.VERSION; 
		this.BIN_NAME = binName;
		this.NAME = name;
		this.PROJECT_NAME = projectName;
		this.OPTION_FORMAT = optionFormater;
		this.EXECUTOR_INFO = executorInfo;
		this.PROCESS_BLOCK = processBlock;

		// store the task in the static variable
		XML_TASKS.put(this.getXMLID(), this);
	}
	
	/**
	 * adds a new flag without argument
	 * @param name
	 */
	public void addFlag(String name, OptionFormat formater) {
		this.addParameter(name, null, true, formater, -1);
	}
	
	/**
	 * adds a parameter with an argument
	 * @param name
	 * @param value
	 * @param formater
	 */
	public void addParameter(String name, String value, OptionFormat formater, int depID) {
		this.addParameter(name, value, false, formater, depID);
	}
	
	/**
	 * sets a return parameter
	 * @param returnName
	 */
	public void addReturnParameter(String returnName) {
		this.returnParamName = returnName;
	}
	
	/**
	 * returns the return parameter file that was used or null if none was used for that input.
	 * @return
	 */
	public File getReturnParameterFile(String inputReplacement) {
		return this.RETURN_FILE_NAME.get(inputReplacement);
	}
	
	public void setReturnParameter(HashMap<String, ReturnType> retParams) {
		if(retParams != null)
			this.RETURN_PARAMS.putAll(retParams);
	}
	
	public HashMap<String, ReturnType> getReturnParameters() {
		return new HashMap<String, ReturnType>(this.RETURN_PARAMS);
	}
	
	/**
	 * sets the parameter that is used to sent the used version of the XSD module to the script
	 */
	public void setModuleVersionParameterSetName(String name) {
		this.moduleVersionParameterSetName = name;
	}
	
	/**
	 * @returns the parameter that is used to sent the used version of the XSD module to the script
	 * @return
	 */
	public String getModuleVersionParameterSetName() {
		return this.moduleVersionParameterSetName;
	}
	
	@SuppressWarnings("rawtypes")
	public void addChecker(Constructor c, ArrayList<Pair<Class, String>> constructorValues, boolean errorChecker, File classFile) {
		this.CHECKER.add(new CheckerContainer(c, constructorValues, errorChecker, classFile));
	}
	
	/**
	 * adds a parameter which might also be a flag
	 * @param name
	 * @param value
	 * @param isFlag
	 * @param formater
	 */
	public void addParameter(String name, String value, boolean isFlag, OptionFormat formater, int depID) {
		// check if there is already a value for that parameter and update the value if it is the case
		if(!isFlag && this.PARAM.containsKey(name)) {
			value = this.PARAM.get(name) + INTERNAL_PARAM_SEP + value;
		}
		this.PARAM.put(name,  value);
		this.PARAM_IS_FLAG.put(name, isFlag);
		
		if(depID != -1)
			this.NEEDED_DEPENDENCIES_4_VARS.add(depID);
		
		// adds the formater if it is valid (i.e. at least one of the parameters were set by the user in the XML file
		if(formater != null) 
			this.PARAM_FORMATER.put(name, formater);
	}
	
	/**
	 * xml id of the task
	 * @return
	 */
	public int getXMLID() {
		return this.XML_ID;
	}
	
	/**
	 * Name of that task
	 * @return
	 */
	public String getTaskName() {
		return this.NAME;
	}
	
	/**
	 * Name of that project
	 * @return
	 */
	public String getProjectName() {
		return this.PROJECT_NAME;
	}
	
	/**
	 * Type of the task
	 * @return
	 */
	public String getTaskType() {
		return this.TASK_TYPE;
	}
	
	/** 
	 * adds a dependency with an id
	 * @param xmlID
	 * @param dependsSeparately
	 * @param prefixName
	 */
	public void addDependencies(int xmlID, boolean dependsSeparately, boolean keep4slave, String prefixName, String sep, HashSet<String> availReturnParameters) {
		int prefixLength = -1;
		// parse number
		if(dependsSeparately) {
			Matcher matcher = ReplaceSpecialConstructs.PATTERN_FILENAME.matcher(prefixName);
			// try to match the pattern
			if(matcher.matches()) {
				String number = matcher.group(1);
				try { 
					// try to parse a number
					prefixLength = Integer.parseInt(number); 
					} catch(NumberFormatException e) {
						prefixLength = 0;
					}
			}
		}
		else // store global dependency which will not be deleted for inputLoop
			this.ORIGINAL_GLOBAL_DEPENDENCIES.add(xmlID);
		
		this.DEPENDS.put(xmlID, prefixLength);
		this.AVAIL_RETURN_PARAMS.put(xmlID, availReturnParameters);
		if(sep != null) {
			this.DEPENDS_SEP.put(xmlID, sep);
			if(keep4slave)
				this.DEPENDS_SEP_KEEP.add(xmlID);
		}
	}
	
	/**
	 * Deletes a global dependency if it exists
	 * @param xmlID
	 */
	private void deleteGlobalDependency(Integer xmlID) {
		if(this.DEPENDS.containsKey(xmlID) && this.DEPENDS.get(xmlID) == -1) {
			this.DEPENDS.remove(xmlID);
			this.AVAIL_RETURN_PARAMS.remove(xmlID);
		}
	}
		
	/**
	 * adds a task which was created based on this XMLTask
	 * @param t
	 */
	public void addExecutionTask(Task t) {
		this.SPAWNED_TASKS.put(t.getGroupFileName(), t);
	}
	
	/**
	 * returns a ArrayList with tasks which were created based on this XMLTask
	 * @return
	 */
	public LinkedHashMap<String, Task> getExecutionTasks() {
		return new LinkedHashMap<>(this.SPAWNED_TASKS);
	}
	
	
	/**
	 * Checks, if the job has some global dependencies
	 * @return
	 */
	public boolean hasGlobalDependencies() {
		return this.getGlobalDependencies().size() > 0;
	}
	
	/**
	 * Checks, if the job has some separate dependencies
	 * param inputName inputName of the job which wants to know if there are any separate dependencies left
	 * @return
	 */
	public boolean hasSeparateDependencies(String inputName) {
		return this.getSeparateDependencies(inputName).size() > 0;
	}
	
	/**
	 * Checks, if the XML task has some separate dependencies
	 * @return
	 */
	public boolean hasSeparateDependencies() {
		return this.getSeparateDependencies().size() > 0;
	}
	
	/**
	 * Returns the separate dependencies for the complete XMLTask
	 * @return
	 */
	public TreeSet<Integer> getSeparateDependencies() {
		return this.getSeparateDependencies(null);
	}
	
	/**
	 * returns the global dependencies of that xml task
	 * @return
	 */
	public TreeSet<Integer> getGlobalDependencies() {
		TreeSet<Integer> dep = new TreeSet<>();
		for(int d : this.DEPENDS.keySet()) {
			if(this.DEPENDS.get(d) == -1) {
				dep.add(d);
			}
		}
		return dep;
	}
	
	/**
	 * returns the separate dependencies of that xml task
	 * @return
	 */
	public TreeSet<Integer> getSeparateDependencies(String inputName) {
		TreeSet<Integer> dep = new TreeSet<>();

		for(int xmlID : this.DEPENDS.keySet()) {
			if(this.DEPENDS.get(xmlID) >= 0) {
				int prefixLength = this.DEPENDS.get(xmlID);
				XMLTask depends = XML_TASKS.get(xmlID);			
				// check, if the task with that prefix ID was already finished
				if(inputName == null || !depends.isProcessBlockTaskReady(inputName, prefixLength, this.DEPENDS_SEP.get(xmlID), this))
					dep.add(xmlID);	
			}
		}
		return dep;
	}
	
	/**
	 * Retuns dependencies that should be transfered to the slave host
	 * @param inputName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public LinkedHashSet<Integer> getSeparateSlaveDependencies() {
		return (LinkedHashSet<Integer>) DEPENDS_SEP_KEEP.clone();
	}
	
	/**
	 * detailed separate dependencies info for GUI
	 * @return
	 */
	public HashMap<Integer, Pair<Integer, String>> getDetailSeperateDependencies() {
		HashMap<Integer, Pair<Integer, String>> r = new HashMap<>();
		for(int id : this.getSeparateDependencies()) {
			r.put(id, Pair.of(this.DEPENDS.get(id), this.DEPENDS_SEP.get(id)));
		}
		return r;
	}
	
	/**
	 * checks, if global dependencies of some tasks can be resolved because these tasks finished successfully
	 */
	public void checkForGlobalResolvedDependencies() {
		if(this.hasXMLTaskSpawnedAllTasks() == false) {

			// check, if some global dependencies of that task are ready
			if(this.hasGlobalDependencies()) {
				for(int xmlID : this.getGlobalDependencies()) {
					if(XML_TASKS.containsKey(xmlID)) {
						XMLTask depends = XML_TASKS.get(xmlID);
						// check, if all tasks of the dependency were finished
						if(depends.isCompleteTaskReady()) {
							if(!depends.isSomeTaskIgnored()) {
								//resolve that dependency by deleting it
								this.deleteGlobalDependency(xmlID);
								LOGGER.info("Resolved global dependency '" + xmlID + "' for task with ID '" + this.getXMLID() + "'.");
							
								// checks, if the task has a slave task ID and sets it if none is set already!
								if(!this.hasGlobalPrevSlaveId()) {
									// do not use slave ids from process blocks as global dependencies!
									if(!this.hasProcessBlock() && !depends.hasProcessBlock()) {
										for(Task t : depends.SPAWNED_TASKS.values()) {
											this.setGlobalSlaveId(t);
										}
									}
								}
							}
							// some task is ignored !
							else {
								this.isIgnored = true;
								if(Task.getMailer() != null)
									Task.getMailer().informIgnoreDependencies(this);
								LOGGER.info("Global dependency '" + xmlID + "' for task with ID '" + this.getXMLID() + "' will not be resolved as the task is ignored.");
							}
						}
					}
					else {
						LOGGER.error("Missing XML dependency for ID '" + xmlID + "'!");
						System.exit(1);
					}
				}
			}
		}
	}
	
	/**
	 * true, if the task is ignored caused by ignored dependencies
	 * @return
	 */
	public boolean isIgnoredBecauseOfIgnoredDependencies() {
		return this.isIgnored;
	}
	
	/**
	 * returns the process block of that xml task
	 * @return
	 */
	public ProcessBlock getProcessBlock() {
		return this.PROCESS_BLOCK;
	}
	
	public boolean mightProcessblockContainFilenames() {
		return this.hasProcessBlock() && this.PROCESS_BLOCK.mightContainFilenames();
	}
	 
	/**
	 * true, if the task is a process block task
	 * @return
	 */
	public boolean hasProcessBlock() {
		return this.PROCESS_BLOCK != null;
	}
	public boolean hasEnvironment() {
		return this.env != null;
	}
	public boolean hasExecutor() {
		return this.EXECUTOR_INFO != null;
	}
	
	/**
	 * test, if the XML task has already spawned all of its tasks
	 * @return
	 */
	public boolean hasXMLTaskSpawnedAllTasks() {
		return this.hasXMLTaskSpawnedAllTasks;
	}
	
	/**
	 * maximal n jobs of that type can run at the same time
	 * @param maxRunning
	 */
	public void setMaxRunning(int maxRunning) {
		if(maxRunning > 0) 
			this.maxRunning = maxRunning;
	}
	
	/**
	 * returns the value of the max running parameter
	 * @return
	 */
	public int getMaxRunning() {
		return this.maxRunning;
	}
	
	/**
	 * Ends the checking if this XML Task can spawn new elements (is set when it has only global dependencies or the same number of tasks was spawned in separate mode)
	 */
	public void endCheckingForNewTasks() {
		this.hasXMLTaskSpawnedAllTasks = true;
		LOGGER.info("XMLTask with ID '" + this.getXMLID() + "' will not spawn any more tasks. It spawned " + this.SPAWNED_TASKS.size() + " tasks!");
	}
	
	/**
	 * Checks, if all spawned elements of this task has been finished successfully
	 * @return
	 */
	public boolean isCompleteTaskReady() {	
		// some tasks will be spawned later
		if(this.hasXMLTaskSpawnedAllTasks == false)
			return false;
		//check, if some of the spawned tasks are still running
		for(Task t : this.getExecutionTasks().values()) {
			if(!t.isTaskIgnored() && !t.hasTaskFinished())
				return false;
		}
		return true;
	}
	
	/**
	 * checks, if some of the tasks are ignored
	 * @return
	 */
	public boolean isSomeTaskIgnored() {
		for(Task t : this.getExecutionTasks().values()) {
			if(t.isTaskIgnored())
				return true;
		}
		return false || this.isIgnored;
	}
	
	/**
	 * checks, if the processing of this task is finished regardless of the status of the job
	 * @return
	 */
	public boolean isProcessingOfTaskFinished() {
		// if, task is currently blocked, it is not finished!
		if(this.isBlocked())
			return false;
		
		// some tasks will be spawned later
		if(this.hasXMLTaskSpawnedAllTasks == false && this.isIgnored == false)
			return false;

		//check, if some of the spawned tasks are still running
		for(Task t : this.getExecutionTasks().values()) {
			if(!t.isTaskIgnored() && (t.getExecutionCounter() == 0 || t.isBlocked() || !(t.hasTaskFinished())))
				return false;
		}
		return true;
	}
	
	public boolean isSomeTaskBlocked() {
		//check, if some of the spawned tasks are blocked
		for(Task t : this.getExecutionTasks().values()) {
			if(!t.isTaskIgnored() && t.getExecutionCounter() > 0 && t.isBlocked())
				return true;
		}
		return false;
	}
	
	/**
	 * Informs about a finished process block
	 */
	public void inform() {
		if(Task.getMailer() != null && this.hasProcessBlock()) {
			// check, if any of the tasks is in status check mode wait with inform until the check is finished!
			for(Task t : this.SPAWNED_TASKS.values()) {
				if(!t.isTaskStatusUpdateFinished() || TaskStatus.STATUS_CHECK.equals(t.getStatus()))
					return;
			}
			
			if(this.hasFailedTasks() || this.getCheckpoint().isEnabled() || (this.notify != null && !this.notify.isDisabled())) {
				if(!this.notify.wasPerformed()) {
					ArrayList<Task> tasks = new ArrayList<>();
					
					for(String key : this.SPAWNED_TASKS.keySet()) {
						tasks.add(this.SPAWNED_TASKS.get(key));
					}
					Task.getMailer().inform(new ArrayList<Task>(tasks));
					this.setNotify(ActionType.PERFORMED);
				}
			}		
		}
	}
	
	/**
	 * Checks, if there are some tasks which were not executed yet
	 * @return
	 */
	public boolean hasRunningTasks() {
		for(Task t : this.SPAWNED_TASKS.values()) {
			if(!t.hasJobInfo())
				return true;
		}
		return false;
	}
	
	/**
	 * Checks, if there are some tasks which failed
	 * @return
	 */
	public boolean hasFailedTasks() {
		for(Task t : this.SPAWNED_TASKS.values()) {
			if(t.hasJobInfo() &&  !t.hasTaskFinishedWithoutBlockingInfo())
				return true;
		}
		return false;
	}
	
	/**
	 * tests, if a task with that input filename was already spawned
	 * @param inputFilename
	 * @return
	 */
	public boolean hasTaskAlreayBeenSpawned(String inputFilename) {
		return this.SPAWNED_TASKS.containsKey(inputFilename);
	}
	
	/**
	 * number of tasks which were spawned by this XML task
	 * @return
	 */
	public int getNumberOfSpawnedTasks() {
		return this.SPAWNED_TASKS.size();
	}
	
	/**
	 * 
	 * @return
	 */
	public HashMap<String, Integer> getSummedStatus() {
		HashMap<String, Integer> status = new HashMap<>();
		
		for(Task t : this.SPAWNED_TASKS.values()) {
			String s = t.getStatus().toString();
			if(!status.containsKey(s))
				status.put(s, 0);
			
			// update the value
			status.put(s, status.get(s)+1);
		}
		return status;
	}
	
	/**
	 * Checks, if a process block task has been finished successfully
	 * @param inputFilename
	 * @param prefixLength
	 * @param separator
	 * @return
	 */
	public boolean isProcessBlockTaskReady(String inputFilename, int prefixLength, String separator, XMLTask successor) {
		// is needed for check without inputFilename
		if(inputFilename == null)
			return false;
		
		// get only file name and ignore path
		String fullName = inputFilename;
		inputFilename = new File(inputFilename).getName();
		
		//check, if task was already spawned and has finished
		for(Task t : this.getExecutionTasks().values()) {
			// task has been finished
			if(t.hasTaskFinished()) {
				String prefixOfFinishedTask = new File(de.lmu.ifi.bio.watchdog.helper.Functions.getPrefixName(new File(t.getGroupFileName()).getName(), prefixLength, separator)).getName();
				//LOGGER.info("Prefix of finished task: '"+ prefixOfFinishedTask +"' vs. '" + inputFilename + "'");
				// check for correct prefix
				if(inputFilename.startsWith(prefixOfFinishedTask)) {
					// save the slave ID of that task if one is set
					if(successor != null && t.getSlaveID() != null && successor.getExecutor().getName().equals(t.getExecutor().getName()) && !successor.SLAVE_IDS.containsKey(fullName) && !t.isSingleSlaveModeForced()) {
						successor.SLAVE_IDS.put(fullName, t.getSlaveID());
					}
					return true;
				}
			}
		}	
		return false;
	}
		
	public String getSlaveIDOfDependencies(String inputFilename) {
		return this.SLAVE_IDS.get(inputFilename);
	}
	
	/**
	 * arguments from cache if they are already there --> needed to get correct 'X' for replacement!
	 * @param inputReplacement
	 * @param nameMapping
	 * @return
	 */
	public LinkedHashMap<String, Pair<Pair<String, String>, String>> getArguments(String inputReplacement, HashMap<String, Integer> nameMapping) {
		return this.getArguments(inputReplacement, nameMapping, false);
	}
	
	/**
	 * If inputReplacement is a valid File the function tries to replace {} with the path of the file
	 * Using {n} results in cropping of n endings (f.e. test.bam.bak with {1} --> test.bam}
	 * @param inputReplacement
	 * @param name mapping for process table
	 * @return
	 */
	public LinkedHashMap<String, Pair<Pair<String, String>, String>> getArguments(String inputReplacement, HashMap<String, Integer> nameMapping, boolean enforceRecalculation) {
		LinkedHashMap<String, Pair<Pair<String, String>, String>> args = new LinkedHashMap<>();
		// check, if result is cached
		if(!enforceRecalculation && this.CACHE.containsKey(inputReplacement))
			args = this.CACHE.get(inputReplacement); 
		else {
			HashMap<String, String> params = this.replace(inputReplacement, nameMapping); // replace [], {}, (), $() and other variables
			
			for(String name : params.keySet()) {
				 // flag
				if(this.PARAM_IS_FLAG.get(name)) {
					args.put(name, Pair.of(Pair.of(this.OPTION_FORMAT.getFlagPrefix(), this.OPTION_FORMAT.getQuote()), null));
				}
				//parameter 
				else {
					String value = params.get(name);
	
					// decide which option formater to use
					OptionFormat formater = null;
					if(this.PARAM_FORMATER.containsKey(name)) {
						formater = this.PARAM_FORMATER.get(name);
					}
					else {
						formater = this.OPTION_FORMAT;
					}
					
					// add the parameter and value
					args.put(name, Pair.of(Pair.of(formater.getParamPrefix() == null ? "" : formater.getParamPrefix(), formater.getQuote() == null ? "" : formater.getQuote()), value));
				}
			}		
			// check, if a return parameter file must be added
			if(this.returnParamName != null) {
				File tmp = Functions.generateRandomTmpExecutionFile(RETURN_PARAM, false);
				this.RETURN_FILE_NAME.put(inputReplacement, tmp);
				args.put(this.returnParamName, Pair.of(Pair.of(this.OPTION_FORMAT.getParamPrefix(), this.OPTION_FORMAT.getQuote()), tmp.getAbsolutePath()));
			}
			// store it
			this.CACHE.put(inputReplacement, args);
		}
		return args;
	}
	
	/**
	 * Gets the arguments as they are without replacement --> single process mode
	 * @return
	 */
	public LinkedHashMap<String, Pair<Pair<String, String>, String>> getArguments() {
		return this.getArguments(null, null);
	}
	
	/**
	 * Gets the binary which should be executed by the job
	 * @return
	 */
	public String getBinaryCall() {
		return this.BIN_NAME;
	}	
	
	public synchronized static boolean hasXMLTask(int id) {
		return XMLTask.XML_TASKS.containsKey(id);
	}
	
	public synchronized static XMLTask getXMLTask(int id) {
		return XMLTask.XML_TASKS.get(id);
	}

	/**
	 * sets an input stream
	 * @param inputStream
	 */
	public void setInputStream(String inputStream) {
		this.stdInString = inputStream;
	}
	
	/**
	 * sets an output stream
	 * @param outputStream
	 * @param append
	 */
	public void setOutputStream(String outputStream, boolean append) {
		this.stdOutString = outputStream;
		this.stdOutAppend = append;
	}
	

	
	/**
	 * sets an error stream
	 * @param errorStream
	 * @param append
	 */
	public void setErrorStream(String errorStream, boolean append) {
		this.stdErrString = errorStream;
		this.stdErrAppend = append;
	}	

	/**
	 * sets a working directory
	 * @param workingDirectory
	 */
	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectoryString = workingDirectory;
	}
	
	/**
	 * replaces the vars for stderr, stdout, stdin and the working directory
	 * @param replacement
	 * @param var
	 * @return
	 */
	private File getFile(String replacement, String var) {
		if(var != null && var.length() > 0) {
			String path = var;
			// replace the var if some replace value is given
			if(replacement != null && replacement.length() > 0)
				path = ReplaceSpecialConstructs.replaceValues(var, replacement, this.getProcessBlock().getClass(), this.SPAWNED_TASKS.size()+1, this.getProcessBlock() instanceof ProcessMultiParam ? ((ProcessMultiParam) this.getProcessBlock()).getNameMapping() : null, this.getExecutor().getWorkingDir(), false, this.mightProcessblockContainFilenames());

			// check, if a absolute path is set or a relative one
			if(!path.startsWith(File.separator) && !path.matches("[A-Z]:\\\\.+") && this.getWorkingDir(replacement) != null) {
				path = this.getWorkingDir(replacement) + File.separator + path; 
			}
			return new File(path);
		}
		return null;
	}
	
	/**
	 * Returns a file for stdin or null if none is set
	 * @param replacement
	 * @return
	 */
	public File getStdIn(String replacement) {
		return this.getFile(replacement, this.stdInString);
	}
	
	/**
	 * Returns a file for stderr or null if none is set
	 * @param replacement
	 * @return
	 */
	public File getStdErr(String replacement) {
		return this.getFile(replacement, this.stdErrString);
	}
	
	/**
	 * Returns a file for stdout or null if none is set
	 * @param replacement 
	 * @return
	 */
	public File getStdOut(String replacement) {
		return this.getFile(replacement, this.stdOutString);
	}
	
	/**
	 * Returns the current working directory
	 * @param replacement 
	 * @return
	 */
	public File getWorkingDir(String replacement) {
		File work = this.getFile(replacement, this.workingDirectoryString);
		if(work == null)
			return new File(Executor.default_working_dir);
		else 
			return work;
	}
	
	/**
	 * true, if stdout should be appended if the file is already existing
	 * @return
	 */
	public boolean isOutputAppended() {
		return this.stdOutAppend;
	}
	
	/**
	 * true, if stderr should be appended if the file is already existing
	 * @return
	 */
	public boolean isErrorAppended() {
		return this.stdErrAppend;
	}

	/**
	 * retuens the executor info
	 * @return
	 */
	public ExecutorInfo getExecutor() {
		return this.EXECUTOR_INFO;
	}

	/**
	 * sets a new notify type
	 * @param notify
	 */
	public void setNotify(ActionType notify) {
		this.notify = notify;
		
		// store information for later, if task must be reseted.
			if(this.notifyBackup == null)
				this.notifyBackup = this.notify;
	}

	/**
	 * sets a new checkpoint type
	 * @param checkpoint
	 */
	public void setCheckpoint(ActionType checkpoint) {
		this.checkpoint = checkpoint;
	}
	
	/**
	 * sets a new checkpoint type
	 * @param confirmParam
	 */
	public void setConfirmParam(ActionType confirmParam) {
		this.confirmParam = confirmParam;
	}

	/**
	 * returns the set notify type
	 * @return
	 */
	public ActionType getNotify() {
		return this.notify;
	}
	
	/**
	 * returns the set checkpoint type
	 * @return
	 */
	public ActionType getCheckpoint() {
		return this.checkpoint;
	}
	
	/**
	 * returns the set confirmParam type
	 * @return
	 */
	public ActionType getConfirmParam() {
		return this.confirmParam;
	}
	

	/**
	 * removes a dependency from that task for include / exclude
	 * @param depID
	 * @return
	 */
	public boolean removeDependencies(int depID) {
		// test if dependecy can be removed
		if(this.DEPENDS.containsKey(depID)) {
			this.DEPENDS.remove(depID);
			this.DEPENDS_SEP.remove(depID);
			this.DEPENDS_SEP_KEEP.remove(depID);
			this.AVAIL_RETURN_PARAMS.remove(depID);
			this.ORIGINAL_GLOBAL_DEPENDENCIES.remove(depID);
			// test, if result is needed for that block as input
			return this.isDependencyNeededForReturnInfo(depID);
		}
		return false;
	}


	/**
	 * Removes all dependencies with IDs smaller or greater than that id
	 * @param cutID
	 * @param smaller
	 */
	public boolean removeDependenciesCut(int cutID, boolean smaller) {
		boolean removeTask = false;
		for(int id : new ArrayList<Integer>(this.DEPENDS.keySet())) {
			if(smaller) {
				if(id < cutID) {
					removeTask = this.removeDependencies(id) | removeTask;
				}
			}
			else if(id > cutID) { 
				removeTask = this.removeDependencies(id) | removeTask;
			}
		}
		return removeTask;
	}
	
	/**
	 * returns the plain arguments
	 * @param inputReplacement
	 * @return
	 */
	public LinkedHashMap<String, String> getPlainArguments(String inputReplacement) {
		// get the name mapping
		HashMap<String, Integer> nameMapping = null;
		if(this.getProcessBlock() instanceof ProcessMultiParam)
			nameMapping = ((ProcessMultiParam) this.getProcessBlock()).getNameMapping();
		
		// replace the params
		return this.replace(inputReplacement, nameMapping);
	}

	/**
	 * Replaces {}, [] and other place holders in the variables
	 * @param inputReplacement
	 */
	protected LinkedHashMap<String, String> replace(String inputReplacement, HashMap<String, Integer> nameMapping) {
		LinkedHashMap<String, String> params = new LinkedHashMap<>();

		// run through all parameters and flags
		for(String name : this.PARAM.keySet()) {
			String value = this.PARAM.get(name);
			OptionFormat f = this.PARAM_FORMATER.get(name);
			String sep = null;
			if(f != null)
				sep = f.getSeparateString();
			else if(this.OPTION_FORMAT != null)
				sep = this.OPTION_FORMAT.getSeparateString();
			else
				sep = XMLTask.DEFAULT_PARAM_SEP;
			
			// replace internal separator with real separator
			if(value != null)
				value = value.replace(INTERNAL_PARAM_SEP, sep);
			
			// only try to replace parameters
			if(inputReplacement != null && !this.PARAM_IS_FLAG.get(name)) {
				value = this.replaceString(value, inputReplacement, nameMapping);
			}

			// add the value
			params.put(name, value);
		}
		return params;
	}
	
	/**
	 * replaces the value of a single parameter
	 * @param value
	 * @param inputReplacement
	 * @param nameMapping
	 * @return
	 */
	public String replaceString(String value, String inputReplacement, HashMap<String, Integer> nameMapping) {
		if(inputReplacement != null)
			return ReplaceSpecialConstructs.replaceValues(value, inputReplacement, this.getProcessBlock() != null ? this.getProcessBlock().getClass() : null, this.SPAWNED_TASKS.size()+1, nameMapping, this.getExecutor().getWorkingDir(), false, this.mightProcessblockContainFilenames());
		
		return value;
	}
	
	/**
	 * changes, the parameter of a XML task.
	 * this is only possible if the task is currently not running
	 * @param params
	 * @param t task for which the parameters should be changed. if null it is changed for all tasks
	 * @param confirmationOnly if true, no changes are made to the task, it is only released
	 * @return
	 */
	public boolean modifyParamsAndReschedule(HashMap<String, String> params, Task t, boolean confirmationOnly) {
		if(!this.blockUntilSchedule) {
			// do nothing if the confirmation button was clicked
			if(!confirmationOnly) {
				this.blockUntilSchedule = true; // allow only one change and then force a rescheduling
				
				// clear the cached argument lists and notify
				this.CACHE.clear();
				this.notify = this.notifyBackup;
				
				// backup the stuff in order to restore it later
				this.PARAM_GLOBAL.putAll(this.PARAM);
				this.PARAM_IS_FLAG_GLOBAL.putAll(this.PARAM_IS_FLAG);
				this.PARAM_FORMATER_GLOBAL.putAll(this.PARAM_FORMATER);
				
				HashSet<String> keys = new HashSet<String>(this.PARAM.keySet());
				// update the parameters
				for(String name : params.keySet()) {
					String value = params.get(name);
					this.PARAM.put(name, value);
					this.PARAM_IS_FLAG.put(name, HTTPListenerThread.CHECK_BOX_IDENTIFIER.equals(value));
					keys.remove(name);
				}
				
				// delete parameters which are not included in the HashMap
				for(String del : keys) {
					this.PARAM.remove(del);
					this.PARAM_IS_FLAG.remove(del);
					this.PARAM_FORMATER.remove(del);
				}
				
				// let this task be rescheduled --> param confirmation took place automatically! 
				if(t != null) {
					this.RESCHEDULE.add(t.getGroupFileName());
								
					// halt execution and reschedule the stuff
					this.resetXMLTask(t);
				}
				// was parameter confirmation for complete XMLTask
				else {
					this.RESCHEDULE.add(null); 
					// halt execution and reschedule all tasks that were FAILED
					this.resetXMLTask(null);
				}
			}
			this.unblock();
			return true;
		}
		return false;
	}
	
	/**
	 * resets a XML task
	 * @param reset re-schedules this task of all if the object is set to null
	 */
	public void resetXMLTask(Task reset) {
		
		if(reset == null) {
			// terminate all tasks
			for(Task t : this.SPAWNED_TASKS.values()) {
				if((t.hasTaskFailed() || t.wasTaskKilled()) && !t.isTaskIgnored()) {
					t.terminateTask();
					t.deleteLogFiles();
					this.SPAWNED_TASKS.remove(t.getGroupFileName());
					this.CACHE.remove(t.getGroupFileName());
					this.RETURN_FILE_NAME.remove(t.getGroupFileName());
				}
			}
		}
		else {
			reset.terminateTask();
			reset.deleteLogFiles();
			this.SPAWNED_TASKS.remove(reset.getGroupFileName());
			this.CACHE.remove(reset.getGroupFileName());
			this.RETURN_FILE_NAME.remove(reset.getGroupFileName());
		}
		
		// force rescheduling
		this.hasXMLTaskSpawnedAllTasks = false;
	}

	/**
	 * should be called from outside when scheduling was performed
	 */
	public void schedulingWasPerformed() {
		this.blockUntilSchedule = false;
		
		// parameter was changed before
		if(this.PARAM_GLOBAL.size() > 0) {
			// reset original params
			this.PARAM.putAll(this.PARAM_GLOBAL);
			this.PARAM_IS_FLAG.putAll(this.PARAM_IS_FLAG_GLOBAL);
			this.PARAM_FORMATER.putAll(this.PARAM_FORMATER_GLOBAL);
			
			// delete the references to the hash maps
			this.PARAM_GLOBAL.clear();
			this.PARAM_IS_FLAG_GLOBAL.clear();
			this.PARAM_FORMATER_GLOBAL.clear();
		}
	}
	
	/**
	 * returns the param list of this XML task
	 * @return
	 */
	public LinkedHashMap<String, String> getParamList() {
		if(this.NO_GUI_LOAD_PARAM.size() == 0)
			return new LinkedHashMap<String, String>(this.PARAM);
		else {
			LinkedHashMap<String, String> t = new LinkedHashMap<>();
			for(String k : this.PARAM.keySet()) {
				if(!NO_GUI_LOAD_PARAM.contains(k))
					t.put(k, this.PARAM.get(k));
			}
			return t;
		}
	}
	
	/**
	 * returns the is flag settings of the params
	 * @return
	 */
	public HashMap<String, Boolean> getParamFlag() {
		return new HashMap<String, Boolean>(this.PARAM_IS_FLAG);
	}
	
	public void block() {
		this.block = true;
	}
	
	public void unblock() {
		this.block = false;
	}
	
	public boolean isBlocked() {
		return this.block;
	}

	public int getBlockCount() {
		return this.PROCESS_BLOCK == null ? 1 : this.PROCESS_BLOCK.size();
	}

	public TreeSet<Integer> getOriginalGlobalDependencies() {
		return new TreeSet<Integer>(this.ORIGINAL_GLOBAL_DEPENDENCIES);
	}
	
	public void clearRescheduled() {
		this.RESCHEDULE.clear();
	}
	
	public boolean isRescheduled(String inputName) {
		return this.RESCHEDULE.contains(inputName);
	}
	
	public boolean isRescheduled() {
		return this.RESCHEDULE.size() > 0;
	}
	
	
	/**
	 * sets new environment values but does not remove the old one
	 * @param environment
	 */
	public void setEnvironment(Environment environment) {
		this.env = environment;
	}
	
	public Environment getEnvironment() {
		return this.env != null ? this.env : (this.getExecutor() != null ? this.getExecutor().getEnv() : null); 
	}
	
	/**
	 * sets a global preeceding slave ID
	 * can be used only once!
	 * @param t
	 * @return
	 */
	private void setGlobalSlaveId(Task t) {
		if(!this.hasGlobalPrevSlaveId())
			this.prev_global_slave_id = t.getSlaveID();
	}
	
	private boolean hasGlobalPrevSlaveId() {
		return this.prev_global_slave_id != null;
	}
	
	/**
	 * returns the ID of slave jobs that are already finished
	 * if there is any separate dependency that has been executed on a slave executer this ID is returned also if some global IDs might be availible.
	 * @return
	 */
	public String getGlobalPrevSlaveId() {
		return this.prev_global_slave_id;
	}

	/**
	 * delete the slave task ID in case a task was finished sucessfully!
	 * @param t
	 */
	public synchronized static void deleteSlaveID(Task t) {
		if(t != null) {
			XMLTask x = XMLTask.getXMLTask(t.getTaskID());
			if(x != null)
				x.SLAVE_IDS.remove(t.getGroupFileName());
		}
	}

	/**
	 * retusn the IDs of slave tasks
	 * @return
	 */
	public synchronized HashMap<String, String> getSlaveIDS() {
		HashMap<String, String> ret = new HashMap<>();
		ret.putAll(this.SLAVE_IDS);
		return ret;
	}

	public synchronized static HashMap<Integer, XMLTask> getXMLTasks() {
		HashMap<Integer, XMLTask> ret = new HashMap<>();
		ret.putAll(XMLTask.XML_TASKS);
		return ret;
	}

	public void addTaskActions(ArrayList<TaskAction> taskActions) {
		this.TASK_ACTIONS.addAll(taskActions);
	}

	/**
	 * 
	 * @param inputReplacement
	 * @return
	 */
	public ArrayList<TaskAction> getTaskActions(String id, String inputReplacement, HashMap<String, Integer> nameMapping) {
		ArrayList<TaskAction> res = new ArrayList<>();
		
		// create copies of the task actions for a new task
		for(TaskAction a : this.TASK_ACTIONS) {
			if(a instanceof CopyTaskAction) {
				CopyTaskAction c = (CopyTaskAction) a;
				String src = this.replaceString(c.getSrc(), inputReplacement, nameMapping);
				String dest = this.replaceString(c.getDest(), inputReplacement, nameMapping);
				String pattern = this.replaceString(c.getPattern(), inputReplacement, nameMapping);
				res.add(new CopyTaskAction(src, dest, pattern, c));
			}
			else if(a instanceof CreateTaskAction) {
				CreateTaskAction c = (CreateTaskAction) a;
				String path = this.replaceString(c.getPath(), inputReplacement, nameMapping);
				res.add(new CreateTaskAction(path, c));
			} 
			else if(a instanceof DeleteTaskAction) {
				DeleteTaskAction d = (DeleteTaskAction) a;
				String path = this.replaceString(d.getPath(), inputReplacement, nameMapping);
				String pattern = this.replaceString(d.getPattern(), inputReplacement, nameMapping);
				res.add(new DeleteTaskAction(path, pattern, d));
			}
			else {
				try { throw new IllegalArgumentException("For action task of type class '" + a.getErrors() + "' no variable replacement is implemented."); }
				catch(Exception e) {
					LOGGER.error(e.getMessage());
					System.exit(1);
				}
			}
		}
		// return the changed results
		return res; 
	}
	
	public ArrayList<ErrorCheckerStore> getCheckers() {
		ArrayList<ErrorCheckerStore> a = new ArrayList<>();
		for(CheckerContainer c : this.CHECKER)
			a.add(c.convert2ErrorCheckerStore());
		return a;
	}
	
	public ArrayList<TaskAction> getTaskActions() {
		ArrayList<TaskAction> a = new ArrayList<>();
		for(TaskAction aa : this.TASK_ACTIONS)
			a.add(aa);
		return a;
	}

	/**
	 * forces the single slave mode in cases in which transfers should be made
	 * @param forceSingleSlaveMode
	 */
	public void setForceSingleSlaveMode(boolean forceSingleSlaveMode) {
		this.forceSingleSlaveMode = forceSingleSlaveMode;
	}
	
	/**
	 * can be set to force single slave mode
	 * @return
	 */
	public boolean isSingleSlaveModeForced() {
		return this.forceSingleSlaveMode;
	}

	/**
	 * tests, if any of the dependencies contains a return variable with that name that can be used as processInput
	 * @param usedReturnName
	 * @return
	 */
	public boolean isReturnVariableAvail(String usedReturnName) {
		for(HashSet<String> retVals : this.AVAIL_RETURN_PARAMS.values()) {
			if(retVals.contains(usedReturnName))
				return true;
		}
		return false;
	}
	
	/**
	 * true, if dependency is needed for return parameters!
	 * @param depID
	 * @return
	 */
	public boolean isDependencyNeededForReturnInfo(int depID) {
		return this.getProcessBlock() != null && this.getProcessBlock().addsReturnInfoToTasks() && this.NEEDED_DEPENDENCIES_4_VARS.contains(depID);
	}

	/**
	 * returns the ID of the task that contains that return variable or -1 if none does
	 * @param usedReturnName
	 * @return
	 */
	public int getTaskBasedOnNameOfReturnVariable(String usedReturnName) {
		for(int id : this.AVAIL_RETURN_PARAMS.keySet()) {
			HashSet<String> retVals = this.AVAIL_RETURN_PARAMS.get(id);
			if(retVals.contains(usedReturnName))
				return id;
		}
		return -1;
	}

	public void setDisableExistenceCheckStdin(boolean disableExistenceCheck) {
		this.disableExistenceCheckStdin = disableExistenceCheck;
	}
	
	public boolean isStdinExistenceDisabled() {
		return this.disableExistenceCheckStdin;
	}
	
	public GUIInfo getGuiInfo() {
		return guiInfo;
	}

	public void setGuiInfo(GUIInfo guiInfo) {
		this.guiInfo = guiInfo;
	}
	
	public String getPlainStdOut() {
		return this.stdOutString;
	}
	public String getPlainStdErr() {
		return this.stdErrString;
	}
	public String getPlainStdIn() {
		return this.stdInString;
	}
	public String getPlainWorkingDir() {
		if(this.workingDirectoryString == null || this.workingDirectoryString.length() == 0)
			return WatchdogThread.DEFAULT_WORKDIR;
		return this.workingDirectoryString;
	}
	
	public void releaseAllTasksFromCheckpoint() {
		for(Task t : this.SPAWNED_TASKS.values()) 
			t.releaseTask();
	}

	public void setSaveResourceUsage(boolean save) {
		this.setSaveResourceUsage = save;
	}
	
	/**
	 * returns true, if flag is enabled and stdout is set
	 * @return
	 */
	public boolean isSaveResourceUsageEnabled() {
		return this.setSaveResourceUsage && this.getPlainStdOut() != null;
	}

	public int getVersion() {
		return this.VERSION;
	}

	public void addNoGUILoadParameter(String name, String value, OptionFormat formater, int depID) {
		this.NO_GUI_LOAD_PARAM.add(name);
		this.addParameter(name, value, formater, depID);
	}
}
