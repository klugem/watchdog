package de.lmu.ifi.bio.watchdog.executor.external.slurm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobInfo;
import org.ggf.drmaa.TryLaterException;

import de.lmu.ifi.bio.watchdog.executor.external.BinaryCallBasedExternalWorkflowManagerConnector;
import de.lmu.ifi.bio.watchdog.executor.external.BinaryCallInfo;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.task.Task;

public class SlurmWorkloadManagerConnector extends BinaryCallBasedExternalWorkflowManagerConnector<SlurmExecutor> {

	public static final String EXECUTOR_NAME = "slurm";
	private static final String ID_REGEX = "Submitted batch job ([0-9]+)";
	private static final String STATUS_SACCT = "\\|(.+)\\|[0-9]+:[0-9]+";
	private static final String RUNNING = "RUNNING";
	private static final String COMPLETED = "COMPLETED";
	private static final String PENDING = "PENDING";
	private static final String GOOD_STATUS = PENDING+"|"+COMPLETED+"|"+RUNNING;
	private static final Pattern ID_REGEX_PATTERN = Pattern.compile(ID_REGEX);
	private static final String INFO_FIELDS = "JobID,JobName,Partition,NCPUS,State,ExitCode,Elapsed,CPUTime,MaxRSS,MaxVMSize,MaxDiskRead,MaxDiskWrite,ConsumedEnergy,AveCPU,AveVMSize,Submit";
	private static final String EXIT_CODE_FIELD = "ExitCode";
	private HashMap<String, Pattern> RUNNING_REGEX_PATTERN = new HashMap<>();
	private boolean isInitComplete = false;
	private boolean isJobInitSubmitted = false;
	private static int MAX_WAIT = 15000; // wait 15 s after job submission
	
	public SlurmWorkloadManagerConnector(Logger l) {
		super(l);
	}
	
	private void prepareRunningPattern(String id) {
		Pattern p = Pattern.compile(id + STATUS_SACCT);
		this.RUNNING_REGEX_PATTERN.put(id, p);
	}

	@Override
	public synchronized String submitJob(Task task, SlurmExecutor ex) throws DrmaaException {
		ArrayList<String> args = new ArrayList<>();
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
		
		// BUG https://bugs.schedmd.com/show_bug.cgi?id=3197 might cause problems in older versions
		if(task.isTaskOnHold()) args.add("--hold"); 
		// BUG
		
		args.add("--job-name"); args.add(task.getProjectShortCut() + " " + task.getName() + " " + task.getID());
		args.add("--time");  args.add(execinfo.getTimelimit());
		args.add("--cpus-per-task");  args.add(Integer.toString(execinfo.getCPUs()));
		args.add("--mem-per-cpu");  args.add(execinfo.getMemory());
		args.add(ex.getFinalCommand(false)[0]);

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
				this.prepareRunningPattern(id);
				int maxWait = 0;
				// wait until the ID is known in the system
				this.isJobInitSubmitted = true;
				while(!this.isJobKnownInGridSystem(id) && maxWait <= MAX_WAIT) {
					try { Thread.sleep(100); } catch(Exception e) {}
					maxWait += 100;
				}
				this.isJobInitSubmitted = false;
				if(maxWait > MAX_WAIT)
					throw new TryLaterException("ID '"+id+"' could not be queried by sacct after a wait time of " + (MAX_WAIT/1000) + " seconds.");
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

	@Override
	public void releaseJob(String id) throws DrmaaException {
		ArrayList<String> args = new ArrayList<>();
		args.add("release");
		args.add(id);
		executeCommand("scontrol", args);
	}

	@Override
	public void holdJob(String id) throws DrmaaException {
		ArrayList<String> args = new ArrayList<>();
		args.add("hold");
		args.add(id);
		executeCommand("scontrol", args);
	}

	@Override
	public void cancelJob(String id) throws DrmaaException {
		ArrayList<String> args = new ArrayList<>();
		args.add(id);
		executeCommand("scancel", args);
	}

	@Override
	public boolean isJobRunning(String id) throws DrmaaException {
		SacctBinaryCallInfo info = getJobStatus(id, false);
		
		if(info == null) {
			System.exit(1);
		}
		return RUNNING.equals(info.status);
	}
	
	public SacctBinaryCallInfo getJobStatus(String id, boolean init) {
		ArrayList<String> args = new ArrayList<>();
		args.add("-b");
		args.add("-P");
		args.add("--job");
		args.add(id);

		SacctBinaryCallInfo info = new SacctBinaryCallInfo(id, this.executeCommand("sacct", args));
		if(info.exit != 0) {
			info.printInfo(this.LOGGER, true);
			System.exit(1);
		}
		else {
			// try to find id
			Matcher m = this.RUNNING_REGEX_PATTERN.get(id).matcher(info.out);
			if(m.find()) {
				info.status = m.group(1);
				return info;
			}
			else if(!init) {
				this.LOGGER.error("Job ID " + id + " was not found in sacct output.");
				info.printInfo(this.LOGGER, true);
			}
		}
		return null;
	}

	@Override
	public JobInfo getJobInfo(String id) throws DrmaaException {
		SacctBinaryCallInfo runningInfo = getJobStatus(id, false);
		if(PENDING.equals(runningInfo.status) || RUNNING.equals(runningInfo.status))
			return null;
		
		ArrayList<String> args = new ArrayList<>();
		args.add("-o");
		args.add(INFO_FIELDS);
		args.add("-P");
		args.add("--job");
		args.add(id);
		// get exit code and resource info
		BinaryCallInfo info = this.executeCommand("sacct", args);
		Scanner s = new Scanner(info.out);
		String header = s.nextLine();
		String values = s.nextLine();
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
		return new SlurmJobInfo(id, exitCode, signal, COMPLETED.equals(runningInfo.status));
	}

	@Override
	public void init() {
		executeCommand("sinfo"); // test, if sinfo works
		this.isInitComplete = true;
	}

	@Override
	public void clean(HashSet<String> ids) {
		this.isInitComplete = false;
		for(String id : ids) 
			try { this.cancelJob(id); } catch(Exception e) { e.printStackTrace(); }
		this.RUNNING_REGEX_PATTERN.clear();
	}

	@Override
	public String getNameOfExecutionNode(String id, String watchdogBaseDir) {	
		ArrayList<String> args = new ArrayList<>();
		args.add("-P");
		args.add("-o");
		args.add("NodeList");
		args.add("--job");
		args.add(id);

		BinaryCallInfo info = this.executeCommand("sacct", args);
		if(info.exit != 0) {
			info.printInfo(this.LOGGER, true);
			System.exit(1);
		}
		else {
			// try to find node list
			Scanner s = new Scanner(info.out);
			String header = s.nextLine();
			String values = s.nextLine();
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
		SacctBinaryCallInfo info = getJobStatus(id, this.isJobInitSubmitted);
		return info != null && info.status.matches(GOOD_STATUS);
	}

	@Override
	public String getExecutorType() {
		return EXECUTOR_NAME;
	}

	@Override
	public boolean isInitComplete() {
		return this.isInitComplete;
	}
	
	// TODO: set env and workingdir to find binarys ?
	protected BinaryCallInfo executeCommand(String command, ArrayList<String> args) {
		return this.executeCommand(command, args, null, null);
	}
	
	// TODO: set env and workingdir to find binarys ?
	protected BinaryCallInfo executeCommand(String command) {
		return this.executeCommand(command, new ArrayList<>(), null, null);
	}
}