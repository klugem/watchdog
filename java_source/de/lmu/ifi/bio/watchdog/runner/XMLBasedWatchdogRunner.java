package de.lmu.ifi.bio.watchdog.runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ggf.drmaa.DrmaaException;
import org.xml.sax.SAXException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import de.lmu.ifi.bio.watchdog.executor.Executor;
import de.lmu.ifi.bio.watchdog.executor.HTTPListenerThread;
import de.lmu.ifi.bio.watchdog.executor.WatchdogThread;
import de.lmu.ifi.bio.watchdog.helper.Mailer;
import de.lmu.ifi.bio.watchdog.helper.PatternFilenameFilter;
import de.lmu.ifi.bio.watchdog.helper.returnType.ReturnType;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask2TaskThread;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Runner for the XML Parser which reads an XML file and executes all containing tasks
 * @author Michael Kluge
 *
 */
public class XMLBasedWatchdogRunner implements SignalHandler {
	private static final Signal SIGTERM = new Signal("TERM"); // kill request
	private static final Signal SIGINT = new Signal("INT"); // Strg + C
	private static final String BASE_STRING = "watchdogBase"; 
	private static final Pattern WATCHDOG_BASE = Pattern.compile("<"+XMLParser.ROOT+".+"+BASE_STRING+"=\"([^\"]+)\".+"); 
	public static final String XSD_PATH = "xsd" + File.separator + "watchdog.xsd";
	private static final String TOOL_NAME = "Watchdog";  
	private static final String VERSION = "version: 1.2.7";
	private static final String REVISION = getRevisionNumber();        
	public static final String LOG_SEP = "#########################################################################################";
	public static int PORT =  WatchdogThread.DEFAULT_HTTP_PORT;
	public final static int SLEEP = 500; // check every 0.5s if all tasks are finished!
	private static final String XML_PATTERN = "*.xml";
	public static final String ENV_WATCHDOG_HOME_NAME = "WATCHDOG_HOME";
	
