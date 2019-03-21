package de.lmu.ifi.bio.watchdog.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;

import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * class which contains the environment stuff
 * @author Michael Kluge
 *
 */
public class Environment implements XMLDataStore {

	private static final long serialVersionUID = -3644008624943186138L;
	private static final String BASH_FUNCTION = "()";
	public static final String DEFAULT_UPDATE_SEP = ":";
	private static final String PATTERN_NAME = "{$NAME}";
	private static final String PATTERN_VALUE = "{$VALUE}";
	private static final String REPLACE_QUOTE = "&quot;";
	private static final String DEFAULT_COMMAND = "export " + PATTERN_NAME + "=" + REPLACE_QUOTE + PATTERN_VALUE + REPLACE_QUOTE;
	public static final String DEFAULT_SHEBANG = "#!/bin/bash"; 
	private static final String DEFAULT_QUERY_FORMAT = "$" + PATTERN_NAME;
	private static final Logger LOGGER = new Logger();
	private static final Map<String, String> LOCAL_ENV = System.getenv();
	public static final String COMMAND_ERROR = "External export command for environment variables must contain '"+PATTERN_NAME+"' and '"+PATTERN_VALUE+"'.";
	
	private String color;
	private final HashMap<String, String> ENV = new HashMap<>();
	private final HashMap<String, String> COMMANDS = new HashMap<>();
	private final ArrayList<Object[]> STORE = new ArrayList<>();
	private final String NAME;
	private final boolean IS_LOCAL_EXECUTOR;
	private boolean COPY_GLOBAL;
	private final boolean USE_EXTERNAL_COMMAND;
	private String shebang = DEFAULT_SHEBANG;
	private String baseCommand = DEFAULT_COMMAND;
	private String queryFormat = DEFAULT_QUERY_FORMAT;

	/**
	 * Constructor for local executor
	 * @param name
	 * @param isLocalExecutor
	 */
	public Environment(String name, boolean isLocalExecutor, boolean copyGlobal) {
		this.NAME = name;
		this.IS_LOCAL_EXECUTOR = isLocalExecutor;
		this.USE_EXTERNAL_COMMAND = false;
		this.COPY_GLOBAL = copyGlobal;
		
		// copy, the values globally
		if(this.COPY_GLOBAL || this.IS_LOCAL_EXECUTOR)
			this.copyAllLocalEnv();
	}
	
	/**
	 * Constructor
	 * @param name
	 * @param isLocalExecutor
	 * @param useExternalCommand
	 */
	public Environment(String name, boolean isLocalExecutor, boolean copyGlobal, boolean useExternalCommand) {
		this.NAME = name;
		this.IS_LOCAL_EXECUTOR = isLocalExecutor;
		this.USE_EXTERNAL_COMMAND = useExternalCommand;
		this.COPY_GLOBAL = copyGlobal;
		
		// copy, the values globally
		if(this.COPY_GLOBAL || this.IS_LOCAL_EXECUTOR)
			this.copyAllLocalEnv();
	}
	
	/**
	 * true, if the external command should be used to set the variables
	 * @return
	 */
	public boolean useExternalCommand() {
		return !this.IS_LOCAL_EXECUTOR && this.USE_EXTERNAL_COMMAND;
	}
	
	/**
	 * retuns the shebang for the external command
	 * @return
	 */
	public String getShebang() {
		return this.shebang;
	}
	
	public String getName() {
		return this.NAME;
	}
	
	public void storeData(String name, String value, String sep, boolean copy, boolean update) {
		this.add(name, value, sep, copy, update);
		this.STORE.add(new Object[]{name, value, sep, copy, update});
	}
	
	/**
	 * returns the data that was stored by the GUI
	 * @return
	 */
	public ArrayList<Object[]> getStoredData() {
		return this.STORE;
	}
	
	/**
	 * adds a variable
	 * @param name
	 * @param value
	 * @param sep
	 * @param copy
	 * @param update
	 */
	public void add(String name, String value, String sep, boolean copy, boolean update) {
		// do not try to export any functions
		if(name.endsWith(BASH_FUNCTION) || value.startsWith(BASH_FUNCTION))
			return;

		if(!copy && !update) {
			this.ENV.put(name, value);
			this.addCommand(name, value, null);
			return;
		}
		
		// get the value from the local host, if copy is set
		if(copy && LOCAL_ENV.containsKey(name)) {
			this.ENV.put(name, LOCAL_ENV.get(name));
			if(!update) {
				this.addCommand(name, LOCAL_ENV.get(name), null);
				return;
			}
		}
			
		// update the value
		if(update) {
			String oldValue = this.ENV.get(name);
			if(oldValue != null && oldValue.length() > 0) {
				this.ENV.put(name, value + sep + oldValue);
				this.addCommand(name, value + sep + oldValue, null);
				return;
			}
			// nothing to update
			else { 
				this.ENV.put(name, value);
				if(!this.IS_LOCAL_EXECUTOR)
					this.addCommand(name, value, null);
				else // try to update the value on remote / cluster using the external command
					this.addCommand(name, value, sep);
				return;
			}
		}
	}
	
