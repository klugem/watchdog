package de.lmu.ifi.bio.watchdog.executor.external.drmaa;

import de.lmu.ifi.bio.watchdog.executor.Executor;
import de.lmu.ifi.bio.watchdog.executor.external.ExternalExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.SyncronizedLineWriter;
import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Grid executor info
 * @author Michael Kluge
 *
 */
public class DRMAAExecutorInfo extends ExternalExecutorInfo {
	
	private static final long serialVersionUID = -8553496121935804071L;
	private static final int RETRY_SUBMIT_COUNT = 10; // try to context the DRM system x times before waiting for new round
	private final static String MEMORY_REGEX = "[MG]";
	private final static String GIGABYTE = "G";
	private final static int G2M = 1024;
	
	private final static String SPACER = " ";
	private final static String S = "-pe serial ";
	private final static String M = "-l vf=";
	private final static String Q = "-q ";
	
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
	public DRMAAExecutorInfo(String type, String name, boolean isDefault, boolean isStick2Host, Integer maxSlaveRunning, String path2java, int maxRunning, String watchdogBaseDir, Environment environment, int slots, String memory, String queue, String workingDir, String customParams, boolean disableDefault) {
		super(type, name, isDefault, isStick2Host, maxSlaveRunning, path2java, maxRunning, watchdogBaseDir, environment, workingDir);
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
	public String getCommandsForGrid() {
		StringBuffer b = new StringBuffer();

		// ignores all parameters that can be set by default (queue, memory, slots)
		if(!this.isDefaultParametersIgnored()) {
			// add queue
			if(this.hasQueueSet()) {
				b.append(Q);
				b.append(this.getQueue());
				b.append(SPACER);
			}		
			// add memory
			if(this.hasMemorySet()) {
				b.append(M);
				b.append(this.getMemory());
				b.append(SPACER);
			}
			// add slot
			b.append(S);
			b.append(this.getSlots());
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
		
		// end the tag
		x.endCurrentTag();
		return x.toString();
	}

	@Override
	public Executor<DRMAAExecutorInfo> getExecutorForTask(Task t, SyncronizedLineWriter logFile) {
		return new DRMAAExecutor(t, logFile, RETRY_SUBMIT_COUNT, this);
	}
	
	@Override
	public Object[] getDataToLoadOnGUI() {
		return new Object[] { this.getQueue(), this.getSlots(), this.getMemory(), this.getCustomParameters(), this.isDefaultParametersIgnored() };		
	}
}
