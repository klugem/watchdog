package de.lmu.ifi.bio.watchdog.executor.external.slurm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobInfo;

import de.lmu.ifi.bio.watchdog.executor.Executor;
import de.lmu.ifi.bio.watchdog.executor.external.BinaryCallBasedExternalWorkflowManagerConnector;
import de.lmu.ifi.bio.watchdog.executor.external.BinaryCallInfo;
import de.lmu.ifi.bio.watchdog.executor.external.GenericJobInfo;
import de.lmu.ifi.bio.watchdog.executor.external.sge.QacctBinaryCallInfo;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.resume.AttachInfo;
import de.lmu.ifi.bio.watchdog.task.Task;

public class SlurmWorkloadManagerConnector extends BinaryCallBasedExternalWorkflowManagerConnector<SlurmExecutor> {

	public static final String EXECUTOR_NAME = "slurm";
	private static final String ID_REGEX = "([0-9]+);(.+)";
	private static final Pattern ID_REGEX_PATTERN = Pattern.compile(ID_REGEX);
	private static final String RUNNING = "RUNNING";
	private static final String COMPLETED = "COMPLETED";
	private static final String PENDING = "PENDING";
	private static final String GOOD_STATUS = PENDING+"|"+COMPLETED+"|"+RUNNING;
	private static final String INFO_FIELDS = "JobID,JobName,Partition,NCPUS,State,ExitCode,Elapsed,CPUTime,MaxRSS,MaxVMSize,MaxDiskRead,MaxDiskWrite,ConsumedEnergy,AveCPU,AveVMSize,Submit";
	private static final String EXIT_CODE_FIELD = "ExitCode";
	private boolean isInitComplete = false;
	private static int MAX_WAIT_CYLCES = 3; // wait 3 cycles after job submission
	private final static int LONG_WAIT = 5000; // don't query to often
	private static final String STATE_SACCT = "^([0-9]+)\\|(.+)\\|(.+)";
	private static final String CLUSTER_SEP = "@";
	private final Pattern STATE_SACCT_PATTERN = Pattern.compile(STATE_SACCT); 
	private String start_date_for_query = Functions.getCurrentDateAndTime();
	private static final String SEPARATOR = "=";
	
	public SlurmWorkloadManagerConnector(Logger l) {
		super(l);
	}
	
	/**
	 * sets a custom start date for sacct queries
	 * no check is made if format is correct
	 * @param date
	 */
	public void setStartDateForQuery(String date) {
		this.start_date_for_query = date;
	}
	
	/**
	 * returns a set of formated environment variables with name=value
	 * @return
	 */
	public static ArrayList<String> getFormatedEnvironmentVariables(HashMap<String, String> env) {
		ArrayList<String> e = new ArrayList<>();
		for(String v : env.keySet()) {
			e.add(v + SEPARATOR + env.get(v));
		}
		return e;
	}
	

	@Override
	public synchronized String submitJob(Task task, SlurmExecutor ex) throws DrmaaException {
		ArrayList<String> args = new ArrayList<>();
		args.add("--parsable"); // outputs only the jobid and cluster name (if present), separated by semicolon, only on successful submission.
		SlurmExecutorInfo execinfo = ex.getExecutorInfo();
		
		// set stdout
		if(task.getStdOut(false) != null) {
			File out = task.getStdOut(true);
			args.add("--output"); 
			args.add(out.getAbsolutePath()); 
			if(!task.isOutputAppended())
				out.delete();
		}
		else {
			args.add("--output"); 
			args.add(DEV_NULL);
		}
		// set stderr
		if(task.getStdErr(false) != null) {
			File err = task.getStdErr(true);
			args.add("--error"); 
			args.add(err.getAbsolutePath()); 
			if(!task.isErrorAppended())
				err.delete();
		}
		else {
			args.add("--error"); 
			args.add(DEV_NULL); 
		}
		// set stdin
		if(task.getStdIn() != null) {
			args.add("--input"); 
			args.add(task.getStdIn().getAbsolutePath()); 
		}
		
		// set the working directory
		String wd = ex.getWorkingDir(false);
		if(wd != null) {
		//	args.add("--workdir");
		//	args.add(wd);
		// TODO
		}
		
		// BUG https://bugs.schedmd.com/show_bug.cgi?id=3197 might cause problems in older versions
		if(task.isTaskOnHold()) args.add("--hold"); 
		// BUG
		args.add("--job-name"); args.add(task.getProjectShortCut() + " " + task.getName() + " " + task.getID());
		args.addAll(execinfo.getCommandsForGrid());
		
		// set the environment variables that should not be set by an external command
		if(ex.hasInternalEnvVars()) {
			ArrayList<String> env = getFormatedEnvironmentVariables(ex.getInternalEnvVars());
			String f = Executor.writeStringsToFile(env, true);
			if(f != null) {
				args.add("--export-file"); 
				args.add(f); 
			}
		}
		
		// add final command
		for(String part : ex.getFinalCommand(false, true))
			args.add(part);

		BinaryCallInfo info = this.executeCommand("sbatch", args);
		if(info.exit != 0) {
			info.printInfo(this.LOGGER, true);
			System.exit(1);
		}
		else {
			// try to find id
			Matcher m = ID_REGEX_PATTERN.matcher(info.out);
			if(m.find()) {
				String id = m.group(1);
				String cluster = m.group(2);
				id = this.addClusterInfo(id, cluster);
				this.SUBMITTED_JOB_STATUS.put(id, 0);
				return id;
			}
			else {
				this.LOGGER.error("Job ID was Slurm executor was not found in stdout!");
				info.printInfo(this.LOGGER, true);
				System.exit(1);
			}
		}
		return null;
	}
		
