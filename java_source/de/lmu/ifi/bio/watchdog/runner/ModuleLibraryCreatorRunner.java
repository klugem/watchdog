package de.lmu.ifi.bio.watchdog.runner;

import java.io.File;
import java.util.ArrayList;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import de.lmu.ifi.bio.watchdog.docu.ModuleLibraryGenerator;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;

/**
 * Creates a web-based documentation of modules called module reference book
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
			System.out.println(params.getDescription());
			System.exit(1);
		}
		
		// display the help
		if(params.help) {
			parser.usage();
			System.exit(0);
		}
		else if(params.desc) {
			System.out.println(params.getDescription());
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

			// do the work (= find all documented Watchdog modules & generate the page!)
			int ret = ModuleLibraryGenerator.generateModuleLibraryPage(new File(params.outputDir), watchdogBase, moduleFolders);

			if(ret > 0) 
				log.info("Module library generation succeeded; the library contains " + ret + " modules!");
			else {
				log.error("Module library generation failed or folders contained no documented modules!");
				System.exit(1);
			}
		}
		System.exit(0);
	}
}
