package de.lmu.ifi.bio.watchdog.helper.ProcessBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import de.lmu.ifi.bio.watchdog.helper.ReplaceSpecialConstructs;
import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Task, which uses the return values of previous tasks
 * @author Michael Kluge
 *
 */
public class ProcessInput extends ProcessMultiParam {

	private static final long serialVersionUID = -6927954201365934647L;
	private final HashMap<String, String> BLOCK_ENTRIES = new HashMap<>();
	private final String GLOBAL_SEP;
	private final String REPLACE_DEFAULT_GROUP;
	
	private static final Logger LOGGER = new Logger();
	private static final LinkedHashMap<String, String> OFFER_VAR = new LinkedHashMap<>();
	private static final String REPLACE = "RET_NAME";
	
	static {
		OFFER_VAR.put("[$"+REPLACE+"]", "return value of a dependency with the name $RET_NAME");
		OFFER_VAR.put("[$"+REPLACE+",n,sep]", "n suffixes of the value stored in return value of a dependency with the name $RET_NAME");
		OFFER_VAR.put("{$"+REPLACE+"}", "return value of a dependency with the name $RET_NAME");
		OFFER_VAR.put("{$"+REPLACE+",n,sep}", "n suffixes of the value stored in return value of a dependency with the name $RET_NAME");
		OFFER_VAR.put("($"+REPLACE+")", "return value of a dependency with the name $RET_NAME");
		OFFER_VAR.put("($"+REPLACE+",n,sep)", "n suffixes of the value stored in return value of a dependency with the name $RET_NAME");
	}
	
	/**
	 * Constructor
	 * @param globalSep
	 * @param replaceDefaultGroupWithVariable
	 */
	public ProcessInput(String name, String globalSep, String replaceDefaultGroupWithVariable) {
		super(name, null, null);
		this.GLOBAL_SEP = globalSep;
		if(replaceDefaultGroupWithVariable != null && replaceDefaultGroupWithVariable.length() == 0)
			replaceDefaultGroupWithVariable = null;
		this.REPLACE_DEFAULT_GROUP = replaceDefaultGroupWithVariable;
	}
	
	/**
	 * separator
	 * @return
	 */
	public String getGlobalSep() {
		return this.GLOBAL_SEP;
	}
	
	/**
	 * is used to replace the default group variable with a specific return variable in order to connect this input loop with other process blocks
	 * @return
	 */
	public String getReplaceDefaultGroup() {
		return this.REPLACE_DEFAULT_GROUP;
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

	@Override
	public HashMap<String, String> getValues() {
		return new HashMap<String, String>(this.BLOCK_ENTRIES);
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
	 * 
	 * @param retValues
	 * @return
	 */
	public String joinGlobalReturnValues(ArrayList<String> retValues) {
		return StringUtils.join(retValues, this.GLOBAL_SEP);
	}
	
	@Override
	public int size() {
		return this.BLOCK_ENTRIES.size();
	}

	@Override
	public String toXML() {
		XMLBuilder x = new XMLBuilder();
		// start with basic tag
		x.startTag(XMLParser.PROCESS_INPUT, false);
		x.addQuotedAttribute(XMLParser.NAME, this.getName());
		
		// add optional attributes
		if(!this.getGlobalSep().equals(":"))
			x.addQuotedAttribute(XMLParser.SEP, this.getGlobalSep());
		if(this.REPLACE_DEFAULT_GROUP != null)
			x.addQuotedAttribute(XMLParser.COMPARE_NAME, this.REPLACE_DEFAULT_GROUP);
		if(this.hasColor())
			x.addQuotedAttribute(XMLParser.COLOR, this.getColor());
		
		// end the tag
		x.endCurrentTag();
		return x.toString();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public LinkedHashMap<String, String> getOfferedVariables() {
		return (LinkedHashMap<String, String>) OFFER_VAR.clone();
	}
	
	public LinkedHashMap<String, String> getOfferedVariables(ArrayList<String> replace) {
		LinkedHashMap<String, String> ret = new LinkedHashMap<>();
		for(String key : OFFER_VAR.keySet()) {
			for(String name : replace) {
				ret.put(key.replace(REPLACE, name), OFFER_VAR.get(key).replace(REPLACE, name));
			}
		}
		return ret;
	}
}