	/**
	 * appends the cluster info to the id
	 * @param id
	 * @param cluster
	 * @return
	 */
	private String addClusterInfo(String id, String cluster) {
		if(cluster != null)
			return id + CLUSTER_SEP + cluster;
		return id;
	}
	
	/**
	 * gets the cluster info from a ID
	 * if no cluster info is there, null is returned
	 * @param id
	 * @param execinfo
	 * @return
	 */
	private String getClusterInfo(String id) {
		String[] tmp = id.split(CLUSTER_SEP);
		if(tmp.length == 2)
			return tmp[1];
		return null;
	}
	
	/**
	 * removes the cluster info from an id
	 * @param id
	 * @return
	 */
	private String removeClusterInfoFromID(String id) {
		String[] tmp = id.split(CLUSTER_SEP);
		return tmp[0];
	}

	@Override
	public long getDefaultWaitTime() {
		return LONG_WAIT;
	}

	@Override
	public void releaseJob(String id) throws DrmaaException {
		ArrayList<String> args = new ArrayList<>();
		this.addClusterInfoToArguments(args, id);
		args.add("release");
		args.add(this.removeClusterInfoFromID(id));
		executeCommand("scontrol", args);
	}

	@Override
	public void holdJob(String id) throws DrmaaException {
		ArrayList<String> args = new ArrayList<>();
		this.addClusterInfoToArguments(args, id);
		args.add("hold");
		args.add(this.removeClusterInfoFromID(id));
		executeCommand("scontrol", args);
	}

	@Override
	public void cancelJob(String id) throws DrmaaException {
		ArrayList<String> args = new ArrayList<>();
		this.addClusterInfoToArguments(args, id);
		args.add(this.removeClusterInfoFromID(id));
		executeCommand("scancel", args);
	}
	
	/**
	 * adds the info about the cluster that must be used to the argument list 
	 * @param args
	 */
	private void addClusterInfoToArguments(ArrayList<String> args, String id) {
		String cluster = this.getClusterInfo(id);
		if(cluster != null) {
			args.add("-M"); // only look at these cluster
			args.add(cluster);
		}
	}
	
	/**
	 * adds the info for which timespan jobs should be displayed
	 * @param args
	 */
	private void addDateInfoToArguments(ArrayList<String> args) {
		args.add("-S"); // get all jobs started after this time
		args.add(this.start_date_for_query);
	}

	@Override
	public boolean isJobRunning(String id) throws DrmaaException {
		if(!this.CACHED_JOB_STATUS.containsKey(id))
			return false;
		
		return RUNNING.equals(this.CACHED_JOB_STATUS.get(id));
	}
	
