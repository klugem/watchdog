package de.lmu.ifi.bio.watchdog.runner;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import de.lmu.ifi.bio.watchdog.docu.DocuXMLParser;
import de.lmu.ifi.bio.watchdog.docu.Moduledocu;
import de.lmu.ifi.bio.watchdog.docu.Paramdocu;
import de.lmu.ifi.bio.watchdog.docu.Returndocu;
import de.lmu.ifi.bio.watchdog.docu.VersionedInfo;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import de.lmu.ifi.watchdog.docu.extractor.Extractor;
import de.lmu.ifi.watchdog.docu.extractor.ExtractorFinder;
import de.lmu.ifi.watchdog.docu.extractor.XSDParameterExtractor;
import de.lmu.ifi.watchdog.docu.extractor.XSDReturnValueExtractor;

/**
 * Creates module documentation templates based on XSD (and other files)
 * @author Michael Kluge
 *
 */
public class ModuleDocuExtractorRunner extends BasicRunner {
	
	public static final DateFormat DF = new SimpleDateFormat("yyyy-MM-dd");
	
	public static void main(String[] args) {
		Logger log = new Logger(LogLevel.INFO);
		ModuleDocuExtractorParameters params = new ModuleDocuExtractorParameters();
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
		// extraction mode
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
			String xsdRootDir = new File(watchdogBase + File.separator + BasicRunner.XSD_PATH).getAbsoluteFile().getParent();
					
			// ensure that the tmp folder is there
			File tmpFolder = new File(watchdogBase.getAbsolutePath() + File.separator + XMLParser.TMP_FOLDER);
			if(params.tmpFolder != null && params.tmpFolder.length() >0) 
				tmpFolder = new File(params.tmpFolder);
				
			if(!tmpFolder.exists()) {
				if(!tmpFolder.mkdirs()) {
					log.error("Could not create temporary folder '"+tmpFolder.getAbsolutePath()+"'.");
					System.exit(1);
				}
			}
			if(!tmpFolder.canRead() || !tmpFolder.canWrite()) {
				log.error("Could not read/write in temporary folder '"+tmpFolder.getAbsolutePath()+"'.");
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
			
			// add some default values from parameters
			HashMap<String, List<String>> defaultValues = new HashMap<>();
			if(params.authors != null && params.authors.size() > 0)
				defaultValues.put(DocuXMLParser.AUTHOR, params.authors);
			if(params.categories != null && params.categories.size() > 0)
				defaultValues.put(DocuXMLParser.CATEGORY, params.categories);
			if(params.maintainer != null && params.maintainer.size() > 0)
				defaultValues.put(DocuXMLParser.MAINTAINER, params.maintainer);
			
			// prepare the XSD parser
			DocumentBuilderFactory dbfXML = DocuXMLParser.prepareDBF(watchdogBase.getAbsolutePath());
			DocumentBuilderFactory dbf = DocuXMLParser.prepareDBF(watchdogBase.getAbsolutePath());

			// get the extraction plugins that could be loaded
			ArrayList<Class<? extends Extractor<Paramdocu>>> paramPluginClasses = new ExtractorFinder<Paramdocu>().getImplementingExtractors(Paramdocu.class);
			ArrayList<Class<? extends Extractor<Returndocu>>> returnPluginClasses = new ExtractorFinder<Returndocu>().getImplementingExtractors(Returndocu.class);
			
			// let the user decide which plugins should be applied
			ArrayList<Extractor<Paramdocu>> paramPlugins = new ExtractorFinder<Paramdocu>().askForPlugins(paramPluginClasses, "parameter", log);
			ArrayList<Extractor<Returndocu>> returnPlugins = new ExtractorFinder<Returndocu>().askForPlugins(returnPluginClasses, "return values", log);
			
			// find all module folders, apply basic XSD extractors and all plugins
			Functions.filterErrorStream();
			int ok = 0;
			int failed = 0;
			HashMap<String, String> targets = XMLParser.findModules(dbf, moduleFolders);
			for(String name : targets.keySet()) {
				log.info("processing '"+ name +"'...");
				File x = new File(targets.get(name));
				try {
					Moduledocu m = createModuleDocuTemplate(x, tmpFolder.getAbsolutePath(), xsdRootDir, dbf, defaultValues);
					
					// apply param plugins
					for(Extractor<Paramdocu> ep : paramPlugins) {
						File target = ep.findTarget(dbf, x.getAbsoluteFile().getParentFile(), x);
						if(target != null) {
							if(ep.canBeApplied(target)) {
								log.info("processing '"+ target.getAbsolutePath() +"' with " + ep.getClass().getSimpleName());
								LinkedHashMap<String, ArrayList<Paramdocu>> extracted = ep.getDocu(target);
								ep.updateDocu(m, extracted);
							}
						}
					}
					// apply return value plugins
					for(Extractor<Returndocu> ep : returnPlugins) {
						File target = ep.findTarget(dbf, x.getAbsoluteFile().getParentFile(), x);
						if(target != null) {
							if(ep.canBeApplied(target)) {
								log.info("processing '"+ target.getAbsolutePath() +"' with " + ep.getClass().getSimpleName());
								LinkedHashMap<String, ArrayList<Returndocu>> extracted = ep.getDocu(target);
								ep.updateDocu(m, extracted);
							}
						}
					}
					
					// write the docu template to disk!
					File docu = new File(x.getAbsolutePath().replaceFirst("\\.xsd$", ".docu.xml"));
					if(!docu.exists() || params.overwrite) {
						XMLParser.writePrettyXML(XMLParser.getRootElement(dbfXML, m.toXML(true)), docu);
					}
					else {
						log.warn("Skipped writing of '"+ docu.getAbsolutePath() +  "' as file already exists.");
					}
					ok++;
				}
				catch(Exception e) {
					log.error("Failed to parse info from file '"+x.getAbsolutePath()+"'");
					e.printStackTrace();
					failed++;
				}
			}
			log.info("Template extraction succeeded for " + ok + " modules.");
			if(failed == 0) {
				log.info("Execution finished without errors.");
				System.exit(0);
			}
			else {
				log.error("Template extraction failed for " + failed + " modules.");
				System.exit(1);
			}
		}
		System.exit(0);
	}
	
