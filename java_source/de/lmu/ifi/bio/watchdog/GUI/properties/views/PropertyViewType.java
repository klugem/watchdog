package de.lmu.ifi.bio.watchdog.GUI.properties.views;

import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Constants;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessBlock;

/**
 * Types of properties that exist
 * @author kluge
 *
 */
public enum PropertyViewType {
	ENVIRONMENT("environment variables", "environment property", Environment.class), EXECUTOR("executors", "executor property", ExecutorInfo.class), PROCESS_BLOCK("process blocks", "process block property", ProcessBlock.class), CONSTANTS("constants", "constant", Constants.class);
	
	private final String LABEL;
	private final String PROPERTY_NAME;
	private final Class<? extends XMLDataStore> TYPE;
	
	PropertyViewType(String label, String propertyName, Class<? extends XMLDataStore> type) {
		this.LABEL = label;
		this.PROPERTY_NAME = propertyName;
		this.TYPE = type;
	}
	
	public static PropertyViewType getCorrespondingType(Class<? extends XMLDataStore> type) {
		for(PropertyViewType t : values()) {
			if(type.equals(t.TYPE))
				return t;
		}
		return null;
	}
	
	public String getLabel() {
		return this.LABEL;
	}
	
	public String getPropertyName() {
		return this.PROPERTY_NAME;
	}
}