	@Override
	protected void updateJobStatusCache() {
		ArrayList<String> args = new ArrayList<>();
		args.add("-L"); // get info from all cluster
		args.add("--format=jobid,state,cluster");
		args.add("-P"); // seperated by sa'|' 
		
		QacctBinaryCallInfo info = new QacctBinaryCallInfo("generic", this.executeCommand("sacct", args));
		if(info.exit != 0) {
			info.printInfo(this.LOGGER, true);
			System.exit(1);
		}
		else {
			// try to find ids
			Scanner s = new Scanner(info.out);
			String id, status, cluster;
			while(s.hasNextLine()) {
				Matcher m = STATE_SACCT_PATTERN.matcher(s.nextLine());
				if(m.matches()) {
					id = m.group(1);
					status = m.group(2);
					cluster = m.group(3);
					id = this.addClusterInfo(id, cluster);
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
		if(runningInfo == null || (PENDING.equals(runningInfo) || RUNNING.equals(runningInfo)))
			return null;

		// values we wan't extract
		ArrayList<String> args = new ArrayList<>();
		this.addDateInfoToArguments(args);
		this.addClusterInfoToArguments(args, id);
		args.add("-o");
		args.add(INFO_FIELDS);
		args.add("-P");
		args.add("--job");
		args.add(this.removeClusterInfoFromID(id));
		// get exit code and resource info
		BinaryCallInfo info = this.executeCommand("sacct", args);
		Scanner s = new Scanner(info.out);
		String header = null;
		String values = null;
		
		if(s.hasNext())
			header = s.nextLine();
		if(s.hasNext())
			values = s.nextLine();
		s.close();
		
		if(header == null || values == null) {
			this.LOGGER.error("Failed to get detailed job info from sacct command!");
			info.printInfo(this.LOGGER, true);
			System.exit(1);
		}
		String[] h = header.split("\\|");
		String[] v = values.split("\\|");
		
		HashMap<String, String> res = new HashMap<>();
		int exitCode = Integer.MIN_VALUE;
		int signal = Integer.MIN_VALUE;
		for(int i = 0; i < h.length; i++) {
			res.put(h[i], v[i]);
			
			if(EXIT_CODE_FIELD.equals(h[i])) {
				String tmp[] = v[i].split(":");
				exitCode = Integer.parseInt(tmp[0]);
				signal = Integer.parseInt(tmp[1]);
			}
		}
		if(exitCode == Integer.MIN_VALUE) {
			this.LOGGER.error("Failed to get exit code info from sacct command!");
			info.printInfo(this.LOGGER, true);
			System.exit(1);
		}
		return new GenericJobInfo(id, exitCode, signal, COMPLETED.equals(runningInfo) || signal == 0, res);
	}
	
	@Override
	public void init() {
		// set query start time if some is set
		if(AttachInfo.hasLoadedData(AttachInfo.ATTACH_INITIAL_START_TIME)) {
			this.setStartDateForQuery(AttachInfo.getLoadedData(AttachInfo.ATTACH_INITIAL_START_TIME).toString());
		}
		
		executeCommand("sinfo"); // test, if sinfo works
		this.isInitComplete = true;
	}

	@Override
	public void clean(HashMap<String, SlurmExecutor> ids, boolean isInDetachMode) {
		this.isInitComplete = false;
		for(Entry<String, SlurmExecutor> job : ids.entrySet()) {
			if(!(isInDetachMode && job.getValue().getExecutorInfo().isWatchdogDetachSupported()))
				try { this.cancelJob(job.getKey()); } catch(Exception e) { e.printStackTrace(); }
		}
		this.CACHED_JOB_STATUS.clear();
		this.SUBMITTED_JOB_STATUS.clear();
	}

	@Override
	public String getNameOfExecutionNode(String id, String watchdogBaseDir) {
		if(!this.isJobKnownInGridSystem(id))
			return null;
		
		ArrayList<String> args = new ArrayList<>();
		this.addDateInfoToArguments(args);
		this.addClusterInfoToArguments(args, id);
		args.add("-P");
		args.add("-o");
		args.add("NodeList");
		args.add("--job");
		args.add(this.removeClusterInfoFromID(id));

		BinaryCallInfo info = this.executeCommand("sacct", args);
		if(info.exit != 0) {
			info.printInfo(this.LOGGER, true);
			System.exit(1);
		}
		else {
			// try to find node list
			Scanner s = new Scanner(info.out);
			String header = null;
			String values = null;
			
			if(s.hasNext())
				header = s.nextLine();
			if(s.hasNext())
				values = s.nextLine();
			
			s.close();
			if(header == null || values == null) {
				this.LOGGER.error("Failed to get detailed job info from sacct command!");
				info.printInfo(this.LOGGER, true);
				System.exit(1);
			}
			return values.split("\\|")[0];
		}
		return null;
	}

	@Override
	public boolean isJobKnownInGridSystem(String id) {
		if(!this.CACHED_JOB_STATUS.containsKey(id))
			return false;
		// check, if it has a good status
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