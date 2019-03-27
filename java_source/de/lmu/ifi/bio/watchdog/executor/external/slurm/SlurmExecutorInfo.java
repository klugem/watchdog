package de.lmu.ifi.bio.watchdog.executor.external.slurm;

import java.util.ArrayList;
import java.util.HashMap;

import de.lmu.ifi.bio.watchdog.executor.Executor;
import de.lmu.ifi.bio.watchdog.executor.external.ExternalExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.SyncronizedLineWriter;
import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Slurm executor info
 * @author Michael Kluge
 *
 */
public class SlurmExecutorInfo extends ExternalExecutorInfo {
	
	private static final long serialVersionUID = -7211509226234461095L;
	private final static String MEMORY_REGEX = "[MG]";
	private final static String GIGABYTE = "G";
	private final static int G2M = 1024;
	
	private final static String T = "--time";
	private final static String C = "--cpus-per-task";
	private final static String M = "--mem";
	private final static String Q = "--clusters";
	private final static String P = "--partition";

	private final String CUSTOM_PARAMS;
	private final boolean IGNORE_DEFAULT_PARAMS;
	private final int CPU;
	private final String MEMORY;
	private final String CLUSTERS;
	private final String PARTITION;
	private final String TIMELIMIT;
	
	private static final ArrayList<String> DEFAULT_SLURM_ARGS = new ArrayList<>();
	
	static {
		DEFAULT_SLURM_ARGS.add("--ntasks-per-node=1");
		DEFAULT_SLURM_ARGS.add("--nodes=1");
	}

	/**
	 * Constructor
	 * @param type
	 * @param name
	 * @param isDefault
	 * @param isStick2Host
	 * @param maxSlaveRunning
	 * @param path2java
	 * @param maxRunning
	 * @param watchdogBaseDir
	 * @param environment
	 * @param cpu
	 * @param memory
	 * @param clusters
	 * @param timelimit
	 * @param workingDir
	 * @param customParams
	 * @param disableDefault
	 */
	public SlurmExecutorInfo(String type, String name, boolean isDefault, boolean isStick2Host, Integer maxSlaveRunning, String path2java, int maxRunning, String watchdogBaseDir, Environment environment, String shebang, int cpu, String memory, String clusters, String partition, String timelimit, String workingDir, String customParams, boolean disableDefault) {
		super(type, name, isDefault, isStick2Host, maxSlaveRunning, path2java, maxRunning, watchdogBaseDir, environment, workingDir, shebang);
		this.CPU = Math.max(1, cpu);
		this.MEMORY = memory;
		this.CLUSTERS = clusters;
		this.TIMELIMIT = timelimit;
		this.PARTITION = partition;
		this.CUSTOM_PARAMS = customParams;
		this.IGNORE_DEFAULT_PARAMS = disableDefault;
	}
	
	/**
	 * number of CPUs which should be requested
	 * @return
	 */
	public int getCPUs() {
		return this.CPU;
	}
	
	/**
	 * memory which should be used per CPU
	 * @return
	 */
	public String getMemory() {
		return this.MEMORY;
	}
	
	/**
	 * cluster on which the task should be executed
	 * @return
	 */
	public String getCluster() {
		return this.CLUSTERS;
	}
	
	/**
	 * partition on which the task should be executed
	 * @return
	 */
	public String getPartition() {
		return this.PARTITION;
	}
	
	/**
	 * custom parameters that can be added by the user to be passed to the SGE
	 * @return
	 */
	public String getCustomParameters() {
		return this.CUSTOM_PARAMS;
	}
	
	/**
	 * true, if default parameters (slots, memory, 
	 * @return
	 */
	public boolean isDefaultParametersIgnored() {
		return this.IGNORE_DEFAULT_PARAMS;
	}
	
	public boolean hasMemorySet() {
		return this.getMemory() != null && this.getMemory().length() > 0;
	}
	
	public boolean hasClusterSet() {
		return this.getCluster() != null && this.getCluster().length() > 0;
	}
	
	public boolean hasPartitionSet() {
		return this.getPartition() != null && this.getPartition().length() > 0;
	}
	
	public boolean hasCustomParametersSet() {
		return this.getCustomParameters() != null && this.getCustomParameters().length() > 0;
	}
	