	/**
	 *  copy complete local env
	 */
	private void copyAllLocalEnv() {
		for(String name : LOCAL_ENV.keySet()) {
			if(!this.ENV.containsKey(name)) {
				this.add(name, LOCAL_ENV.get(name), null, false, false);
			}
		}
	}
	
	public static boolean isCommandValid(String command) {
		return command.contains(PATTERN_NAME) && command.contains(PATTERN_VALUE);
	}
	
	/**
	 * sets a new command to set environment variables
	 * @param command
	 */
	public void setCommand(String command) {
		// test, if both required variables are used correctly
		if(!isCommandValid(command)) {
			LOGGER.error(COMMAND_ERROR);
			System.exit(1);
		}
		this.baseCommand = command;
	}
	
	public void setShebang(String shebang) {
		if(shebang.isEmpty()) {
			LOGGER.error("Shebang can not be empty.");
			System.exit(1);
		}
		this.shebang = shebang;
	}
	
	/**
	 * sets a new default query format, which is used to update the variable
	 * @param command
	 */
	public void setQueryFormat(String queryFormat) {
		// test, if required variable is used correctly
		if(!(queryFormat.contains(PATTERN_NAME))) {
			LOGGER.error("Default query format for environment variables must contain '"+PATTERN_NAME+"'.");
			System.exit(1);
		}
		this.queryFormat = queryFormat;
	}
	
	/**
	 * adds a remote update request
	 * @param name
	 * @param value
	 */
	private void addCommand(String name, String value, String sep) {
		if(sep != null)
			value = value + sep + this.queryFormat.replace(PATTERN_NAME, name);
		this.COMMANDS.put(name, StringEscapeUtils.unescapeHtml4(this.getExternalCommand()).replace(PATTERN_NAME, name).replace(PATTERN_VALUE, value)); 
	}
	
	/**
	 * returns the environment variables which can be set without commands (no update on remote/cluster hosts)
	 */
	public HashMap<String, String> getEnv() {
		HashMap<String, String> env = new HashMap<>();
		
		for(String k : this.ENV.keySet())
			env.put(k, this.ENV.get(k));
		return env;
	}
	
	/**
	 * returns the commands to set the environment variables
	 * @return
	 */
	public ArrayList<String> getCommands() {
		ArrayList<String> commands = new ArrayList<>();
		
		for(String c : this.COMMANDS.values())
			commands.add(c);

		return commands;
	}
	
	public String getExternalCommand() {
		return this.baseCommand;
	}

	@Override
	public String toXML() {
		XMLBuilder x = new XMLBuilder();
		// start with basic tag
		x.startTag(XMLParser.ENVIRONMENT, false);
		x.addQuotedAttribute(XMLParser.NAME, this.getName());
		
		// add optional attributes
		if(this.COPY_GLOBAL)
			x.addQuotedAttribute(XMLParser.COPY_LOCAL_VALUE, this.COPY_GLOBAL);
		if(this.useExternalCommand()) {
			x.addQuotedAttribute(XMLParser.USE_EXTERNAL_EXPORT, true);
			x.addQuotedAttribute(XMLParser.SHEBANG, this.getShebang());
			x.addQuotedAttribute(XMLParser.EXPORT_COMMAND, this.getExternalCommand().replace("\"", REPLACE_QUOTE));
		}
		if(this.hasColor())
			x.addQuotedAttribute(XMLParser.COLOR, this.getColor());
		boolean first = true;
		
		// add variables
		for(Object[] data : this.STORE) {
			String name = (String) data[0];
			String value = (String) data[1];
			String sep = (String) data[2];
			boolean copyLocalValue = (Boolean) data[3];
			boolean update = (Boolean) data[4];
			
			x.startTag(XMLParser.VAR, true, !first);
			x.addQuotedAttribute(XMLParser.NAME, name);
			
			// add optional attributes
			if(update)
				x.addQuotedAttribute(XMLParser.UPDATE, true);
			if(copyLocalValue)
				x.addQuotedAttribute(XMLParser.COPY_LOCAL_VALUE, true);
			if(update && sep != null)
				x.addQuotedAttribute(XMLParser.SEP, sep);
			
			// add the value and end the tag
			x.addContentAndCloseTag(value);
			first = false;
		}
		
		// close environment tag
		x.endCurrentTag();
		return x.toString();
	}
	
	public boolean isCopyLocalValues() {
		return this.COPY_GLOBAL;
	}
	
	@Override
	public void setColor(String c) {
		this.color = c;
	}
	@Override
	public String getColor() {
		return this.color;
	}
	
	@Override
	public String toString() {
		return this.getName();
	}
	
	@Override
	public void onDeleteProperty() {}
	
	@Override
	public Object[] getDataToLoadOnGUI() { return null; }
}
