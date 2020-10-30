package de.lmu.ifi.bio.watchdog.processblocks;

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
	 * @param hasNoMoreGlobalDependencies should be, set if task should be executed NOW!
	 * @return
	 */
	public HashMap<String, Integer> getNameMapping(boolean hasNoMoreGlobalDependencies) {
		return this.NAME_MAPPING;
	}
	
	/**
	 * returns the name to column mapping
	 */
	public HashMap<String, Integer> getNameMapping() {
		return this.getNameMapping(false);
	}
	
	@Override
	public boolean isResumeReattachValueAddingRequired() {
		return false;
	}
	
	/**
	 * indicates if the names of return values can be verified during parsing of the XML file
	 * @return
	 */
	public abstract boolean allowsReturnValueVerification();
}
