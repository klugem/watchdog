package de.lmu.ifi.bio.watchdog.docu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.logger.Logger;

/**
 * Class that generates the module library website based on the module docu XML files
 * @author Michael Kluge
 *
 */
public class ModuleLibraryGenerator { 

	/////////////////// CONFIG VARIABLES ///////////////////
	public static int MAX_FILTER_BUTTON_NUMBER = 5; 
	public static String FILTER_VALUE_SEP = "§";
	public static String META_NAME = "meta_info"; // name of var in JS file
	////////////////////////////////////////////////////////

	public static String NEWLINE = System.lineSeparator();
	
	///////////////// INDEX REPLACE NAMES ///////////////////
	public static String FILTER_REPLACE_TEMPLATE = "{@FILTER_REPLACE_TEMPLATE@}";
	public static String MODULE_REPLACE_TEMPLATE= "{@MODULE_REPLACE_TEMPLATE@}";
	////////////////////////////////////////////////////////
	
	// meta fields to filter to replace
	public static final String FILTER_BUTTON_TEMPLATE_CATEGORY = "<!--FILTER_BUTTON_TEMPLATE_CATEGORY-->";
	public static final String FILTER_BUTTON_TEMPLATE_AUTHOR = "<!--FILTER_BUTTON_TEMPLATE_AUTHOR-->";
	public static final String FILTER_BUTTON_TEMPLATE_AGE = "<!--FILTER_BUTTON_TEMPLATE_AGE-->";
	public static final String DETAIL_PARAM_TEMPLATE = "<!--DETAIL_PARAM_TEMPLATE-->";
	public static final String DETAIL_DEPENDENCY_TEMPLATE = "<!--DETAIL_DEPENDENCY_TEMPLATE-->";
	public static final String DETAIL_CITE_TEMPLATE = "<!--DETAIL_CITE_TEMPLATE-->";
	public static final String DETAIL_RETURN_TEMPLATE = "<!--DETAIL_RETURN_TEMPLATE-->";
	public static final String VERSION_LINK_TEMPLATE = "<!--VERSION_LINK_TEMPLATE-->";
			
	// fields to replace
	public static String VALUE = "{@VALUE@}";
	public static String VALUE_DISPLAY = "{@VALUE_DISPLAY@}";
	public static String MIN_VER = "{@MIN_VER@}";
	public static String MAX_VER = "{@MAX_VER@}";
	////////////////////////////////////////////////////////
	
	//////////////////////   MODULE   //////////////////////
	// fields to replace
	public static String CATEGORY =  "{@CATEGORY@}";
	public static String AGE =  "{@AGE@}";
	public static String MOD_NAME = "{@MOD_NAME@}";
	public static String DESC = "{@DESC@}";
	public static String GITHUB = "{@GITHUB@}";
	public static String URL = "{@URL@}";
	public static String PMID = "{@PMID@}";
	public static String AUTHOR = "{@AUTHOR@}";
	public static String VERSION = "{@VERSION@}";
	public static String CAT_ID = "{@CAT_ID@}";
	
	// meta fields to filter to replace
	public static String META_AUTHOR = "{@META_AUTHOR@}";
	public static String META_CATEGORY = "{@META_CATEGORY@}";
	public static String META_AGE = "{@META_AGE@}";
	public static String META_ID = "{@META_ID@}";
	public static String LINK_COUNTER = "{@LINK_COUNTER@}";
	
	// remove comment tags
	public static String REMOVE_LINK_PREFIX = "\\s*<a.+";
	public static String REMOVE_MOD_GIT = REMOVE_LINK_PREFIX + "<!--GIT_LINK-->";
	public static String REMOVE_MOD_URL = REMOVE_LINK_PREFIX + "<!--URL_LINK-->";
	public static String REMOVE_MOD_PMID = REMOVE_LINK_PREFIX + "<!--PMID_LINK-->";
	
	// full detail fields to replace
	public static String DEPENDENCIES = "{@DEPENDENCIES@}";
	public static String PARAMETER = "{@PARAMETER@}";
	public static String RETURN_VALUES = "{@RETURN_VALUES@}";
	public static String VERSION_LINKS = "{@VERSION_LINKS@}";
	public static String VERSION_LINKS_HIDE = "{@VERSION_LINKS_HIDE@}";
	public static String DEPENDENCIES_HIDE = "{@DEPENDENCIES_HIDE@}";
	public static String PARAMETER_HIDE = "{@PARAMETER_HIDE@}";
	public static String RETURN_VALUES_HIDE = "{@RETURN_VALUES_HIDE@}";
	public static String CITATION_HIDE = "{@CITATION_HIDE@}";
	