	/**
	 * gets the base template from the XSD file
	 * @param xsd
	 * @param tmpBaseDir
	 * @param xsdRootDir
	 * @return
	 * @throws Exception
	 */
	public static Moduledocu createModuleDocuTemplate(File xsd, String tmpBaseDir, String xsdRootDir, DocumentBuilderFactory dbf, HashMap<String, List<String>> defaultValues) throws Exception {
		// fill variables with dummy or real values obtained from the XSD file
		String name = XMLParser.getTaskTypeOfModule(dbf, xsd);
		String updated  = DF.format(new Date());
		String paperDesc = "PAPER_SENTENCE";
		
		ArrayList<String> pmid = new ArrayList<>();
		ArrayList<String> website = new ArrayList<>();
		ArrayList<VersionedInfo<String>> description = new ArrayList<>();
		ArrayList<String> authors = new ArrayList<>();
		ArrayList<String> categories = new ArrayList<>();
		ArrayList<VersionedInfo<String>> dependencies = new ArrayList<>();
		ArrayList<VersionedInfo<String>> comments = new ArrayList<>();
		ArrayList<String> maintainer = new ArrayList<>();
		
		// test, if some default values are avail
		if(defaultValues.containsKey(DocuXMLParser.AUTHOR))
			authors.addAll(defaultValues.get(DocuXMLParser.AUTHOR));
		else
			authors.add("AUTHOR [1-]");
		if(defaultValues.containsKey(DocuXMLParser.CATEGORY))
			categories.addAll(defaultValues.get(DocuXMLParser.CATEGORY));
		else
			categories.add("CATEGORY [1-]");
		if(defaultValues.containsKey(DocuXMLParser.MAINTAINER))
			maintainer.addAll(defaultValues.get(DocuXMLParser.MAINTAINER));
		else
			maintainer.add("GITHUB_USERNAME [0-]");
		
		pmid.add("-1");
		website.add("WEBSITE [1-]");
		dependencies.add(new VersionedInfo<String>("DEPENDENCY [0-]", 1, 1));
		comments.add(new VersionedInfo<String>("COMMENT [0-]", 1, 1));
		
		// get parameter and return values from basic XSD extractor plugin
		HashMap<String, ArrayList<Paramdocu>> params = new XSDParameterExtractor(tmpBaseDir, xsdRootDir).getDocu(xsd);
		HashMap<String, ArrayList<Returndocu>> returnValues = new XSDReturnValueExtractor().getDocu(xsd);
		HashSet<Integer> versions = XMLParser.getVersionsOfModule(dbf, xsd);
		
		description.add(new VersionedInfo<String>("DESCRIPTION [1x per version]", versions.stream().min(Integer::compare).orElse(1), versions.stream().max(Integer::compare).orElse(1)));
		
		// create the object
		Moduledocu m = new Moduledocu(name, categories, updated, authors, pmid, website, paperDesc, dependencies, comments, description, versions, params, returnValues, maintainer);
		return m;
	}
}
