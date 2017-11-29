package de.lmu.ifi.bio.watchdog.helper.ProcessBlock;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;

/**
 * Base class for process group and process loop
 * @author Michael Kluge
 *
 */
public abstract class ProcessBlock implements XMLDataStore {
		
	private static final long serialVersionUID = -8354009798848646906L;
	protected final static Pattern BASE_SPLIT = Pattern.compile("(?<=/)|(?=/)");
	public String gui_basename;
	public String gui_splitPath;
	
	private final String NAME;
	private String color;
	
	public ProcessBlock(String name, String path, String baseName) {
		this.NAME = name;
		
		// test if we have a basename and if yes, split it
		if((baseName == null || baseName.length() == 0) && path != null) {
			String pBak = path;
			baseName = "";
			String[] tmp = ProcessMultiParam.BASE_SPLIT.split(path);
			if(tmp.length > 1) {
				path = tmp[tmp.length-1];
				int stop = 0;
				if(File.separator.equals(path)) {
					path = tmp[tmp.length-2] + path;
					stop = 1;
				}
				for(int i = 0; i < tmp.length-2-stop; i++) { // we don't want the last split char to be part as it is added afterwards again.
					baseName += tmp[i];
				}
				// account for /x patterns
				if(baseName.length() == 0 && pBak.startsWith(File.separator) && !path.startsWith(File.separator)) {
					path = File.separator + path;
				}
			}
		}
		// update GUI vars
		this.gui_basename = baseName;
		this.gui_splitPath = path;
	}
	
	/**
	 * retuns the split path without the basename
	 * @return
	 */
	public String getSplitPath() {
		return this.gui_splitPath;
	}

	/**
	 * Returns the values of this block the function can work with
	 * key: group name --> value
	 * @return
	 */
	public abstract HashMap<String, String> getValues();
	
	/**
	 * number of elements in this processblock
	 * @return
	 */
	public abstract int size();
	
	@Override
	public String getName() {
		return this.NAME;
	}
	
	/**
	 * variables that are offered by that process block to be used as replacement
	 * @return
	 */
	public abstract LinkedHashMap<String, String> getOfferedVariables();
	
	@Override
	public void setColor(String c) {
		this.color = c;
	}
	@Override
	public String getColor() {
		return this.color;
	}
	
	@Override
	public Class<? extends XMLDataStore> getStoreClassType() {
		return ProcessBlock.class;
	}
	
	@Override
	public void onDeleteProperty() {}

	public static String cleanFilename(String filename) {
		return filename.replaceAll(File.separator + "+",File.separator);
	}
}