	public static String PARAM_NAME = "{@PARAM_NAME@}";
	public static String PARAM_OCCURENCE = "{@PARAM_OCCURENCE@}";
	public static String PARAM_TYPE = "{@PARAM_TYPE@}";
	public static String PARAM_RESTRICTION = "{@PARAM_RESTRICTION@}";
	public static String PARAM_DEFAULT = "{@PARAM_DEFAULT@}";
	public static String PARAM_DESCRIPTION = "{@PARAM_DESCRIPTION@}";
	public static String SINGLE_DEPENDENCY = "{@SINGLE_DEPENDENCY@}";
	public static String PAPER_DESC = "{@PAPER_DESC@}";
	public static String SINGLE_PMID = "{@SINGLE_PMID@}";
	public static String PMID_LIST = "{@PMID_LIST@}";
	public static String VERSION_ID = "{@VERSION_ID@}";
	
	public static String RETURN_NAME = "{@RETURN_NAME@}";
	public static String RETURN_TYPE = "{@RETURN_TYPE@}";
	public static String RETURN_DESCRIPTION = "{@RETURN_DESCRIPTION@}";
	////////////////////////////////////////////////////////
	
	public static String TEMPLATE_PATH = "documentation" + File.separator + "module_library_tpl";
	public static String TEMPLATE_INDEX = TEMPLATE_PATH + File.separator + "index.htm.tpl";
	public static String TEMPLATE_FILTER = TEMPLATE_PATH + File.separator + "filter.htm.tpl";
	public static String TEMPLATE_MODULE = TEMPLATE_PATH + File.separator + "module.htm.tpl";
	public static String CSS_FOLDER_NAME = "css";
	public static String JS_FOLDER_NAME = "js";
	public static String CSS_FOLDER_PATH = TEMPLATE_PATH + File.separator + CSS_FOLDER_NAME;
	public static String JS_FOLDER_PATH = TEMPLATE_PATH + File.separator + JS_FOLDER_NAME;
	
	public static HashMap<String, HashMap<String, Integer>> FILTER_GROUPS = new HashMap<>();
	
	static {
		FILTER_GROUPS.put(FILTER_BUTTON_TEMPLATE_CATEGORY, new HashMap<String, Integer>());
		FILTER_GROUPS.put(FILTER_BUTTON_TEMPLATE_AUTHOR, new HashMap<String, Integer>());
		
		HashMap<String, Integer> categoryAgeMap = new HashMap<>();
		categoryAgeMap.put("3 days".toUpperCase(), 3);
		categoryAgeMap.put("1 week".toUpperCase(), 2);
		categoryAgeMap.put("3 weeks".toUpperCase(), 1);
		FILTER_GROUPS.put(FILTER_BUTTON_TEMPLATE_AGE, categoryAgeMap);
	}
		
