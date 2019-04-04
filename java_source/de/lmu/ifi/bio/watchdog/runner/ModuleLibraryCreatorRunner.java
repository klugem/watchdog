package de.lmu.ifi.bio.watchdog.runner;

import java.io.File;
import java.util.ArrayList;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;

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
			//Functions.filterErrorStream();
		//	ArrayList<Pair<File, File>> moduleXSDandXMLDocuFiles = DocuXMLParser.findAllDocumentedModules(
			//ArrayList<Moduledocu> documentedModules = DocuXMLParser.parseAllXMLFiles(watchdogBase.getAbsolutePath(), params.module);
	/*	XMLParser.getXSDCacheDir(tmpBaseDir)
		
		else if(params.validate) {
			File xml = new File(params.xml);				
			// process the complete folder
			if(xml.isDirectory()) {
				
				// check all files in the example folder
				int succ = 0;
				for(File xmlFile : xml.listFiles(new PatternFilenameFilter(XMLBasedWatchdogRunner.XML_PATTERN, false))) {
					String xmlFilename = xmlFile.getAbsolutePath();
					log.info("Validating '" + xmlFilename + "'...");
					Object[] = XMLParser.parse(xmlFilename, findXSDSchema(xmlFilename, params.useEnvBase, log).getAbsolutePath(), params.tmpFolder, params.ignoreExecutor, false, false, true, params.disableCheckpoint, params.forceLoading, params.disableMails);
					succ++;
				}
				System.out.println("Validation of " + succ + " files stored in '"+ xml.getCanonicalPath() +"' succeeded.");
			}
			// process only that file
			else {				
				XMLParser.parse(xml.getAbsolutePath(), findXSDSchema(xml.getAbsolutePath(), params.useEnvBase, log).getAbsolutePath(), params.tmpFolder, params.ignoreExecutor, false, false, true, params.disableCheckpoint, params.forceLoading, params.disableMails);
				System.out.println("Validation of '"+ xml.getCanonicalPath() +"' succeeded!");
			}
			System.exit(0);
		}*/
			
			
			//log.info("Module library generation succeeded; the library contains " + modules + " modules.");
		}
		System.exit(0);
	}
	
	
	/*public boolean validateModuleXSDFile(File xsd) {
		
	}
	
	public boolean validateModuleDocuXSDFile(File xsd) {
		parseXMLFile(DocumentBuilderFactory dbf, File xmlDocuFile, File xsdFile)
		
	}
	
	public boolean validateModuleDocuXMLFile(File xml) {
		
	}*/
}
