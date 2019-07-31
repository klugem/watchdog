package de.lmu.ifi.bio.watchdog.runner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ggf.drmaa.DrmaaException;
import org.xml.sax.SAXException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import de.lmu.ifi.bio.watchdog.docu.DocuXMLParser;
import de.lmu.ifi.bio.watchdog.docu.Moduledocu;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.resume.ResumeInfo;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;


/**
 * Runner that extracts the short description of documented modules based on a log of a watchdog run
 * @author Michael Kluge
 *
 */
public class MethodTemplateGeneratorRunner extends BasicRunner {
	
	public static final String OPEN_BRACKET = " [";
	public static final String CLOSE_BRACKET = "]";
	public static final String SEP_KOMMA = ", ";
	public static final String SEP_SEMIC = "; ";
	public static final String ENUMERATE = ") ";
	public static final String MODNAME_SEP = ": ";
	public static final String PMIDS = "PMIDS: ";
	public static final String USED = "USED: ";

	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException, DrmaaException, InterruptedException {
		Logger log = new Logger(LogLevel.INFO);
		MethodTemplateGeneratorParameters params = new MethodTemplateGeneratorParameters();
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
		// citation info mode
		else {
			// test if output file does not exist yet
			File citationInfo = new File(params.outputFile);
			if(citationInfo.exists() && citationInfo.isFile()) {
				log.error("Output file '"+citationInfo.getAbsolutePath()+"' already exists.");
				System.exit(1);
			}

			// get ID restriction parameters
			int startID = params.start;
			int stopID = params.stop;
			HashSet<Integer> includeID = new HashSet<>();
			HashSet<Integer> excludeID = new HashSet<>();
			params.include.forEach(x -> includeID.add(x));
			params.exclude.forEach(x -> excludeID.add(x));
			
			// check, if start and stop ID are ok
			if(startID > stopID) {
				log.error("Start id must be smaller or equal than stop id ('"+startID+"' vs '"+stopID+"')!");
				System.exit(1);
			}if(includeID.size() > 0 && excludeID.size() > 0) {
				log.error("Parameters '-include' and '-exclude' can not be used at the same time.");
				System.exit(1);
			}
		
			// try to read XML file 
			File xml = new File(params.xml);
			File resume = new File(params.resume);	
			if(!(xml.isFile()&& xml.canRead())) {
				log.error("Failed to read XML workflow file '" + xml.getAbsolutePath() + "'.");
				System.exit(1);
			}
			if(!(resume.isFile()&& resume.canRead())) {
				log.error("Failed to watchdog status log file '" + resume.getAbsolutePath() + "'.");
				System.exit(1);
			}
			
			// get Watchdog base
			File xsdSchema = XMLBasedWatchdogRunner.findXSDSchema(xml.getAbsolutePath(), false, log);
			String watchdogBase = xsdSchema.getParentFile().getParentFile().getAbsolutePath();
			
			// load the modules that are used within the workflow
			Functions.filterErrorStream();
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setIgnoringElementContentWhitespace(true);
			dbf.setNamespaceAware(true);
			ArrayList<String> moduleFolders = XMLParser.getModuleFolders(dbf, xml, watchdogBase);
	
			// try to find and load documentation for these modules
			HashMap<String, Moduledocu> moduleDocu = new HashMap<>();
			for(Moduledocu md : DocuXMLParser.parseAllXMLFiles(watchdogBase, moduleFolders)) {
				moduleDocu.put(md.getName(), md);
			}

			// read resume info
			LinkedHashMap<String, Pair<Integer, Integer>> usageInfo = new LinkedHashMap<>();
			HashMap<Integer, HashMap<String, ResumeInfo>> resumeInfo = XMLBasedWatchdogRunner.getResumeInfo(params.resume, log,  new AtomicInteger(0));
			
			// get info on used modules
			for(Integer id : resumeInfo.keySet()) {
				// test, if ID should be used
				if(!(startID <= id && id <= stopID))
					continue;
				if(includeID.size() > 0 && !includeID.contains(id))
					continue;
				if(excludeID.contains(id))
					continue;
				
				// get return info
				HashMap<String, ResumeInfo> ret = resumeInfo.get(id);
				boolean isFirstOfTaskID = true;
				// iterate over all sub-tasks
				for(String subKey : ret.keySet()) {
					ResumeInfo info = ret.get(subKey);
					String modName = info.getModuleName().replaceFirst(DocuXMLParser.REPLACE_SUFFIX, "");
					
					// add empty pair
					if(!usageInfo.containsKey(modName))
						usageInfo.put(modName, Pair.of(0, 0));
					
					// update counter
					Pair<Integer, Integer> update = usageInfo.get(modName);
					int diffIDs = update.getLeft();
					int subTasks = update.getRight() + 1;
					if(isFirstOfTaskID) 
						diffIDs += 1;
					
					usageInfo.put(modName, Pair.of(diffIDs, subTasks));
					isFirstOfTaskID = false;
				}
			}

			// prepare the final output
			int counter = 1;
			StringBuffer buf = new StringBuffer();
			for(String modName : usageInfo.keySet()) {
				Pair<Integer, Integer> called = usageInfo.get(modName);
				String desc = "no short description given in documentation of module '"+ modName +"'";
				ArrayList<String> cite = new ArrayList<>();
				// try to find docu and get info if there
				if(moduleDocu.containsKey(modName)) {
					Moduledocu md = moduleDocu.get(modName);
					if(md.getPaperDescription().length() > 0)
						desc = md.getPaperDescription();
					else if(params.ignore)
						continue;
					cite = md.getPMIDs();
				}
				else if(params.ignore)
					continue;
				
				// add new entry to the buffer
				if(params.enumerate) {
					buf.append(counter++);
					buf.append(ENUMERATE);
				}
				if(params.name) {
					buf.append(modName);
					buf.append(MODNAME_SEP);
				}
				buf.append(desc);
				// add meta info if wished
				if(params.pmid || params.used) {
					buf.append(OPEN_BRACKET);
					if(params.pmid && cite.size() > 0) {
						buf.append(PMIDS);
						buf.append(StringUtils.join(cite, SEP_KOMMA));
						if(params.used)
							buf.append(SEP_SEMIC);
						else 
							buf.append(CLOSE_BRACKET);
					}
					if(params.used) {
						buf.append(USED);
						// different task IDs : total (sub)tasks
						buf.append(called.getLeft() + SEP_KOMMA + called.getRight());
						buf.append(CLOSE_BRACKET);
					}
				}
				if(params.newline)
					buf.append(XMLParser.NEWLINE);
			}
			
			// ensure that parent folder exists
			File outputFolder = citationInfo.getParentFile();
			if(!outputFolder.exists()) {
				if(!outputFolder.mkdirs()) {
					log.error("Could not create output folder '"+outputFolder.getAbsolutePath()+"'.");
					System.exit(1);
				}
			}
			if(!outputFolder.canRead() || !outputFolder.canWrite()) {
				log.error("Could not read/write to output folder '"+outputFolder.getAbsolutePath()+"'.");
				System.exit(1);
			}
			// write it to file
			if(!Functions.write(citationInfo.toPath(), buf.toString())) {
				log.error("Could not write to output file '"+citationInfo.getAbsolutePath()+"'.");
				System.exit(1);
			}
			
			// all went good!
			System.exit(0);
		}
	}
}