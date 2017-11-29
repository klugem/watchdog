package de.lmu.ifi.bio.watchdog.executor.external.slurm;

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
	
	private final static String SPACER = " ";
	private final static String T = "--cpus-per-task";
	private final static String C = "--cpus-per-task";
	private final static String M = "--mem-per-cpu";
	private final static String Q = "--clusters";
	
	private final String CUSTOM_PARAMS;
	private final boolean IGNORE_DEFAULT_PARAMS;
	private final int CPU;
	private final String MEMORY;
	private final String CLUSTERS;
	private final String TIMELIMIT;
	

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
	public SlurmExecutorInfo(String type, String name, boolean isDefault, boolean isStick2Host, Integer maxSlaveRunning, String path2java, int maxRunning, String watchdogBaseDir, Environment environment, int cpu, String memory, String clusters, String timelimit, String workingDir, String customParams, boolean disableDefault) {
		super(type, name, isDefault, isStick2Host, maxSlaveRunning, path2java, maxRunning, watchdogBaseDir, environment, workingDir);
		this.CPU = Math.max(1, cpu);
		this.MEMORY = memory;
		this.CLUSTERS = clusters;
		this.TIMELIMIT = timelimit;
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
	
	public boolean hasCustomParametersSet() {
		return this.getCustomParameters() != null && this.getCustomParameters().length() > 0;
	}
	
	/**
	 * gets the command which is set on the grid
	 * @return
	 */
	public String getCommandsForGrid() {
		StringBuffer b = new StringBuffer();

		// ignores all parameters that can be set by default (cluster, memory, cpu, time)
		if(!this.isDefaultParametersIgnored()) {
			// add cluster
			if(this.hasClusterSet()) {
				b.append(Q);
				b.append(this.getCluster());
				b.append(SPACER);
			}		
			// add memory
			if(this.hasMemorySet()) {
				b.append(M);
				b.append(this.getMemory());
				b.append(SPACER);
			}
			// add slot
			b.append(C);
			b.append(this.getCPUs());
			// add timelimit
			b.append(T);
			b.append(this.TIMELIMIT);
		}
		if(this.hasCustomParametersSet()) {
			if(b.length() > 0)
				b.append(SPACER);
			b.append(this.getCustomParameters());
		}
		
		return b.toString();
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
		return new Object[] { this.getCluster(), this.getCPUs(), this.getMemory(), this.getTimelimit(), this.getCustomParameters(), this.isDefaultParametersIgnored() };		
	}
}