	@SuppressWarnings({ "unchecked" })
	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException, DrmaaException, InterruptedException {
		Logger log = new Logger(LogLevel.DEBUG);
		Parameters params = new Parameters();
		JCommander parser = null;
		try { 
			parser = new JCommander(params, args); 
		}
		catch(ParameterException e) { 
			log.error(e.getMessage());
			new JCommander(params).usage();
			System.exit(1);
		}
		
		// display the help
		if(params.help) {
			parser.usage();
			System.exit(0);
		}
		else if(params.version) {
			System.out.println(getVersion());
			System.exit(0);
		}
		// validate mode
		else if(params.validate) {
			File xml = new File(params.xml);				
			// process the complete folder
			if(xml.isDirectory()) {
				
				// check all files in the example folder
				int succ = 0;
				for(File xmlFile : xml.listFiles(new PatternFilenameFilter(XML_PATTERN, false))) {
					String xmlFilename = xmlFile.getAbsolutePath();
					log.info("Validating '" + xmlFilename + "'...");
					XMLParser.parse(xmlFilename, findXSDSchema(xmlFilename, params.useEnvBase, log).getAbsolutePath(), params.ignoreExecutor, false, false, true, params.disableCheckpoint, params.forceLoading, params.disableMails);
					succ++;
				}
				System.out.println("Validation of " + succ + " files stored in '"+ xml.getCanonicalPath() +"' succeeded.");
			}
			// process only that file
			else {				
				XMLParser.parse(xml.getAbsolutePath(), findXSDSchema(xml.getAbsolutePath(), params.useEnvBase, log).getAbsolutePath(), params.ignoreExecutor, false, false, true, params.disableCheckpoint, params.forceLoading, params.disableMails);
				System.out.println("Validation of '"+ xml.getCanonicalPath() +"' succeeded!");
			}
			System.exit(0);
		}
		// normal run mode
		else {
			// get parsed parameters 
			File xmlPath = new File(params.xml);
			File logFile = null;
			if(params.log != null) 
				logFile = new File(params.log);
			int port = params.port;
			PORT = port; // copy that for other classes which want to access that
			int startID = params.start;
			int stopID = params.stop;
			boolean enforceNameUsage = false;
			String[] include = params.include.toArray(new String[0]);
			String[] exclude = params.exclude.toArray(new String[0]);
			
			// check, if id mapping must be enforced!
			for(String sID : (String[]) ArrayUtils.addAll(include, exclude)) {
				try { Integer.parseInt(sID); }
				catch(Exception e) { enforceNameUsage = true; }
			}
			
			File xsdSchema = null;
			File mailConfig = null;
			if(params.mailConfig != null) {
				mailConfig = new File(params.mailConfig);
				if(!(mailConfig.exists() && mailConfig.canRead() && mailConfig.isFile())) {
					log.error("Could not find mail config file '"+mailConfig.getAbsolutePath()+"'.");
					System.exit(1);
				}
			}
			// test if include and exclude is set ?!
			if(include.length > 0 && exclude.length > 0) {
				log.error("Parameters '-include' and '-exclude' can not be used at the same time.");
				System.exit(1);
			}
			
			// check, if the port is ok
			if(!portOK(port)) {
				log.error("Port '"+port+"' is already used by another programm.");
				System.exit(1);
			}
			// check, if start and stop ID are ok
			if(startID > stopID) {
				log.error("Start id must be smaller or equal than stop id ('"+startID+"' vs '"+stopID+"')!");
				System.exit(1);
			}
			if(!(xmlPath.isFile() && xmlPath.canRead())) {
				log.error("Can not find XML file '"+ xmlPath +"'");
				System.exit(1);
			}
			// find path to base XSD dir
			else {
				xsdSchema = findXSDSchema(xmlPath.getAbsolutePath(), params.useEnvBase, log);
				
				if(xsdSchema == null) {
					log.error("XML file '"+ xmlPath.getAbsolutePath() +"' is lacking the '"+BASE_STRING+"' attribute.");
					System.exit(1);
				}
			}
			
			if(!(xsdSchema.isFile() && xsdSchema.canRead())) {
				log.error("Can not find XSD watchdog schema '"+ xsdSchema.getAbsolutePath() +"'");
				System.exit(1);
			}
			
			// install signals to handle
			Signal.handle(SIGINT, new XMLBasedWatchdogRunner());
			
			log.info("XML file: " + xmlPath.getAbsolutePath());
			log.info("XSD file: " + xsdSchema.getAbsolutePath());
			if(logFile != null) 
				log.info("Log file: " + logFile.getAbsolutePath());
			else
				log.info("Log file: ** not saved **");

			// parse the XML Tasks
			Object[] ret = XMLParser.parse(xmlPath.getAbsolutePath(), xsdSchema.getAbsolutePath(), params.ignoreExecutor, enforceNameUsage, false, false, params.disableCheckpoint, params.forceLoading, params.disableMails);
			ArrayList<XMLTask> xmlTasks = (ArrayList<XMLTask>) ret[0];
			String mail = (String) ret[1];
			HashMap<String, Pair<HashMap<String, ReturnType>, String>> retInfo = (HashMap<String, Pair<HashMap<String, ReturnType>, String>>) ret[3];
			HashMap<String, Integer> name2id = (HashMap<String, Integer>) ret[4]; 
			
			log.info("Parsed " + xmlTasks.size() + " from the provided XML file.");

			// check, if some of the tasks should be removed
			if(startID != Integer.MIN_VALUE) {
				for(XMLTask x : new ArrayList<XMLTask>(xmlTasks)) {
					// remove complete task
					if(x.getXMLID() < startID) {
						xmlTasks.remove(x);
						continue;
					}
					// only remove dependencies, but remove the complete task, if something is removed and it is a processInput node)
					if(x.removeDependenciesCut(startID, true))
						xmlTasks.remove(x);
				}
			}
			if(stopID != Integer.MAX_VALUE) {
				for(XMLTask x : new ArrayList<XMLTask>(xmlTasks)) {
					// remove complete task
					if(x.getXMLID() > stopID) {
						xmlTasks.remove(x);
						continue;
					}
					// only remove dependencies, but remove the complete task, if something is removed and it is a processInput node)
					if(x.removeDependenciesCut(stopID, false))
						xmlTasks.remove(x);		
				}
			}
			// create hash of include list
			if(include.length > 0) {
				HashSet<Integer> includeHash = new HashSet<>();
				for(String sID : include) {
					int id = getStringID2int(sID, name2id);
					includeHash.add(id);
				}
				// remove all tasks that are not included
				for(int id : XMLTask.getXMLTasks().keySet()) {
					if(!includeHash.contains(id)) {
						if(XMLTask.hasXMLTask(id))
							removeTaskFromList(XMLTask.getXMLTask(id), xmlTasks);
					}
				}
			}
			// apply exclude
			for(String sID : exclude) {
				int id = getStringID2int(sID, name2id);
				if(XMLTask.hasXMLTask(id))
					removeTaskFromList(XMLTask.getXMLTask(id), xmlTasks);
			}
		
			File watchdogBase = xsdSchema.getParentFile().getParentFile();
			// create thread that listens to the HTTP server
			HTTPListenerThread control = new HTTPListenerThread(params.port, xmlTasks, watchdogBase.getAbsolutePath());
			try {
				control.start();
			}
			catch(Exception e) {
				log.error("Webserver can not bind to port '"+port+"'. Ports 1 to 1023 might be protected by the system for privileged usage. Please use another one.");
				e.printStackTrace();
				System.exit(1);
			}
			
			// create mailer
			Mailer mailer = null;
			if(mail != null && mail.length() > 0) {
				if(mailConfig == null)
					mailer = new Mailer(mail);
				else
					mailer = new Mailer(mail, mailConfig);
				
				Task.setMail(mailer);
			}

			// create a new watchdog object and xml2 thread stuff
			WatchdogThread watchdog = new WatchdogThread(params.simulate, null, xsdSchema, logFile); 
			watchdog.setWebserver(control);
			XMLTask2TaskThread xml2taskThread = new XMLTask2TaskThread(watchdog, xmlTasks, mailer, retInfo, xmlPath, params.mailWaitTime);
			
			WatchdogThread.addUpdateThreadtoQue(xml2taskThread, true);
			Executor.setXml2Thread(xml2taskThread);
			Executor.setWatchdogBase(watchdogBase);
			watchdog.start();

			// do not end program
			while(xml2taskThread.hasUnfinishedTasks()) {
				Thread.sleep(SLEEP);
			}
			// execute shutdown commands
			watchdog.shutdown();

			// print or send goodbye message
			if(Task.isMailSet())
				Task.getMailer().goodbye(xmlTasks);
			else {
				System.out.println(LOG_SEP);
				System.out.println(Mailer.getGoodbyeTxt(xmlTasks));
				System.out.println(LOG_SEP);				
			}
			
			log.info("All Tasks are finished!");
			
			// stop the threads
			control.stop();
			watchdog.requestStop(5, TimeUnit.SECONDS);
			System.exit(0);
		}
	}
	
