package de.lmu.ifi.bio.watchdog.helper.ProcessBlock;

import java.util.HashMap;

/**
 * Class for multi param process block
 * @author Michael Kluge
 *
 */
public abstract class ProcessMultiParam extends ProcessBlock {

	private static final long serialVersionUID = 319332260168367863L;
	protected final HashMap<String, Integer> NAME_MAPPING = new HashMap<>();
	
	public ProcessMultiParam(String name, String path, String baseName) {
		super(name, path, baseName);
	}
	
	/**
	 * returns the name to column mapping
	 * @return
	 */
	public HashMap<String, Integer> getNameMapping() {
		return this.NAME_MAPPING;
	}
}
