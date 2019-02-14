package de.lmu.ifi.bio.watchdog.runner;

import java.io.File;
import java.util.ArrayList;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.watchdog.docu.ModuleLibraryGenerator;

/**
 * Creates a web-based documentation of modules 
 * @author Michael Kluge
 *
 */
public class ModuleLibraryCreatorRunner extends BasicRunner {
		
	public static void main(String[] args) {
		Logger log = new Logger(LogLevel.INFO);
		ModuleLibraryCreatorParameters params = new ModuleLibraryCreatorParameters();
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
		// generation mode
		else {
			if(params.module.size() == 0) {
				log.error("At least one module folder must be given!");
				System.exit(1);
			}
			// find Watchdog's base to work with
			File b = null;
			if(params.watchdogBase != null && params.watchdogBase.length() > 0)
				b = new File(params.watchdogBase);
			File watchdogBase = findWatchdogBase(b);
			if(watchdogBase == null) {
				log.error("Failed to find Watchdog's install directory! Please use -w to provide it.");
				System.exit(1);
			}
					
			// ensure that the outputFolder folder is there
			File outputFolder = null;
			if(params.outputFolder != null && params.outputFolder.length() >0) 
				outputFolder = new File(params.outputFolder);
			else {
				log.error("Output folder must be given!");
			}
				
			if(!outputFolder.exists()) {
				if(!outputFolder.mkdirs()) {
					log.error("Could not output folder '"+outputFolder.getAbsolutePath()+"'.");
					System.exit(1);
				}
			}
			if(!outputFolder.canRead() || !outputFolder.canWrite()) {
				log.error("Could not read/write to output folder '"+outputFolder.getAbsolutePath()+"'.");
				System.exit(1);
			}
			// ensure that the entered module folders exist
			ArrayList<String> moduleFolders = new ArrayList<>();
			for(String ms : params.module) {
				File msf = new File(ms);
				if(!msf.exists() || !msf.isDirectory() || !msf.canRead()) {
					if(!msf.exists())
						log.error("Module folder '"+msf.getAbsolutePath()+"' does not exist.");
					else if(!msf.canRead())
						log.error("Module folder '"+msf.getAbsolutePath()+"' is not readable.");
					else if(!msf.isDirectory())
						log.error("Module folder '"+msf.getAbsolutePath()+"' is not a directory.");
					System.exit(1);
				}
				moduleFolders.add(msf.getAbsolutePath());
			}
			
			// do the work!
			Functions.filterErrorStream();
			int modules = ModuleLibraryGenerator.generateModuleLibraryPage(outputFolder, watchdogBase, moduleFolders);
			log.info("Module library generation succeeded; the library contains " + modules + " modules.");
		}
		System.exit(0);
	}
}