	/**
	 * gets the command which is set on the grid
	 * @return
	 */
	public ArrayList<String> getCommandsForGrid() {
		ArrayList<String> b = new ArrayList<>();

		// ignores all parameters that can be set by default (cluster, memory, cpu, time)
		if(!this.isDefaultParametersIgnored()) {
			b.addAll(DEFAULT_SLURM_ARGS);
			// add cluster
			if(this.hasClusterSet()) {
				b.add(Q);
				b.add(this.getCluster());
			}	
			if(this.hasPartitionSet()) {
				b.add(P);
				b.add(this.getPartition());
			}	
			// add memory
			if(this.hasMemorySet()) {
				b.add(M);
				b.add(Integer.toString(this.getTotalMemorsInMB()));
			}
			// add slot
			b.add(C);
			b.add(Integer.toString(this.getCPUs()));
			
			// add timelimit
			b.add(T);
			b.add(this.TIMELIMIT);
		}
		if(this.hasCustomParametersSet()) {
			b.add(this.getCustomParameters());
		}
		return b;
	}

	/**
	 * returns the total memory reserved by that slot
	 * @return
	 */
	public int getTotalMemorsInMB() {
		int memory = Integer.parseInt(this.getMemory().replaceAll(MEMORY_REGEX, ""));
		
		// convert gigabyte to megabyte
		if(this.getMemory().endsWith(GIGABYTE))
			memory = G2M * memory;
		
		return this.getCPUs() * memory;
	}

	
	@Override
	public String toXML() {
		XMLBuilder x = new XMLBuilder();
		// start with basic tag
		x.startTag(SlurmWorkloadManagerConnector.EXECUTOR_NAME, false);
		x.addQuotedAttribute(XMLParser.NAME, this.getName());

		// add optional attributes
		if(this.hasDefaultEnv())
			x.addQuotedAttribute(XMLParser.ENVIRONMENT, this.getEnv().getName());
		if(this.isDefaultExecutor())
			x.addQuotedAttribute(XMLParser.DEFAULT, true);
		if(this.isStick2Host())
			x.addQuotedAttribute(XMLParser.STICK2HOST, true);
		
		if(this.getMaxSimRunning() >= 1)
			x.addQuotedAttribute(XMLParser.MAX_RUNNING, this.getMaxSimRunning());
		// add default grid parameters
		if(!this.isDefaultParametersIgnored()) {
			if(this.getCPUs() > 1)
				x.addQuotedAttribute(XMLParser.CPU, this.getCPUs());
			if(this.hasMemorySet())
				x.addQuotedAttribute(XMLParser.MEMORY, this.getMemory());
			if(this.hasClusterSet())
				x.addQuotedAttribute(XMLParser.CLUSTER, this.getCluster());
			if(this.hasPartitionSet())
				x.addQuotedAttribute(XMLParser.PARTITION, this.getPartition());
			if(this.hasTimelimitSet()) {
				x.addQuotedAttribute(XMLParser.TIMELIMIT, this.getTimelimit());
			}
		}
		else {
			x.addQuotedAttribute(XMLParser.DISABLE_DEFAULT, true);
		}
		// add custom parameters
		if(this.hasCustomParametersSet())
			x.addQuotedAttribute(XMLParser.CUSTOM_PARAMETERS, this.getCustomParameters());
		
		if(this.hasColor())
			x.addQuotedAttribute(XMLParser.COLOR, this.getColor());
		if(this.hasCustomShebang()) 
			x.addQuotedAttribute(XMLParser.SHEBANG, this.getShebang());
		
		
		// end the tag
		x.endCurrentTag();
		return x.toString();
	}

	public String getTimelimit() {
		return this.TIMELIMIT;
	}

	public boolean hasTimelimitSet() {
		return this.getTimelimit() != null && this.getTimelimit().length() > 0;
	}

	@Override
	public Executor<SlurmExecutorInfo> getExecutorForTask(Task t, SyncronizedLineWriter logFile) {
		return new SlurmExecutor(t, logFile, 1, this);
	}

	@Override
	public Object[] getDataToLoadOnGUI() {
		return new Object[] { this.getCluster(), this.getPartition(), this.getCPUs(), this.getMemory(), this.getTimelimit(), this.getCustomParameters(), this.isDefaultParametersIgnored() };		
	}

	@Override
	public boolean isWatchdogDetachSupported() {
		return true;
	}
	
	@Override
	public HashMap<String, String> getExecutorSpecificEnvironmentVariables() {
		// set infos about the number of used cores and the total memory
		HashMap<String, String> env = new HashMap<>();
		env.put(Executor.WATCHGOD_CORES, Integer.toString(this.getCPUs()));
		env.put(Executor.WATCHGOD_MEMORY, Integer.toString(this.getTotalMemorsInMB()));
		return env; 
	}
}