	private static String getRevisionNumber() {
		Properties prop = new Properties();
		try {
		    prop.load(XMLBasedWatchdogRunner.class.getClassLoader().getResourceAsStream("de/lmu/ifi/bio/watchdog/helper/.svn_revision_number.properties"));
		    return prop.getProperty("build.current.revision");
		} 
		catch (IOException ex) {}
		return "unknown build";
	}

	public static String getVersion() {
		return TOOL_NAME + " - " + VERSION + " (" + getRevision() +"r)";
	}

	/**
	 * gets an int ID based on a String ID
	 * @param sID
	 * @param name2id
	 * @return
	 */
	private static int getStringID2int(String sID, HashMap<String, Integer> name2id) {
		if(name2id.containsKey(sID))
			return name2id.get(sID);
		else
			return Integer.parseInt(sID);
	}
	
	/**
	 * removes an ID from the list
	 * @param x
	 * @param xmlTasks
	 */
	private static void removeTaskFromList(XMLTask x, ArrayList<XMLTask> xmlTasks) {
		xmlTasks.remove(x);
		for(XMLTask xx : new ArrayList<XMLTask>(xmlTasks)) {
			if(xx.removeDependencies(x.getXMLID())) {
				removeTaskFromList(xx, xmlTasks);
			}
		}
	}

	/**
	 * returns the SVN revision of the last commit
	 * @return
	 */
	private static String getRevision() {
		return REVISION;
	}

	/**
	 * returns the xsd schema, extracted from a XML file
	 * @param xmlPath
	 * @return
	 */
	public static File findXSDSchema(String xmlPath, boolean overrideWithEnvWatchdogHomeVar, Logger log) {
		if(overrideWithEnvWatchdogHomeVar) {
			String home = System.getenv(ENV_WATCHDOG_HOME_NAME);
			if(home == null || home.length() == 0) {
				log.error("Environment variable '"+ ENV_WATCHDOG_HOME_NAME +"' is not set or missing. Disable the -useEnvBase flag or set the variable correctly.");
				System.exit(1);
			}
			return new File(home + File.separator + XSD_PATH);
		}
		try {
			BufferedReader bf = new BufferedReader(new FileReader(xmlPath));
			String line;		
			Matcher m;
			while((line = bf.readLine()) != null) {
				m = WATCHDOG_BASE.matcher(line);
				if(m.matches()) {
					bf.close();
					return new File(m.group(1) + File.separator + XSD_PATH);
				}
			}
			bf.close();
		}
		catch(Exception e) {}
		return null;
	}

	@Override
	public void handle(Signal arg0) {
		if(SIGINT.equals(arg0)) {
			System.out.println("Do you really want to terminate Watchdog and abort all running tasks? (Y or N)");
			try {
				int b;
				boolean first = true;
				while((b = System.in.read()) != -1) {
				// send termination event
					if(first) { 
						if(b == ((int) 'Y') && ((b = System.in.read()) == 10 || b == 13)) {
							if(Task.getMailer() != null)
								Task.getMailer().setOrderedShutdown();
							Signal.raise(SIGTERM);
							return; 
						}
						else if(b == ((int) 'N')  && ((b = System.in.read()) == 10 || b == 13))
							return;
						
						first = false; 
					}
					if(b == 10) // newline end handling.
						break;
				} 
				// if we are still here, entry was not Y or N!
				System.out.println("Only 'Y' (yes) or 'N' (no) are valid answers!");
			}
			catch(Exception e) { e.printStackTrace(); }
		}
	}
	
	/**
	 * checks, if the port is in use by another tool
	 * @param p
	 * @return
	 */
	@SuppressWarnings("resource")
	public static boolean portOK(int p) {
	    try {
	    	@SuppressWarnings("unused")
			Socket ignored = new Socket("localhost", p);
	    	return false;
	    } catch (IOException e) {
	        return true;
	    }
	}
}
