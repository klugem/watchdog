package de.lmu.ifi.bio.watchdog.helper;

import java.io.Serializable;
import java.util.HashMap;

import de.lmu.ifi.bio.watchdog.GUI.WorkflowDesignController;

/**
 * Classes that should be used by GUI must implement that interface
 * @author kluge
 *
 */
public abstract interface XMLDataStore extends Serializable {
	static final long serialVersionUID = -1910178648862889040L;
	static final String SEP = "~][~";
	static final HashMap<String, XMLDataStore> STORE = new HashMap<>();	
	public abstract String toXML();
	
	public default Class<? extends XMLDataStore> getStoreClassType() {
		return this.getClass();
	}

	public abstract String getName();
	
	public default String getRegisterName() {
		return getRegisterName(this.getName(), this.getStoreClassType());
	}
	
	public abstract void setColor(String c);
	
	public abstract String getColor();
	
	public default boolean hasColor() {
		return this.getColor() != null && this.getColor().length() > 0;
	}
	
	@SuppressWarnings("rawtypes")
	public static String getRegisterName(String name, Class c) {
		if(c == null)
			return name;
		else
			return name + SEP + c.getSimpleName();
	}
	
	/**
	 * registers a XMLDataStore that is currently in use
	 * @param data
	 * @return
	 */
	public static boolean registerData(XMLDataStore data) {
		if(!hasRegistedData(data.getRegisterName(), null)) {
			STORE.put(data.getRegisterName(), data);
			WorkflowDesignController.configureHasChanged();
			return true;
		}
		return false;
	}
	
	/**
	 * checks, if a data with that name is currently in use
	 * @param name
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static boolean hasRegistedData(String name, Class c) {
		return STORE.containsKey(getRegisterName(name, c));
	}
	
	public static void clearAllRegisteredData() {
		STORE.clear();
	}
	
	/**
	 * returns the registered data with that name or null if no data is there
	 * @param name
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static XMLDataStore getRegistedData(String name, Class c) {
		return STORE.get(getRegisterName(name, c));
	}
		
	/**
	 * unregisters the data from use
	 * @param data
	 * @return
	 */
	public static boolean unregisterData(XMLDataStore data) {
		if(hasRegistedData(data.getRegisterName(), null)) {
			WorkflowDesignController.configureHasChanged();
			return STORE.remove(data.getRegisterName()) != null;
		}
		return false;
	}

	/**
	 * is called when the property is readlly deleted on the GUI
	 */
	public abstract void onDeleteProperty();
}
