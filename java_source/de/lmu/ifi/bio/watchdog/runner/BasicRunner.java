package de.lmu.ifi.bio.watchdog.runner;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Properties;

import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Basic runner functions
 * @author Michael Kluge
 *
 */
public abstract class BasicRunner {
	public static final Logger LOGGER = new Logger();
	public static final String XSD_PATH = XMLParser.XSD + File.separator + "watchdog.xsd";
	private static final String TOOL_NAME = "Watchdog";  
	private static final String VERSION = "version: 2.0.8 beta";
	private static final String REVISION = getRevisionNumber();    
	private static final String DEV_DIR = "watchdog";
	private static final String DIST_DIR = "..";
	
	/**
	 * tries to find the watchdog base dir based on the location of the jar file
	 * @return
	 */
	public static File findWatchdogBase(File base) {
		try {
			// try to find base dir on it's own
			if(base == null) {
				String path = BasicRunner.class.getProtectionDomain().getCodeSource().getLocation().getPath();
				base = new File(URLDecoder.decode(path, "UTF-8")).getParentFile();
			}
			
			// test if it is a deployed version or not
			if(new File(base.getAbsolutePath() + File.separator + XSD_PATH).canRead())
				return base.getAbsoluteFile();
			else if(new File(base.getAbsolutePath() + File.separator + DEV_DIR + File.separator + XSD_PATH).canRead())
				return new File(base.getAbsolutePath() + File.separator + DEV_DIR).getAbsoluteFile();
			else if(new File(base.getAbsolutePath() + File.separator + DIST_DIR + File.separator + XSD_PATH).canRead())
				return new File(base.getAbsolutePath() + File.separator + DIST_DIR).getAbsoluteFile();
			else {
				LOGGER.error("Faild to find Watchdog's install directory in '"+base.getAbsolutePath()+"'.");
				return null;
			}
		}
		catch(Exception e) {
			LOGGER.error("Faild to find Watchdog's install directory.");
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	
	/**
	 * returns the current SVN revision number
	 * @return
	 */
	private static String getRevisionNumber() {
		Properties prop = new Properties();
		try {
		    prop.load(XMLBasedWatchdogRunner.class.getClassLoader().getResourceAsStream("de/lmu/ifi/bio/watchdog/helper/.svn_revision_number.properties"));
		    return prop.getProperty("build.current.revision");
		} 
		catch (IOException ex) {}
		return "unknown build";
	}

	/**
	 * creates the version string
	 * @return
	 */
	public static String getVersion() {
		return TOOL_NAME + " - " + VERSION + " (" + getRevision() +"r)";
	}

	/**
	 * returns the SVN revision of the last commit
	 * @return
	 */
	public static String getRevision() {
		return REVISION;
	}
}
