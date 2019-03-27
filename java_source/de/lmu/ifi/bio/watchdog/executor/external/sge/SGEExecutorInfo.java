package de.lmu.ifi.bio.watchdog.executor.external.sge;

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
 * SGE executor info
 * @author Michael Kluge
 *
 */
public class SGEExecutorInfo extends ExternalExecutorInfo {
	
	private static final long serialVersionUID = -8553496121935804071L;
	private static final int RETRY_SUBMIT_COUNT = 10; // try to context the DRM system x times before waiting for new round
	private final static String MEMORY_REGEX = "[MG]";
	private final static String GIGABYTE = "G";
	private final static int G2M = 1024;
	
	private final static String PE1= "-pe";
	private final static String PE2= "serial";
	private final static String M1 = "-l";
	private final static String M2 = "vf=";
	private final static String Q = "-q";
	
	private final String CUSTOM_PARAMS;
	private final boolean IGNORE_DEFAULT_PARAMS;
	private final int SLOTS;
	private final String MEMORY;
	private final String QUEUE;
	
	/**
	 * Constructor
	 * @param name
	 * @param isDefault
	 * @param maxRunning
	 * @param watchdogBaseDir
	 * @param environment
	 * @param slots
	 * @param memory
	 * @param queue
	 */
	public SGEExecutorInfo(String type, String name, boolean isDefault, boolean isStick2Host, Integer maxSlaveRunning, String path2java, int maxRunning, String watchdogBaseDir, Environment environment, String shebang, int slots, String memory, String queue, String workingDir, String customParams, boolean disableDefault) {
		super(type, name, isDefault, isStick2Host, maxSlaveRunning, path2java, maxRunning, watchdogBaseDir, environment, workingDir, shebang);
		this.SLOTS = Math.max(1, slots);
		this.MEMORY = memory;
		this.QUEUE = queue;
		this.CUSTOM_PARAMS = customParams;
		this.IGNORE_DEFAULT_PARAMS = disableDefault;
	}
	
	/**
	 * number of slots which should be used
	 * @return
	 */
	public int getSlots() {
		return this.SLOTS;
	}
	
	/**
	 * memory which should be used
	 * @return
	 */
	public String getMemory() {
		return this.MEMORY;
	}
	
	/**
	 * queue on which the task should be executed
	 * @return
	 */
	public String getQueue() {
		return this.QUEUE;
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
	
	public boolean hasQueueSet() {
		return this.getQueue() != null && this.getQueue().length() > 0;
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

		// ignores all parameters that can be set by default (queue, memory, slots)
		if(!this.isDefaultParametersIgnored()) {
			// add queue
			if(this.hasQueueSet()) {
				b.add(Q);
				b.add(this.getQueue());
			}		
			// add memory
			if(this.hasMemorySet()) {
				b.add(M1);
				b.add(M2 + this.getMemory());
			}
			// add slot
			b.add(PE1);
			b.add(PE2);
			b.add(Integer.toString(this.getSlots()));
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
		
		return this.getSlots() * memory;
	}

	
	@Override
	public String toXML() {
		XMLBuilder x = new XMLBuilder();
		// start with basic tag
		x.startTag(XMLParser.CLUSTER, false);
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
			if(this.getSlots() > 1)
				x.addQuotedAttribute(XMLParser.SLOTS, this.getSlots());
			if(this.hasMemorySet())
				x.addQuotedAttribute(XMLParser.MEMORY, this.getMemory());
			if(this.hasQueueSet())
				x.addQuotedAttribute(XMLParser.QUEUE, this.getQueue());
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

	@Override
	public Executor<SGEExecutorInfo> getExecutorForTask(Task t, SyncronizedLineWriter logFile) {
		return new SGEExecutor(t, logFile, RETRY_SUBMIT_COUNT, this);
	}
	
	@Override
	public Object[] getDataToLoadOnGUI() {
		return new Object[] { this.getQueue(), this.getSlots(), this.getMemory(), this.getCustomParameters(), this.isDefaultParametersIgnored() };		
	}
	
	@Override
	public boolean isWatchdogDetachSupported() {
		return true;
	}
	
	@Override
	public HashMap<String, String> getExecutorSpecificEnvironmentVariables() {
		// set infos about the number of used cores and the total memory
		HashMap<String, String> env = new HashMap<>();
		env.put(Executor.WATCHGOD_CORES, Integer.toString(this.getSlots()));
		env.put(Executor.WATCHGOD_MEMORY, Integer.toString(this.getTotalMemorsInMB()));
		return env; 
	}
}
