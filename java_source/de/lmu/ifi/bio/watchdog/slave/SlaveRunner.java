package de.lmu.ifi.bio.watchdog.slave;
 
import java.io.File;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import de.lmu.ifi.bio.watchdog.executor.WatchdogThread;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.runner.XMLBasedWatchdogRunner;
import de.lmu.ifi.bio.watchdog.slave.clientEvents.KillEventEH;
import de.lmu.ifi.bio.watchdog.slave.clientEvents.TaskExecutorEventEH;
import de.lmu.ifi.bio.watchdog.slave.clientEvents.TerminateTaskEventEH;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Establishes a connection to the master and executes the commands that are send from the master using the local executor class
 * @author Michael Kluge
 *
 */
public class SlaveRunner {

	public static void main(String[] args) {
		Logger log = new Logger(LogLevel.DEBUG);
		SlaveParameters params = new SlaveParameters();
		@SuppressWarnings("unused")
		JCommander parser = null;
		File watchdogXSD = null;
		try { 
			parser = new JCommander(params, args);
			watchdogXSD = new File(params.base + File.separator + XMLBasedWatchdogRunner.XSD_PATH);
			// check, if the base folder could be read
			if(params.base == null || !params.base.isDirectory() || !watchdogXSD.exists() || !watchdogXSD.canRead()) {
				log.error("Can not find the XSD file of Watchdog in '"+ watchdogXSD.getAbsolutePath() +"'.");
				System.exit(1);
			}
			// set temporary folder
			Functions.setTemporaryFolder(params.base + File.separator + XMLParser.TMP_FOLDER);
		}
		catch(ParameterException e) { 
			log.error(e.getMessage());
			new JCommander(params).usage();
			System.exit(1);
		}
		
		// try to connect to the master and register some event handlers
		try {
			// wait until the server sends the termination signal or connection breaks!
			WatchdogThread w = new WatchdogThread(false, params.maxRunning, watchdogXSD, null);
			w.start();
			
			// establish the connection to the server
			Slave s = Slave.getSlave(params.host, params.port, params.id);
			SlaveStatusHandler.initStatusHandler(s);

			// register the needed event handlers 
			s.getEventSocket().registerEventHandler(new KillEventEH(w)); //listen for termination event of slave
			s.getEventSocket().registerEventHandler(new TaskExecutorEventEH(w)); // listen for task execution event
			s.getEventSocket().registerEventHandler(new TerminateTaskEventEH()); // listen for task termination events
			
		}
		catch(Exception e) {
			log.error("Failed to connect to task master on '"+params.host+"' at port '"+params.port+"'!");
			e.printStackTrace();
			System.exit(1);
		}
	}
}
