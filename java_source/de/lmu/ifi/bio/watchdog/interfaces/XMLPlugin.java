package de.lmu.ifi.bio.watchdog.interfaces;

/**
 * Interface that must be implemented by classes in order to be dynamically parsed by Watchdog using the plugin system
 * @author kluge
 *
 */
public interface XMLPlugin {

	/**
	 * name of the XML Element (must be unique in the XML document for that type)
	 * @return
	 */
	public String getName();	
}