	public static int generateModuleLibraryPage(File outputDir, File watchdogBasedir, ArrayList<String> moduleFoldersToInclude) {
		Logger log = new Logger();
		int ok = 0;
		if(!outputDir.exists())
			outputDir.mkdirs();
		else {
			log.error("Output directory '"+outputDir.getAbsolutePath()+"' already exists!");
			System.exit(1);
		}
		if(!outputDir.exists()) {
			log.error("Failed to generate output directory '"+outputDir.getAbsolutePath()+"'");
			System.exit(1);
		}
		if(!outputDir.canWrite()) {
			log.error("Output directory '"+outputDir.getAbsolutePath()+"' is not writable.");
			System.exit(1);
		}
		// get the modules to use
		ArrayList<Moduledocu> documentedModules = DocuXMLParser.parseAllXMLFiles(watchdogBasedir.getAbsolutePath(), moduleFoldersToInclude);
		documentedModules = documentedModules.stream().sorted((a,b) -> a.getName().compareTo(b.getName())).collect(Collectors.toCollection(ArrayList::new));
		
		String baseOut = outputDir.getAbsolutePath() + File.separator;
		File cssDestFolder = new File(baseOut + CSS_FOLDER_NAME);
		File jsDestFolder = new File(baseOut + JS_FOLDER_NAME);
		String indexOut = baseOut + "index.htm";
		String jsIndexOut = jsDestFolder.getAbsolutePath() + File.separator + "meta.json";
		
		try {
			// read the template files
			String base = watchdogBasedir.getAbsolutePath() + File.separator;
			File cssFolder = new File(base + CSS_FOLDER_PATH);
			File jsFolder = new File(base + JS_FOLDER_PATH);
			
			List<String> index = Files.readAllLines(Paths.get(base + TEMPLATE_INDEX));
			List<String> filter = Files.readAllLines(Paths.get(base + TEMPLATE_FILTER));
			List<String> module = Files.readAllLines(Paths.get(base + TEMPLATE_MODULE));
			
			/***********************************************************************************/
			// update module list
			String sep = FILTER_VALUE_SEP + ';' + FILTER_VALUE_SEP;
			HashMap<String, Integer> authorCountMap = FILTER_GROUPS.get(FILTER_BUTTON_TEMPLATE_AUTHOR);
			HashMap<String, Integer> categoryCountMap = FILTER_GROUPS.get(FILTER_BUTTON_TEMPLATE_CATEGORY);
			String moduleTemplate = StringUtils.join(module, NEWLINE);
			String paramTemplate = module.stream().filter(l -> l.contains(DETAIL_PARAM_TEMPLATE)).findFirst().orElse(null);
			String dependencyTemplate = module.stream().filter(l -> l.contains(DETAIL_DEPENDENCY_TEMPLATE)).findFirst().orElse(null);
			String pmidTemplate = module.stream().filter(l -> l.contains(DETAIL_CITE_TEMPLATE)).findFirst().orElse(null);
			String returnTemplate = module.stream().filter(l -> l.contains(DETAIL_RETURN_TEMPLATE)).findFirst().orElse(null);
			String versionLinkTemplate = module.stream().filter(l -> l.contains(VERSION_LINK_TEMPLATE)).findFirst().orElse(null);
			moduleTemplate = moduleTemplate.replace(paramTemplate, "");
			moduleTemplate = moduleTemplate.replace(dependencyTemplate, "");
			moduleTemplate = moduleTemplate.replace(pmidTemplate, "");
			moduleTemplate = moduleTemplate.replace(returnTemplate, "");
			moduleTemplate = moduleTemplate.replace(versionLinkTemplate, "");
			
			paramTemplate = paramTemplate.replace(DETAIL_PARAM_TEMPLATE, "");
			dependencyTemplate = dependencyTemplate.replace(DETAIL_DEPENDENCY_TEMPLATE, "");
			pmidTemplate = pmidTemplate.replace(DETAIL_CITE_TEMPLATE, "");
			returnTemplate = returnTemplate.replace(DETAIL_RETURN_TEMPLATE, "");
			versionLinkTemplate = versionLinkTemplate.replace(VERSION_LINK_TEMPLATE, "");
			
			LinkedHashMap<Moduledocu, String> allModules = new LinkedHashMap<>();
			ArrayList<String> search = new ArrayList<>();
			int i = 0;
			for(Moduledocu md : documentedModules) {
				log.info("Processing '" +md.getName()+ "'...");
				// get new template
				String newEntry = moduleTemplate;
				
				// replace meta filter values
				ArrayList<String> authors = md.getAuthorNames().stream().map(String::toUpperCase).collect(Collectors.toCollection(ArrayList::new));
				ArrayList<String> cats = md.getCategories().stream().map(String::toUpperCase).collect(Collectors.toCollection(ArrayList::new));
				newEntry = newEntry.replace(META_AUTHOR, FILTER_VALUE_SEP + StringUtils.join(authors, sep) + FILTER_VALUE_SEP);
				newEntry = newEntry.replace(META_CATEGORY, FILTER_VALUE_SEP + StringUtils.join(cats, sep) + FILTER_VALUE_SEP);
				newEntry = newEntry.replace(META_AGE, md.getDate());

				// replace values
				newEntry = newEntry.replace(CATEGORY, md.getCategories().get(0));
				newEntry = newEntry.replace(AGE, md.getDate());
				newEntry = newEntry.replace(MOD_NAME, md.getName());
				newEntry = newEntry.replace(DESC, md.getDescription().stream().max((s1, s2) -> Integer.compare(s1.MAX_VER, s2.MAX_VER)).orElse(new VersionedInfo<String>("missing", 1, 1)).VALUE);
				newEntry = newEntry.replace(AUTHOR, md.getAuthorNames().get(0));
				newEntry = newEntry.replace(VERSION, Integer.toString(md.getMaxVersion()));
								
				// replace links
				if(md.hasGithubURL())
					newEntry = newEntry.replace(GITHUB, md.getGithub());
				else
					newEntry = newEntry.replaceAll(REMOVE_MOD_GIT, "");
				if(md.getWebsite().size() > 0)
					newEntry = newEntry.replace(URL, md.getWebsite().get(0));
				else
					newEntry = newEntry.replaceAll(REMOVE_MOD_URL, "");
				
				if(md.getPMIDs().size() > 0)
					newEntry = newEntry.replace(PMID, md.getPMIDs().get(0));
				else
					newEntry = newEntry.replaceAll(REMOVE_MOD_PMID, "");
				
				// for full detail page only
				///////////////////////////// DEPENDENCIES
				if(md.getDependencies().size() > 0) {
					StringBuilder depb = new StringBuilder();
					for(VersionedInfo<String> dep : md.getDependencies()) {
						String t = dependencyTemplate;
						t = t.replace(SINGLE_DEPENDENCY, dep.VALUE);
						t = t.replace(MIN_VER, Integer.toString(dep.MIN_VER));
						t = t.replace(MAX_VER, Integer.toString(dep.MAX_VER));
						depb.append(t);
					}
					newEntry = newEntry.replace(DEPENDENCIES, depb);
					newEntry = newEntry.replace(DEPENDENCIES_HIDE, Boolean.toString(false));
				}
				else {
					newEntry = newEntry.replace(DEPENDENCIES_HIDE, Boolean.toString(true));
				}
				/////////////////////////////
				///////////////////////////// PARAMETER
				if(md.getParameter().size() > 0) {
					StringBuilder paramb = new StringBuilder();
					for(Paramdocu pd : md.getParameter()) {
						String t = paramTemplate;
						t = t.replace(PARAM_NAME, pd.getName());
						t = t.replace(PARAM_OCCURENCE, pd.getOccurenceInfo());
						t = t.replace(PARAM_TYPE, pd.getType());
						t = t.replace(PARAM_RESTRICTION, pd.getRestriction());
						t = t.replace(PARAM_DEFAULT, pd.getDefault());
						t = t.replace(PARAM_DESCRIPTION, pd.getDescription());
						t = t.replace(MIN_VER, Integer.toString(pd.getMinVersion()));
						t = t.replace(MAX_VER, Integer.toString(pd.getMaxVersion()));
						paramb.append(t);
					}
					// replace it and remove template line
					newEntry = newEntry.replace(PARAMETER, paramb);
					newEntry = newEntry.replace(PARAMETER_HIDE, Boolean.toString(false));
				}
				else {
					newEntry = newEntry.replace(PARAMETER_HIDE, Boolean.toString(true));
				}
				/////////////////////////////
				///////////////////////////// RETURN VALUES
				if(md.getReturnValues().size() > 0) {
					StringBuilder returnb = new StringBuilder();
					for(Returndocu rd : md.getReturnValues()) {
						String t = returnTemplate;
						t = t.replace(RETURN_NAME, rd.getName());
						t = t.replace(RETURN_TYPE, rd.getType());
						t = t.replace(RETURN_DESCRIPTION, rd.getDescription());
						t = t.replace(MIN_VER, Integer.toString(rd.getMinVersion()));
						t = t.replace(MAX_VER, Integer.toString(rd.getMaxVersion()));
						returnb.append(t);
					}
					newEntry = newEntry.replace(RETURN_VALUES, returnb);
					newEntry = newEntry.replace(RETURN_VALUES_HIDE, Boolean.toString(false));
				}
				else {
					newEntry = newEntry.replace(RETURN_VALUES_HIDE, Boolean.toString(true));
				}
				////////////////////////////ok/
				///////////////////////////// CITATION INFO
				if(!(md.getPMIDs().size() == 0 && (md.getPaperDescription() == null || md.getPaperDescription().length() == 0))) {
					StringBuilder citeb = new StringBuilder();
					for(String p : md.getPMIDs()) {
						String t = pmidTemplate;
						t = t.replace(SINGLE_PMID, p);
						
						citeb.append(t);
					}
					newEntry = newEntry.replace(PMID_LIST, citeb);
					newEntry = newEntry.replace(PAPER_DESC, md.getPaperDescription());
					newEntry = newEntry.replace(CITATION_HIDE, Boolean.toString(false));
				}
				else {
					newEntry = newEntry.replace(CITATION_HIDE, Boolean.toString(true));
				}
				/////////////////////////////
				///////////////////////////// VERSION LINKS
				if(md.getVersions().size() > 1) {
					StringBuilder versions = new StringBuilder();
					for(int v : md.getVersions()) {
						String t = versionLinkTemplate;
						t = t.replace(VERSION_ID, Integer.toString(v));
						versions.append(t);
					}
					newEntry = newEntry.replace(VERSION_LINKS, versions);
					newEntry = newEntry.replace(VERSION_LINKS_HIDE, Boolean.toString(false));
				}
				else {
					newEntry = newEntry.replace(VERSION_LINKS_HIDE, Boolean.toString(true));
				}

				/////////////////////////////
				newEntry = newEntry.replace(META_ID, Integer.toString(md.getAutoID()));
				
				// add the finished module to the list
				allModules.put(md, newEntry);
				
				// update categories 
				md.getAuthorNames().stream().forEachOrdered(n -> updateHashMapCounter(authorCountMap, n.toUpperCase()));
				md.getCategories().stream().forEachOrdered(n -> updateHashMapCounter(categoryCountMap, n.toUpperCase()));
				
				// collect JS json search info
				search.add(md.getJsonInfo());
				i++;
			}
			/***********************************************************************************/
			
			/***********************************************************************************/
			// update filter list
			String filterAll = StringUtils.join(filter, NEWLINE);
			for(String type : FILTER_GROUPS.keySet()) {
				// get key map and sort it
				HashMap<String, Integer> buttonKeys = FILTER_GROUPS.get(type);
				ArrayList<String> sortedCountMap = buttonKeys.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).map(Map.Entry::getKey).collect(Collectors.toCollection(ArrayList::new));
				// replace the values
				String template = filter.stream().filter(l -> l.contains(type)).findFirst().orElse(null);
				if(template != null) {
					ArrayList<String> add = new ArrayList<>();
					for(i = 0; i < MAX_FILTER_BUTTON_NUMBER && sortedCountMap.size() > i; i++) {
						String catName = sortedCountMap.get(i);
						String newEntry = template.replace(VALUE, FILTER_VALUE_SEP + catName + FILTER_VALUE_SEP);
						newEntry = newEntry.replace(VALUE_DISPLAY, catName + " ("+ buttonKeys.get(catName)  +")");
						newEntry = newEntry.replace(LINK_COUNTER, Integer.toString(i+1));
						add.add(newEntry); // :-)
						
						// replace the cat-id for the top-ranked-modules
						for(Map.Entry<Moduledocu, String> e : allModules.entrySet()) {
							Moduledocu m = e.getKey();
							// replace it
							String catNameHigh = catName.toUpperCase();
							if(m.getCategories().stream().filter(x -> x.toUpperCase().equals(catNameHigh)).count() > 0) {
								String v = e.getValue().replace(CAT_ID, Integer.toString(i+1));
								allModules.put(m, v);
							}
						}
					}
					
					// replace it in template
					filterAll = filterAll.replaceFirst(".*" +type+ ".*", StringUtils.join(add, NEWLINE));
				}
				else {
					System.out.println("Template line for filter '"+type+"' not found.");
					System.exit(1);
				}
			}
			/***********************************************************************************/
			
			/***********************************************************************************/
			// replace all in index
			StringBuilder finalIndex = new StringBuilder();
			for(String l : index) {
				l = l.replace(FILTER_REPLACE_TEMPLATE, filterAll);
				l = l.replace(MODULE_REPLACE_TEMPLATE, StringUtils.join(allModules.values(), NEWLINE));
				
				finalIndex.append(l);
				finalIndex.append(NEWLINE);
			}
			
			// write the index file
			Functions.write(Paths.get(indexOut), finalIndex.toString());
			// copy the JS/CSS folder
			FileUtils.copyDirectory(cssFolder, cssDestFolder);
			FileUtils.copyDirectory(jsFolder, jsDestFolder);

			// write the JSON meta-data file
			Functions.write(Paths.get(jsIndexOut), "var " + META_NAME + "='[" + StringUtils.join(search, ", ")+ "]'");
			ok = allModules.size();
			/***********************************************************************************/
			
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		// all went good
		return ok;
	}
	
	public static void updateHashMapCounter(HashMap<String, Integer> map, String key) {
		if(!map.containsKey(key))
			map.put(key, 0);
		
		// update the map
		map.put(key, map.get(key)+1);
	}
}
