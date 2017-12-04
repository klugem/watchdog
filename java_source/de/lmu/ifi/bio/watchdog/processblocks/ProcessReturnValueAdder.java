package de.lmu.ifi.bio.watchdog.processblocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import de.lmu.ifi.bio.watchdog.helper.ReplaceSpecialConstructs;
import de.lmu.ifi.bio.watchdog.logger.Logger;

public abstract class ProcessReturnValueAdder extends ProcessMultiParam implements Cloneable {

	private static final long serialVersionUID = 844534047220037755L;
	private static final Logger LOGGER = new Logger();
	public static final String DEFAULT_MULTIPLE_RETURN_VALUE_SEPARATOR = ":";
	
	protected final HashMap<String, String> BLOCK_ENTRIES = new HashMap<>();
	private final String REPLACE_DEFAULT_GROUP;
	private final String GLOBAL_SEP;
	
	public ProcessReturnValueAdder(String name, String globalSep, String replaceDefaultGroupWithVariable) {
		super(name, null, null);
		this.GLOBAL_SEP = globalSep;
		if(replaceDefaultGroupWithVariable != null && replaceDefaultGroupWithVariable.length() == 0)
			replaceDefaultGroupWithVariable = null;
		this.REPLACE_DEFAULT_GROUP = replaceDefaultGroupWithVariable;
	}
	
	public String getReturnValueSeperator() {
		return this.GLOBAL_SEP;
	}
	
	/**
	 * is used to replace the default group variable with a specific return variable in order to connect this input loop with other process blocks
	 * @return
	 */
	public String getReplaceDefaultGroup() {
		return this.REPLACE_DEFAULT_GROUP;
	}
	
	@Override
	public abstract Object clone();
	
	/**
	 * adds these values to the return vales
	 * @param retValues
	 * @return
	 */
	public String joinGlobalReturnValues(ArrayList<String> retValues) {
		return StringUtils.join(retValues, this.getReturnValueSeperator());
	}
	
	/**
	 * tests, if a block with that name is already there
	 * @param groupFileName
	 * @return
	 */
	public boolean hasBlock(String groupFileName) {
		return this.BLOCK_ENTRIES.containsKey(groupFileName);
	}
	
	/**
	 * adds a new "block" entry
	 * @param groupName
	 * @param variables
	 * @param replaceDefaultGroupWithVariable
	 */
	public void addBlock(String groupName, TreeMap<String, String> variables) {
		// add a initial name mapping
		if(this.BLOCK_ENTRIES.size() == 0) {
			int i = 1;
			for(String var : variables.keySet()) {
				this.NAME_MAPPING.put(var, i);
				i++;
			}
		}
		else if(this.NAME_MAPPING.size() != variables.size()) {
			LOGGER.error("New block of process input does not have the correct number of variables.");
			LOGGER.error("mapping: " + StringUtils.join(this.NAME_MAPPING, "; "));
			LOGGER.error("variables: " + StringUtils.join(variables, "; "));
			System.exit(1);
		}
		else {
			// ensure that name mapping is ok
			int i = 1;
			for(String name : variables.keySet()) {
				if(!(this.NAME_MAPPING.containsKey(name) && this.NAME_MAPPING.get(name) == i)) {
					LOGGER.error("New block of process input does not have the same input variables.");
					System.exit(1);	
				}
				i++;
			}
		}
		// get correct group name
		if(this.REPLACE_DEFAULT_GROUP != null) {
				if(!variables.containsKey(this.REPLACE_DEFAULT_GROUP)) {
					LOGGER.error("Return values of process loop do not contain a value named '"+this.REPLACE_DEFAULT_GROUP+"'.");
					System.exit(1);
				}
			groupName = variables.get(this.REPLACE_DEFAULT_GROUP);
		}
		
		this.BLOCK_ENTRIES.put(groupName, StringUtils.join(variables.values(), ReplaceSpecialConstructs.TAB));
	}

}
