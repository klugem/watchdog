package de.lmu.ifi.bio.watchdog.processblocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Task, which uses the return values of previous tasks
 * @author Michael Kluge
 *
 */
public class ProcessInput extends ProcessReturnValueAdder {

	private static final long serialVersionUID = -6927954201365934647L;

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

	private String gui_sep;
	private String gui_compare;
	
	/**
	 * Constructor
	 * @param globalSep
	 * @param replaceDefaultGroupWithVariable
	 */
	public ProcessInput(String name, String globalSep, String replaceDefaultGroupWithVariable) {
		super(name, globalSep, replaceDefaultGroupWithVariable);
		
		this.gui_sep = globalSep;
		this.gui_compare = replaceDefaultGroupWithVariable;
	}

	@Override
	public HashMap<String, String> getValues() {
		return new LinkedHashMap<String, String>(this.BLOCK_ENTRIES);
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
		if(!this.getReturnValueSeperator().equals(DEFAULT_MULTIPLE_RETURN_VALUE_SEPARATOR))
			x.addQuotedAttribute(XMLParser.SEP, this.getReturnValueSeperator());
		if(this.getReplaceDefaultGroup() != null)
			x.addQuotedAttribute(XMLParser.COMPARE_NAME, this.getReplaceDefaultGroup());
		if(this.hasColor())
			x.addQuotedAttribute(XMLParser.COLOR, this.getColor());
		
		// end the tag
		x.endCurrentTag();
		return x.toString();
	}
	

	@Override
	public LinkedHashMap<String, String> getOfferedVariables(ArrayList<String> replace) {
		LinkedHashMap<String, String> ret = new LinkedHashMap<>();
		if(replace == null) {
			ret.putAll(OFFER_VAR);
		}
		else {
			for(String key : OFFER_VAR.keySet()) {
				for(String name : replace) {
					ret.put(key.replace(REPLACE, name), OFFER_VAR.get(key).replace(REPLACE, name));
				}
			}
		}
		return ret;
	}
	
	@Override
	public boolean isAppendAble() {
		return false;
	}

	@Override
	public boolean mightContainFilenames() {
		return true;
	}
	
	@Override
	public boolean addsReturnInfoToTasks() {
		return true;
	}			
	
	@Override
	public Object[] getDataToLoadOnGUI() { 
		return new Object[] { this.gui_sep, this.gui_compare }; 
	}
	
	@Override
	public Object clone() {
		return new ProcessInput(this.getName(), this.getReturnValueSeperator(), this.getReplaceDefaultGroup());
	}
}
