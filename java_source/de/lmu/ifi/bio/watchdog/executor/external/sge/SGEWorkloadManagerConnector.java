package de.lmu.ifi.bio.watchdog.executor.external.sge;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobInfo;
import org.ggf.drmaa.TryLaterException;

import de.lmu.ifi.bio.watchdog.executor.external.BinaryCallBasedExternalWorkflowManagerConnector;
import de.lmu.ifi.bio.watchdog.executor.external.BinaryCallInfo;
import de.lmu.ifi.bio.watchdog.executor.external.GenericJobInfo;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.task.Task;

/**
 * 
 */
public class SGEWorkloadManagerConnector extends BinaryCallBasedExternalWorkflowManagerConnector<SGEExecutor> {

	public static final String EXECUTOR_NAME = "sge";
	private static final String HOSTNAME_REGEX = ".*<JG_qhostname>(.+)</JG_qhostname>.*";
	private static final String ID_REGEX = "Your job ([0-9]+) \\(\".+\"\\) has been submitted";
	private static final String STATE_QSTAT = "^\\W*([0-9]+)\\W+.+\\W+(\\S+)\\W+([0-9]{2})/([0-9]{2})/([0-9]{4})\\W+.*";
	private static final Pattern SPLIT_PATTERN = Pattern.compile("\\W+");
	private static final String RUNNING = "r";
	private static final String TRANSFERING = "t";
	private static final String HOLD = "hqw";
	private static final String PENDING = "qw";
	private static final String GOOD_STATUS = PENDING+"|"+TRANSFERING+"|"+RUNNING+"|"+HOLD;
	private static final Pattern ID_REGEX_PATTERN = Pattern.compile(ID_REGEX);
	private static final Pattern HOSTNAME_REGEX_PATTERN = Pattern.compile(HOSTNAME_REGEX);
	private static final HashSet<String> INFO_FIELDS = new HashSet<>();
	private static final String EXIT_CODE_FIELD = "exit_status";
	private static final String FAILED_FIELD = "failed";
	private static final String ID_REP = "#<ID>#";
	private static final String ID_NOT_KNOWN = "error: job id "+ID_REP+" not found";
	private static final int MAX_QACCT_TRIES = 25;
	private static final int MAX_WAIT_CYLCES = 3;  // wait 3 cycles after job submission
	private final Pattern STATE_QSTAT_PATTERN = Pattern.compile(STATE_QSTAT);
	private final static int LONG_WAIT = 5000; // don't query to often
	
	public HashMap<String, String> NOT_KNOWN_MATCH = new HashMap<>(); // used to test, if the ID is NOT known by the GRID system anymore
	public HashMap<String, Integer> QACCT_TRIES = new HashMap<>(); // stores how often qacct was called for each job in oder to get the requiered termination info
	
	private boolean isInitComplete = false;
	
	static {
		// defined key entries that are used as resources
		INFO_FIELDS.add("granted_pe");
		INFO_FIELDS.add("slots");
		INFO_FIELDS.add("ru_wallclock");
		INFO_FIELDS.add("maxvmem");
		INFO_FIELDS.add("io");
		INFO_FIELDS.add("iow");
		INFO_FIELDS.add("mem");
		INFO_FIELDS.add("cpu");
	}
	
	public SGEWorkloadManagerConnector(Logger l) {
		super(l);
	}
	
	private void ensurePreparedPatternAreThere(String id) {
		if(!this.NOT_KNOWN_MATCH.containsKey(id)) {
			this.prepareRunningPattern(id);
		}
	}

	private void prepareRunningPattern(String id) {
		this.NOT_KNOWN_MATCH.put(id, ID_NOT_KNOWN.replace(ID_REP, id));
	}

