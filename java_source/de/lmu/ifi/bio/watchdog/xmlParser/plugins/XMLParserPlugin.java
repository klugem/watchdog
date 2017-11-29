package de.lmu.ifi.bio.watchdog.xmlParser.plugins;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Element;

import de.lmu.ifi.bio.watchdog.interfaces.XMLPlugin;
import de.lmu.ifi.bio.watchdog.logger.Logger;

/**
 * Abstract class that a XMLParserPlugin must implement in order to be dynamically loaded by Watchdog
 * @author kluge
 *
 * @param <A>
 */
public abstract class XMLParserPlugin<A extends XMLPlugin> {

	protected boolean no_exit;
	protected boolean GUI_load_attempt;
	protected final Logger LOGGER;
	protected HashMap<String, A> PARSED_INFO = new HashMap<>();
	
	/**
	 * [IMPORTANT] Extending classes must implement exactly a constructor of the same type or reflection call will fail!
	 * @param l
	 */
	public XMLParserPlugin(Logger l) {
		this.LOGGER = l;
	}
	
	public static <A extends XMLPlugin, B extends XMLParserPlugin<A>> B getInstance(Class<B> c, Logger logger, Boolean noExit, Boolean isGUILoadAttempt) {
		Constructor<? extends B> constructor = null;
		// get constructor
		try {
			constructor = c.getConstructor(Logger.class);
		}
		catch(Exception e) {
			e.printStackTrace();
			logger.error("Failed to find constructor with arguments of type (Logger) for class '"+c.getCanonicalName()+"'.");
			if(!noExit)
				System.exit(1);
		}
		// get instance of class
		if(constructor != null) {
			try {
				B p = constructor.newInstance(logger);
				p.setExitAndGUIInfoAndClear(noExit, isGUILoadAttempt);
				return p;
			}
			catch(Exception e) {
				e.printStackTrace();
				logger.error("Failed to create a instance of class '"+c.getCanonicalName()+"'.");
				if(!noExit)
					System.exit(1);
			}
		}
		return null;
	}
	
	/**
	 * return the name of the tag in XML format that can be parsed by that class
	 * @return
	 */
	public abstract String getNameOfParseableTag();
	
	/**
	 * name of the parent tag of the element
	 * @return
	 */
	public abstract String getNameOfParentTag();

	/**
	 * parses the XML element and returns the XMLPlugin or null if it fails
	 * @param el
	 * @param watchdogBaseDir
	 * @param additionalData
	 * @return
	 */
	public abstract A parseElement(Element el, String watchdogBaseDir, Object[] additionalData);
	
	/**
	 * parses the element and stores it for later internal use
	 * @param el
	 * @param watchdogBaseDir
	 * @param additionalData
	 * @return
	 */
	public A parse(Element el, String watchdogBaseDir, Object[] additionalData) {
		A info = parseElement(el, watchdogBaseDir, additionalData);
		this.PARSED_INFO.put(info.getName(), info);
		return info;
	}
	
	/**
	 * Path to the file with the corresponding XSD definition
	 * path must be relative to Watchdog's XSD directory
	 * @return
	 */
	public abstract String getXSDDefinition();
	
	
	/**
	 * runs an additional test on an element
	 * @param name
	 */
	public abstract void runAdditionalTestsOnElement(String name);
	
	/**
	 * runs tests on all plugins part of the hashmap
	 * @param elementsToTest
	 */
	public void runAdditionalTest(HashSet<String> elementsToTest) {
		Set<String> testKeys = new HashSet<>();
		testKeys.addAll(this.PARSED_INFO.keySet());
		for(String exName : testKeys) {
			if(elementsToTest.contains(exName)) // run test on element
				this.runAdditionalTestsOnElement(exName);
			else // unused --> remove it
				this.PARSED_INFO.remove(exName);
		}
	}

	public void setExitAndGUIInfoAndClear(boolean noExit, boolean isGUILoadAttempt) {
		this.PARSED_INFO.clear();
		this.no_exit = noExit;
		this.GUI_load_attempt = isGUILoadAttempt;
	}
}
