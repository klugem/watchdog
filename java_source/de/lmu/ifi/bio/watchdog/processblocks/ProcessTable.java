package de.lmu.ifi.bio.watchdog.processblocks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.commons.lang3.StringUtils;

import de.lmu.ifi.bio.watchdog.helper.ReplaceSpecialConstructs;
import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Reads the information from a tab based file
 * @author Michael Kluge
 *
 */
public class ProcessTable extends ProcessMultiParam {

	private static final long serialVersionUID = -1469627092542745865L;
	private final File TABLE;
	private boolean wasRead = false;
	private final ArrayList<String> BUFFER = new ArrayList<>();
	private final LinkedHashMap<String, String> RES = new LinkedHashMap<>();
	private static final Logger LOGGER = new Logger(LogLevel.WARNING);
	private final String KEY_COLUMN;
	private static final LinkedHashMap<String, String> OFFER_VAR = new LinkedHashMap<>();
	
	// only used as data store for GUI
	public boolean gui_disableExistanceCheck;
	public String gui_compareColum;
	public String gui_table;
	private static final String REPLACE = "COL_NAME";
	
	static {
		OFFER_VAR.put("[$"+REPLACE+"]", "value stored in the column named $COL_NAME");
		OFFER_VAR.put("[$"+REPLACE+", n, sep]", "n suffixes of the value stored in the column named $COL_NAME");
	}
	
	public ProcessTable(String name, File table, String keyColumn, boolean disableExistanceCheck) {
		super(name, null, null);
		this.TABLE = table;
		this.KEY_COLUMN = keyColumn;
		this.gui_disableExistanceCheck = disableExistanceCheck;
		
		// read file into buffer and process the stuff
		if(!disableExistanceCheck)
			this.readFile();
	}
	
	public ProcessTable(String name, String table, String baseName, String compareColum, boolean disableExistanceCheck) {
		super(name, table, baseName);
		
		this.gui_table = this.getSplitPath(); // calculated by parent class
		this.gui_compareColum = compareColum;
		this.gui_disableExistanceCheck = disableExistanceCheck;
		
		if(this.gui_basename == null || this.gui_basename.length() == 0) {
			this.TABLE = new File(table);
		}
		else {
			table = this.gui_table;
			this.gui_table = this.gui_basename + File.separator + this.gui_table;
			String basenameReplaced = XMLParser.replaceConstants(this.gui_basename, XMLParser.getLastReadConstants());
			this.TABLE = new File(basenameReplaced + File.separator + table);
		}
		this.KEY_COLUMN = compareColum;
	}
	
	@Override
	public HashMap<String, Integer> getNameMapping(boolean hasNoMoreGlobalDependencies) {
		if(hasNoMoreGlobalDependencies || !this.gui_disableExistanceCheck)
			this.readFile();
		return super.getNameMapping(hasNoMoreGlobalDependencies);
	}
	