	@Override
	public synchronized String submitJob(Task task, SGEExecutor ex) throws DrmaaException {
		ArrayList<String> args = new ArrayList<>();
		SGEExecutorInfo execinfo = ex.getExecutorInfo();

		// set stdout
		if(task.getStdOut(false) != null) {
			File out = task.getStdOut(true);
			args.add("-o"); 
			args.add(out.getAbsolutePath()); 
			if(!task.isOutputAppended())
				out.delete();
		}
		else {
			args.add("-o"); 
			args.add(DEV_NULL);
		}
		// set stderr
		if(task.getStdErr(false) != null) {
			File err = task.getStdErr(true);
			args.add("-e"); 
			args.add(err.getAbsolutePath()); 
			if(!task.isErrorAppended())
				err.delete();
		}
		else {
			args.add("-e"); 
			args.add(DEV_NULL); 
		}
		// set stdin
		if(task.getStdIn() != null) {
			args.add("-i"); 
			args.add(task.getStdIn().getAbsolutePath()); 
		}
		
		// set the working directory
		String wd = ex.getWorkingDir(false); // TODO
		//if(wd != null) {
		//	args.add("-wd");
//			args.add(wd);
	//	}
		
		// set on hold if required
		if(task.isTaskOnHold()) 
			args.add("-h"); 

		args.add("-N"); args.add(task.getProjectShortCut() + " " + task.getName() + " " + task.getID());		
		// add queue, memory, slots and custom parameter
		args.addAll(execinfo.getCommandsForGrid());
		
		// set the environment variables that should not be set by an external command
		if(ex.hasInternalEnvVars()) {
			HashMap<String, String> env = ex.getInternalEnvVars();
			for(String key : env.keySet()) {
				args.add("-v"); 
				args.add(env.get(key)); 
			}
		}

		// add command
		args.add("-b");
		args.add("y");
		for(String part : ex.getFinalCommand(false, false))
			args.add(part);

		BinaryCallInfo info = this.executeCommand("qsub", args);
		if(info.exit != 0) {
			info.printInfo(this.LOGGER, true);
			System.exit(1);
		}
		else {
			// try to find id
			Matcher m = ID_REGEX_PATTERN.matcher(info.out);
			if(m.find()) {
				String id = m.group(1);
				this.prepareRunningPattern(id);
				this.SUBMITTED_JOB_STATUS.put(id, 0);
				return id;
			}
			else {
				this.LOGGER.error("Job ID not found in stdout of qstat!");
				info.printInfo(this.LOGGER, true);
				System.exit(1);
			}
		}
		return null;
	}
	
	@Override
	public long getDefaultWaitTime() {
		return LONG_WAIT;
	}

	@Override
	public void releaseJob(String id) throws DrmaaException {
		ArrayList<String> args = new ArrayList<>();
		args.add(id);
		executeCommand("qrls", args);
	}

	@Override
	public void holdJob(String id) throws DrmaaException {
		ArrayList<String> args = new ArrayList<>();
		args.add(id);
		executeCommand("qhold", args);
	}

	@Override
	public void cancelJob(String id) throws DrmaaException {
		ArrayList<String> args = new ArrayList<>();
		args.add(id);
		executeCommand("qdel", args);
	}

	@Override
	public boolean isJobRunning(String id) throws DrmaaException {
		if(!this.CACHED_JOB_STATUS.containsKey(id))
			return false;
		
		return RUNNING.equals(this.CACHED_JOB_STATUS.get(id));
	}
	
	@Override
	protected void updateJobStatusCache() {
		QacctBinaryCallInfo info = new QacctBinaryCallInfo("generic", this.executeCommand("qstat", new ArrayList<String>()));
		if(info.exit != 0) {
			info.printInfo(this.LOGGER, true);
			System.exit(1);
		}
		else {
			// try to find ids
			Scanner s = new Scanner(info.out);
			String id, status;
			while(s.hasNextLine()) {
				Matcher m = STATE_QSTAT_PATTERN.matcher(s.nextLine());
				if(m.matches()) {
					id = m.group(1);
					status = m.group(2);
					this.CACHED_JOB_STATUS.put(id, status);
				}
			}
			s.close();
		}
	}
	
			
	@Override
	public JobInfo getJobInfo(String id) throws DrmaaException {
		// check, if job is running
		if(this.isJobRunning(id))
			return null;
		// check, if job is in a "running-like" state
		String runningInfo = this.CACHED_JOB_STATUS.get(id);
		if(runningInfo != null && (PENDING.equals(runningInfo) || RUNNING.equals(runningInfo) || HOLD.equals(runningInfo) || TRANSFERING.equals(runningInfo)))
			return null;

		this.ensurePreparedPatternAreThere(id);
		// we don't want block with this call until flushing for qacct is ready as it is really slow 
		// (and it depends on the GRID config see: reporting_params; flush_time; sge_conf;)
		// init qacct tires
		if(!this.QACCT_TRIES.containsKey(id))
			this.QACCT_TRIES.put(id, 0);
		
		// values we wan't extract
		HashMap<String, String> res = new HashMap<>();
		int exitCode = Integer.MIN_VALUE;
		int signal = Integer.MIN_VALUE;
		String failed = null; 
		
		// arguments for qacct call
		ArrayList<String> args = new ArrayList<>();
		args.add("-j");
		args.add(id);
				
		// get exit code and resource info
		BinaryCallInfo info = this.executeCommand("qacct", args);
		
		// test, if id is not known
		if(info.err.contains(NOT_KNOWN_MATCH.get(id))) {
			// update tries
			int tries = this.QACCT_TRIES.get(id)+1;
			this.QACCT_TRIES.put(id, tries);
			if(tries > MAX_QACCT_TRIES) {				
				this.LOGGER.error("Job ID " + id + " was not found in qacct output.");
				info.printInfo(this.LOGGER, true);
				throw new TryLaterException("Too many qacct tries for job '"+id+"'.");
			}
			return null;
		}
		
		Scanner s = new Scanner(info.out);
		String l = null;
		String[] tmp;
		String key, value;
		while(s.hasNextLine()) {
			l = s.nextLine();
			tmp = SPLIT_PATTERN.split(l, 2);
			if(tmp.length == 2) {
				key = tmp[0]; 
				value = tmp[1];
				// failed key values: https://docs.oracle.com/cd/E19957-01/820-0699/chp11-3/index.html
				if(FAILED_FIELD.equals(key)) {
					failed = value;
					
					if(failed.equals("0")) 
						signal = 0;
					else {
						signal = Integer.parseInt(failed.split(" ")[0]);
					}
				}
				else if(EXIT_CODE_FIELD.equals(key)) {
					exitCode = Integer.parseInt(value.split(" ")[0]);
				}
				else if(INFO_FIELDS.contains(key)) {
					res.put(key, value.replaceFirst("[ ]+$", ""));
				}
			}
		}
		s.close();

		// test if all is ok
		if(exitCode == Integer.MIN_VALUE) {
			this.LOGGER.error("Failed to get exit code info from qacct command!");
			info.printInfo(this.LOGGER, true);
			System.exit(1);
		}
		// if all is ok, we can do some cleaning
		this.clean(id);
		return new GenericJobInfo(id, exitCode, signal, true, res);
	}