	/**
	 * reads the file, which is given as input
	 */
	private void readFile() {
		String tpath = this.TABLE.getPath();
		if(XMLParser.getParentOfCurrentlyParsedFilePath() != null)
			tpath = tpath.replace("${"+XMLParser.WF_PARENT_BLOCKED_CONST+"}", XMLParser.getParentOfCurrentlyParsedFilePath());
		
		File table = new File(tpath);
		// buffer the result the first time this function is called.
		if(!this.wasRead) {
			if(!table.exists()) {
				LOGGER.error("Process table file '" + table.getAbsolutePath() + "' was not found!");
				if(!XMLParser.isNoExit()) System.exit(1);
			}
			try {
				this.BUFFER.addAll(Files.readAllLines(Paths.get(table.getAbsolutePath()))); 
						
				// create the mapping
				if(this.BUFFER.size() == 0) {
					LOGGER.error("Process table file '" + table.getAbsolutePath() + "' does not contain any lines!");
					if(!XMLParser.isNoExit()) System.exit(1);
				}
				else {
					if(this.BUFFER.size() == 1) {
						LOGGER.warn("Process table file '" + table.getAbsolutePath() + "' does only contain a header line!");
					}
					
					String[] names =  this.BUFFER.remove(0).split(ReplaceSpecialConstructs.TAB);
					for(int i = 0; i < names.length; i++)
						this.NAME_MAPPING.put(names[i], i+1);
					
					// find the key column 
					int keyID = 0;
					boolean noKey = false;
					// check, if a valid key column is there
					if(this.KEY_COLUMN == null || this.KEY_COLUMN.length() == 0)
						noKey = true;
					else {
						if(!this.NAME_MAPPING.containsKey(this.KEY_COLUMN)) {
							LOGGER.error("Process table file '" + table.getAbsolutePath() + "' does not contain a column named '"+this.KEY_COLUMN+"'!");
							if(!XMLParser.isNoExit()) System.exit(1);
						}
						else
							keyID = this.NAME_MAPPING.get(this.KEY_COLUMN)-1;
					}
					
					// add the entries
					String key;
					String e[];
					int c = 2;
					int mustHave = this.NAME_MAPPING.size();
					for(String v : this.BUFFER) {
						// ensure that all lines have the same number of elements
						e = v.split(ReplaceSpecialConstructs.TAB);
						if(e.length != mustHave) {
							LOGGER.error("Line " + c + " of file '"+table.getAbsolutePath()+"' has " + e.length + " columns but " + mustHave + " are expected!");
							LOGGER.error("content of line: '"+v+"'");
							LOGGER.error("Column names: " + StringUtils.join(this.NAME_MAPPING, " "));
							if(!XMLParser.isNoExit()) System.exit(1);
						}
						if(noKey)
							key = v;
						else
							key = e[keyID];
						this.RES.put(key, v);
						c++;
					}
					
				} 
			} catch (IOException e) {
				e.printStackTrace();
				LOGGER.error("Failed to read file '" + table.getAbsolutePath() + "'!");
				if(!XMLParser.isNoExit()) System.exit(1);
			}
			this.wasRead = true;
		}
	}

	@Override
	public LinkedHashMap<String, String> getValues(boolean cachedCall) {
		// cachedCall parameter is ignored here as Wachdog does only read the table file once
		this.readFile();
		return new LinkedHashMap<String, String>(this.RES);
	}
	
	@Override
	public int size() {
		return this.RES.size();
	}

	@Override
	public String toXML() {
		XMLBuilder x = new XMLBuilder();
		// start with basic tag
		x.startTag(XMLParser.PROCESS_TABLE, false);
		x.addQuotedAttribute(XMLParser.NAME, this.getName());
		x.addQuotedAttribute(XMLParser.TABLE, ProcessBlock.cleanFilename(this.gui_table));
		
		// add optional attributes
		if(this.gui_disableExistanceCheck)
			x.addQuotedAttribute(XMLParser.DISABLE_EXISTANCE_CHECK, this.gui_disableExistanceCheck);
		if(!(this.gui_compareColum == null || this.gui_compareColum.length() == 0))
			x.addQuotedAttribute(XMLParser.COMPARE_NAME, this.gui_compareColum);
		if(this.hasColor())
			x.addQuotedAttribute(XMLParser.COLOR, this.getColor());
		
		// end the tag
		x.endCurrentTag();
		return x.toString();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public LinkedHashMap<String, String> getOfferedVariables(ArrayList<String> replace) {
		LinkedHashMap<String, String> ret = (LinkedHashMap<String, String>) OFFER_VAR.clone();
		
		// replace the values with the actual ones
		if(!this.gui_disableExistanceCheck) {
			for(String key : OFFER_VAR.keySet()) {
				for(String name : this.getNameMapping().keySet()) {
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
		return false;
	}
	
	@Override
	public boolean addsReturnInfoToTasks() {
		return false;
	}
	
	@Override
	public Object[] getDataToLoadOnGUI() { 
		return new Object[] { this.gui_table, this.gui_compareColum, !this.gui_disableExistanceCheck }; 
	}

	@Override
	public boolean allowsReturnValueVerification() {
		return this.wasRead;
	}
}