	@Override
	public void init() {
		executeCommand("qstat"); // test, if qstat works
		this.isInitComplete = true;
	}
	
	/**
	 * removes some information about tasks for which a GenericJobInfo element was found
	 * @param id
	 */
	private void clean(String id) { 
		this.QACCT_TRIES.remove(id);
		this.NOT_KNOWN_MATCH.remove(id);
		this.CACHED_JOB_STATUS.remove(id);
		this.SUBMITTED_JOB_STATUS.remove(id);
	}

	@Override
	public void clean(HashMap<String, SGEExecutor> ids, boolean isInDetachMode) {
		this.isInitComplete = false;
		for(Entry<String, SGEExecutor> job : ids.entrySet()) {
			if(!(isInDetachMode && job.getValue().getExecutorInfo().isWatchdogDetachSupported()))
				try { this.cancelJob(job.getKey()); } catch(Exception e) { e.printStackTrace(); }
		}
		this.QACCT_TRIES .clear();
		this.NOT_KNOWN_MATCH.clear();
		this.CACHED_JOB_STATUS.clear();
		this.SUBMITTED_JOB_STATUS.clear();
	}

	@Override
	public String getNameOfExecutionNode(String id, String watchdogBaseDir) {
		ArrayList<String> args = new ArrayList<>();
		args.add("-xml");
		args.add("-j");
		args.add(id);

		BinaryCallInfo info = this.executeCommand("qstat", args);
		if(info.exit != 0) {
			info.printInfo(this.LOGGER, true);
			System.exit(1);
		}
		else {
			// try to find hostname
			Scanner s = new Scanner(info.out);
			Matcher m = null;
			while(s.hasNextLine()) {
				m = HOSTNAME_REGEX_PATTERN.matcher(s.nextLine());
				if(m.matches()) {
					s.close();
					return m.group(1);
				}
			}
			s.close();
		}
		return null;
	}

	@Override
	public boolean isJobKnownInGridSystem(String id) {
		if(!this.CACHED_JOB_STATUS.containsKey(id))
			return false;
		String info = this.CACHED_JOB_STATUS.get(id);
		return info != null && info.matches(GOOD_STATUS);
	}

	@Override
	public String getExecutorType() {
		return EXECUTOR_NAME;
	}

	@Override
	public boolean isInitComplete() {
		return this.isInitComplete;
	}
	
	protected BinaryCallInfo executeCommand(String command, ArrayList<String> args, File redirectOutput) {
		return this.executeCommand(command, args, null, null, redirectOutput);
	}
	
	protected BinaryCallInfo executeCommand(String command, ArrayList<String> args) {
		return this.executeCommand(command, args, null, null, null);
	}
	
	protected BinaryCallInfo executeCommand(String command) {
		return this.executeCommand(command, new ArrayList<>(), null, null, null);
	}

	@Override
	public int getMaxWaitCyclesUntilJobsIsVisible() {
		return MAX_WAIT_CYLCES;
	}
}