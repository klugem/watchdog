package de.lmu.ifi.bio.watchdog.xmlParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.executor.local.LocalExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.ActionType;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.helper.GUIInfo;
import de.lmu.ifi.bio.watchdog.helper.Parameter;
import de.lmu.ifi.bio.watchdog.helper.PatternFilenameFilter;
import de.lmu.ifi.bio.watchdog.helper.ReplaceSpecialConstructs;
import de.lmu.ifi.bio.watchdog.helper.returnType.BooleanReturnType;
import de.lmu.ifi.bio.watchdog.helper.returnType.DoubleReturnType;
import de.lmu.ifi.bio.watchdog.helper.returnType.FileReturnType;
import de.lmu.ifi.bio.watchdog.helper.returnType.IntegerReturnType;
import de.lmu.ifi.bio.watchdog.helper.returnType.ReturnType;
import de.lmu.ifi.bio.watchdog.helper.returnType.StringReturnType;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.optionFormat.OptionFormat;
import de.lmu.ifi.bio.watchdog.optionFormat.ParamFormat;
import de.lmu.ifi.bio.watchdog.optionFormat.QuoteFormat;
import de.lmu.ifi.bio.watchdog.optionFormat.SpacingFormat;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessBlock;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessMultiParam;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessReturnValueAdder;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.task.TaskAction;
import de.lmu.ifi.bio.watchdog.task.TaskActionTime;
import de.lmu.ifi.bio.watchdog.task.actions.CopyTaskAction;
import de.lmu.ifi.bio.watchdog.task.actions.CreateTaskAction;
import de.lmu.ifi.bio.watchdog.task.actions.DeleteTaskAction;
import de.lmu.ifi.bio.watchdog.xmlParser.plugins.XMLPluginTypeLoaderAndProcessor;
import de.lmu.ifi.bio.watchdog.xmlParser.plugins.executorParser.XMLExecutorProcessor;
import de.lmu.ifi.bio.watchdog.xmlParser.plugins.processBlockParser.XMLProcessBlockProcessor;

/**
 * Parses an XML file which follows the XSD watchdog schema and creates XMLTask objects based on that information.
 * @author Michael Kluge
 *
 */
public class XMLParser {
	public static final String XML_1_1 = "http://www.w3.org/XML/XMLSchema/v1.1";
	public static final String HOST_SEP = ";";
	public static final Logger LOGGER = new Logger();
	private static final String BASE_FOLDER_CONST_PREFIX = "BASE_FOLDER_";
	private static final String IS_TEMPLATE_STRING = "isTemplate"; 
	public static final String IS_NOT_VALID_XSD_STRING = "isNotValidToXSD"; 
	private static final Pattern IS_TEMPLATE = Pattern.compile("<"+XMLParser.ROOT+".+\\W+"+IS_TEMPLATE_STRING+"=\"(true|1)\".*>"); 
	private static final Pattern IS_NOT_VALID = Pattern.compile("<"+XMLParser.ROOT+".+\\W+"+IS_NOT_VALID_XSD_STRING+"=\"(true|1)\".*>"); 
	
	private static final String REPLACE_CHARS = "^[\\[{\\(\\$].*";
	public static final String NEWLINE = System.lineSeparator();
	public static final String FILE_CHECK = "xsd" + File.separator + "watchdog.xsd";
	public static final String DEFAULT_LOCAL_NAME = "default local executor";
	public static final String DEFAULT_LOCAL_COPY_ENV = "default local copy environment";
	public static final String TMP_FOLDER = "tmp";
	public static final String SPACER = " ";
	public static final String EQUAL = "=";
	public static final String QUOTE = "\"";
	public static final String OPEN_TAG = "<";
	public static final String CLOSE_TAG = ">";
	public static final String OPEN_CLOSE_TAG = "</";
	public static final String CLOSE_NO_CHILD_TAG = "/>";
	private static final String DISABLE_FLAG = "no";
	public static String SUFFIX_SEP = "'+'*'";
	
	/* Names of the elements */
	public static final String ROOT = "watchdog";
	public static final String PROCESS_SEQUENCE = "processSequence";
	public static final String PROCESS_INPUT = "processInput";
	public static final String PROCESS_TABLE = "processTable";
	public static final String BASE_FOLDER = "baseFolder";
	public static final String EXECUTORS = "executors";
	public static final String LOCAL = "local";
	public static final String REMOTE = "remote";
	public static final String CLUSTER = "cluster";
	public static final String CPU = "cpu";
	public static final String TIMELIMIT = "timelimit";
	public static final String PROCESS_BLOCK = "processBlock";
	public static final String PROCESS_FOLDER = "processFolder";
	public static final String DISABLE_EXISTANCE_CHECK = "disableExistenceCheck";
	public static final String PARAMETER = "parameter";
	public static final String STREAMS = "streams";
	public static final String ENVIRONMENT = "environment";
	public static final String CHECKERS = "checkers";
	public static final String SETTINGS = "settings";
	
	public static final String CHECKER = "checker";
	public static final String ERROR = "error";
	public static final String CONSTANTS = "constants";
	public static final String CONST = "const";
	public static final String DEPENDENCIES = "dependencies";
	public static final String DEPENDS = "depends";
	public static final String STD_OUT = "stdout";
	public static final String STD_ERR = "stderr";
	public static final String STD_IN = "stdin";
	public static final String ENVIRONMENTS = "environments";
	public static final String VAR = "var";
	public static final String ACTIONS = "actions";
	public static final String CREATE_FILE = "createFile";
	public static final String COPY_FILE = "copyFile";
	public static final String DELETE_FILE = "deleteFile";
	public static final String CREATE_FOLDER = "createFolder";
	public static final String COPY_FOLDER = "copyFolder";
	public static final String DELETE_FOLDER = "deleteFolder";
		
	/* attributes */
	public static final String PROJECT_NAME = "projectName";
	public static final String NAME = "name";
	public static final String FOLDER = "folder";
	public static final String TABLE = "table";
	public static final String PATTERN = "pattern";
	public static final String IGNORE = "ignore";
	public static final String TASKS = "tasks";
	public static final String ID = "id";
	public static final String BIN_NAME = "binName";
	public static final String PRE_BIN_COMMAND = "preBinCommand";
	public static final String SEPARATE = "separate";
	public static final String PREFIX_NAME = "prefixName";
	public static final String IS_WATCHDOG_MODULE = "isWatchdogModule";
	public static final String WATCHDOG_BASE = "watchdogBase";
	public static final String PARAM_FORMAT = "paramFormat";
	public static final String SPACING_FORMAT = "spacingFormat";
	public static final String SEPARATE_FORMAT = "separateFormat";
	public static final String QUOTE_FORMAT = "quoteFormat";
	public static final String MAX_RUNNING = "maxRunning";
	public static final String MAX_SLAVE_RUNNING = "maxSlaveRunning";
	public static final String HOST = "host";
	public static final String PORT = "port";
	public static final String USER = "user";
	public static final String PRIVATE_KEY = "privateKey";
	public static final String DISABLE_STRICT_HOST_CHECK = "disableStrictHostCheck";
	public static final String SLOTS = "slots";
	public static final String MEMORY = "memory";
	public static final String QUEUE = "queue";
	public static final String DEFAULT = "default";
	public static final String WORKING_DIR = "workingDir";
	public static final String APPEND = "append";
	public static final String EXECUTOR = "executor";
	public static final String START = "start";
	public static final String END = "end";
	public static final String STEP = "step";
	public static final String MAIL = "mail";
	public static final String NOTIFY = "notify";
	public static final String CHECKPOINT = "checkpoint";
	public static final String CONFIRM_PARAM = "confirmParam";
	public static final String UPDATE = "update";
	public static final String USE_EXTERNAL_EXPORT = "useExternalExport";
	public static final String EXPORT_COMMAND = "exportCommand";
	public static final String SHEBANG = "shebang";
	public static final String SEP = "sep";
	public static final String COPY_LOCAL_VALUE = "copyLocalValue";
	public static final String MAX_DEPTH = "maxDepth";
	public static final String COMPARE_NAME = "compareName";
	public static final String CLASS_PATH = "classPath";
	public static final String CLASS_NAME = "className";
	public static final String C_ARG = "cArg";
	public static final String STICK2HOST = "stickToHost";
	public static final String SAVE_RESOURCE_USAGE = "saveResourceUsage";
	public static final String FILE = "file";
	public static final String DESTINATION = "destination";
	public static final String CREATE_PARENT = "createParent";
	public static final String DELETE_SOURCE = "deleteSource";
	public static final String OVERRIDE = "override";
	public static final String TIME = "time";
	public static final String PATH2JAVA = "pathToJava";
	public static final String DEFAULT_FOLDER = "defaultFolder";
	public static final String MODULES = "modules";
	public static final String WORKING_DIR_EXC = "workingDir";
	public static final String UNCOUPLE_FROM_EXECUTOR = "uncoupleFromExecutor";
	public static final String DISABLE_DEFAULT = "disableDefault"; 
	public static final String CUSTOM_PARAMETERS = "customParameters";
	
	/* values for the return param */
	private static final String X_EXTENSION = "x:extension";
	private static final String X_ELEMENT = "x:element";
	private static final String X_COMPLEX_TYPE = "x:complexType";
	private static final String SUBSTITUTION_GROUP = "substitutionGroup";
	private static final String ABSTRACT_TASK = "abstractTask";
	private static final String X_RESTRICTION = "x:restriction";
	private static final String X_ATTRIBUTE = "x:attribute";
	private static final String X_COMPLEX = "x:complexType";
	private static final String X_SIMPLE = "x:simpleType";
	private static final String X_ALL = "x:all";
	private static final String BASE = "base";
	private static final String MIN_OCCURS = "minOccurs";
	private static final String MAX_OCCURS = "maxOccurs";
	public static final String TYPE = "type";
	private static final String ABSTRACT_TASK_PATH = "xsd/abstract_task.xsd";
	private static final String EXTENSION_RETURN = "taskReturnType";
	private static final String X = "x:";
	private static final String STRING = "string";
	private static final String INTEGER = "integer";
	private static final String DOUBLE = "double";
	private static final String BOOLEAN = "boolean";
	private static final String BASE_ATTRIBUTE_TASK_TYPE = "baseAttributeTaskType";
	private static final String RETURN_FILE_PARAMETER = "returnFilePathParameter";
	private static final String FIXED = "fixed";
	private static final String PARAM = "param";
		
	/* names of types in XSD */
	private static final String FLAG_TYPE = "paramBoolean";
	private static final String DEPENDS_TYPE = "dependsType";
	
	/* GUI info stuff */
	public static final String POSX = "posX";
	public static final String POSY = "posY";
	public static final String COLOR = "color";
	
	/* pattern */
	private static final String MATCH_RANDOM = ".*?";
	public static final String REPLACE_CONST = "\\$\\{([A-Za-z_][A-Za-z_0-9]*)\\}";
	private static final String SEARCH_CONST = MATCH_RANDOM + REPLACE_CONST + MATCH_RANDOM;
	public static final Pattern PATTERN_CONST = Pattern.compile(SEARCH_CONST);
	private static final String REPLACE = "@";
	private static final String REPLACE_IMPORT_PATH = "<x:include schemaLocation=\"";
	private static final String INCLUDE_XSD = REPLACE_IMPORT_PATH + REPLACE+"\" />";
	private static final String XSD_PATTERN = "*.xsd";
	private static final String REPLACE_MODULE_IMPORT = "<!-- @!JAVA_REPLACE_MODULE_INCLUDE!@ -->";
	private static final String REPLACE_PLUGIN_IMPORT = "<!-- @!JAVA_REPLACE_PLUGIN_INCLUDE!@ -->";
	private static final HashMap<String, String> BLOCKED_CONST_NAMES = new HashMap<>();
	private static final ArrayList<String> BASE_ROOT_TYPES = new ArrayList<>();
	public static final HashMap<String, Object[]> XML_MODULE_INFO = new HashMap<>();
	private static HashMap<String, String> last_consts = new HashMap<>();
	private static boolean isGUILoadAttempt;
	private static boolean noExitInCaseOfError = false;
	
	// XML PLUGINS to load 
	private static final ArrayList<XMLPluginTypeLoaderAndProcessor<?>> LOADED_PLUGINS = new ArrayList<>();
	private static HashSet<String> XSD_PLUGIN_FILES = new HashSet<>();
	private static XMLExecutorProcessor PLUGIN_EXECUTOR_PARSER;
	private static XMLProcessBlockProcessor PLUGIN_PROCESSBLOCK_PARSER;
	
	static {
		BLOCKED_CONST_NAMES.put("TMP", "It is internally used for storage of executors working directories (see attribute workingDir of executor).");
		
		BASE_ROOT_TYPES.add("param_types.xsd");
		BASE_ROOT_TYPES.add("base_param_types.xsd");
		
		// load all XML plugins that are installed
		PLUGIN_EXECUTOR_PARSER = new XMLExecutorProcessor();
		PLUGIN_PROCESSBLOCK_PARSER = new XMLProcessBlockProcessor();
		LOADED_PLUGINS.add(PLUGIN_EXECUTOR_PARSER);
		LOADED_PLUGINS.add(PLUGIN_PROCESSBLOCK_PARSER);
	}
	
	public static void setGUILoadAttempt(boolean guiLoadAttempt) {
		isGUILoadAttempt = guiLoadAttempt;
	}
	
	public static boolean isGUILoadAttempt() {
		return isGUILoadAttempt;
	}
	
	public static void setNoExitInCaseOfError(boolean noExit) {
		noExitInCaseOfError = noExit;
	}
	
	public static boolean isNoExit() {
		return noExitInCaseOfError;
	}
	
	/**
	 * parses a single xml file
	 * @param filenamePath
	 * @param schemaPath
	 * @param ignoreExecutor
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	@SuppressWarnings({ "rawtypes", "resource", "unchecked" })
	public static Object[] parse(String filenamePath, String schemaPath, int ignoreExecutor, boolean enforceNameUsage, boolean noExit, boolean validationMode, boolean disableCheckpoint, boolean forceLoading) throws SAXException, IOException, ParserConfigurationException {	
		if(isGUILoadAttempt() || isNoExit())
			noExit = true;
		String watchdogBaseDir = new File(schemaPath).getParentFile().getParent();
		
		// will be executed only once --> init plugins
		initPlugins(watchdogBaseDir, LOGGER, noExit, isGUILoadAttempt());

		HashMap<String, Integer> name2id = new HashMap<>();
		String mail = null;
		
		// test if not a template was loaded
		if(XMLParser.testIfTemplate(filenamePath)) {
			LOGGER.error("Templates can not be loaded. Set the 'isTemplate' attribute to false if this file is not a template.");
			if(!noExit) System.exit(1);
			throw new IllegalArgumentException("Templates can not be loaded. Set the 'isTemplate' attribute to false if this file is not a template.");
		}
		
		// ensure that the tmp folder is there
		File tmpFolder = new File(watchdogBaseDir + File.separator + TMP_FOLDER);
		if(!tmpFolder.exists()) {
			if(!tmpFolder.mkdirs()) {
				LOGGER.error("Could not create temporary folder '"+tmpFolder.getAbsolutePath()+"'.");
				if(!noExit) System.exit(1);
				throw new IllegalArgumentException("Could not create temporary folder '"+tmpFolder.getAbsolutePath()+"'.");
			}
		}
		if(!tmpFolder.canRead() || !tmpFolder.canRead()) {
			LOGGER.error("Could not read/write in temporary folder '"+tmpFolder.getAbsolutePath()+"'.");
			if(!noExit) System.exit(1);
			throw new IllegalArgumentException("Could not read/write in temporary folder '"+tmpFolder.getAbsolutePath()+"'.");
		}
		
		Functions.filterErrorStream();
		
		// create a new document factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringElementContentWhitespace(true);
		dbf.setNamespaceAware(true);
		
		/****************** auto-load modules and plugins **************************/
		File xmlFile = new File(filenamePath);
		// get all module folders
		ArrayList<String> moduleFolders = getModuleFolders(dbf, xmlFile, watchdogBaseDir);
		// get all modules stored in that folders
		HashMap<String, String> moduleName2Path = findModules(dbf, moduleFolders);
				
		// get all the modules that must be loaded
		getModules2LoadAndCheckID(dbf, xmlFile);
		HashSet<String> modules2load = getModules2Load(dbf, xmlFile);
		boolean allTasksHaveIDs = !enforceNameUsage && hasAllTasksNumericIDs(dbf, xmlFile) && areAllDependenciesNumeric(dbf, xmlFile);
	
		Pair<File, HashSet<String>> tmpXSDInfo = createTemporaryXSDFile(schemaPath, modules2load, moduleName2Path, moduleFolders, null);
		if(tmpXSDInfo == null) {
			if(!noExit) System.exit(1);
			return null;
		}
		File includedSchemaPath = tmpXSDInfo.getKey();
		HashSet<String> includedXSDFiles = tmpXSDInfo.getValue();
		/***************************************************************/
		
		// get the return information
		HashMap<String, Pair<HashMap<String, ReturnType>, String>> retInfo = getReturnInformation(dbf, includedXSDFiles, watchdogBaseDir);

		// load the schema in XSD 1.1 format
		SchemaFactory schemaFac = SchemaFactory.newInstance(XML_1_1);
		Schema schema = schemaFac.newSchema(includedSchemaPath);
		dbf.setSchema(schema);
		
		// validate the file if it is not disabled in order to load incomplete XML files
		Element docEle = null;
		if(!forceLoading) {
			// get a path for a temporary file
			Path tmpFile = Files.createTempFile("watchdog_" + new File(filenamePath).getName(), ".xml.tmp");
			tmpFile.toFile().deleteOnExit();
	
			// validate the stuff
			Validator validator = schema.newValidator();
			try {
				// validate the XML as it is
				validator.validate(new StreamSource(new File(filenamePath)));
				if(!XMLParser.isGUILoadAttempt()) {
					// parse the constants out of the original file
					HashMap<String, String> consts = XMLParser.getConstants(XMLParser.getRootElement(dbf, new File(filenamePath)));
					ArrayList<String> out = new ArrayList<>();
					for(String line : Files.readAllLines(Paths.get(filenamePath))) {
						out.add(XMLParser.replaceConstants(line, consts));
					}
					// write the changed content to the file
					Functions.write(tmpFile, StringUtils.join(out, NEWLINE));
	
					// validate it again
					validator.validate(new StreamSource(tmpFile.toFile()));
				}
				// do not change anything
				else 
					Files.copy(new File(filenamePath).toPath(), tmpFile, StandardCopyOption.REPLACE_EXISTING);
	
			}
			catch(Exception e) {
				e.printStackTrace();
				int lineNumber = -1;
				// try to get the line number
				try{
					StringWriter errorWriter = new StringWriter();
					e.printStackTrace(new PrintWriter(errorWriter));
					String error = errorWriter.getBuffer().toString();
					lineNumber = Integer.parseInt(error.split(System.lineSeparator())[0].split("; ")[2].split(": ")[1]);
				}
				catch(Exception ee) { }
			
				// remove the trailing trash message
				String error = e.getMessage().split("\n")[0].replaceFirst("cvc-assertion.failure: ", "");
				
				// print the error
				LOGGER.error("XML file is not valid! (line: " + lineNumber + ")");
				LOGGER.error(error);
				if(!noExit) System.exit(1);
				throw new IllegalArgumentException(error);
			}
			
			// document is valid --> parse it
			docEle = XMLParser.getRootElement(dbf, tmpFile.toFile());
			tmpFile.toFile().delete();
		}
		else
			docEle = XMLParser.getRootElement(dbf, xmlFile);
		
		LinkedHashMap<String, ProcessBlock> blocks = new LinkedHashMap<>();
		LinkedHashMap<String, Element> envs = new LinkedHashMap<>();
		LinkedHashMap<String, String> consts;
		LinkedHashMap<String, ExecutorInfo> exec = new LinkedHashMap<>();
		HashSet<Integer> ids = new HashSet<>();
		ExecutorInfo defaultExecutor = null;
		
		// check, if XML has right ROOT node
		if(ROOT.equals(docEle.getTagName())) {
			/********** try to find watchdogBase attribute */
			File watchdogBase = new File(XMLParser.getAttribute(docEle, WATCHDOG_BASE)); 
			if(watchdogBase.exists() && watchdogBase.isDirectory() && watchdogBase.canRead()) {
				if(checkWatchdogXSD(watchdogBase.getAbsoluteFile())) {
					Functions.setTemporaryFolder(watchdogBase.getAbsolutePath() + File.separator + TMP_FOLDER);
					
					// get constants
					consts = XMLParser.getConstants(docEle);
					
					
					/********** check for base folders */
					ProcessBlock processblock = null;
					int baseFolderConstCounter = 1;
					NodeList baseFolder = docEle.getElementsByTagName(BASE_FOLDER);
					for(int i = 0 ; i < baseFolder.getLength(); i++) {
						Element elBaseFolder = (Element) baseFolder.item(i);
						String baseName = ProcessBlock.cleanFilename(XMLParser.getAttribute(elBaseFolder, FOLDER));
						Integer maxDepth = Integer.parseInt(XMLParser.getAttribute(elBaseFolder, MAX_DEPTH));
						if(maxDepth == -1) { maxDepth = null; } // erase default value
						
						// introduce a new constant, if a GUI load attempt as baseFolders are not supported by the GUI
						if(isGUILoadAttempt() && !XMLParser.PATTERN_CONST.matcher(baseName).matches()) {
							String newConstName = null;
							while(newConstName == null || consts.containsKey(newConstName)) {
								newConstName = BASE_FOLDER_CONST_PREFIX + baseFolderConstCounter++;
							}
							consts.put(newConstName, baseName);
							baseName = "${" + newConstName + "}";	
						}
						
						// go through each of the annotated process blocks
						Element el;
						NodeList proccessblockList = elBaseFolder.getChildNodes();
						for(int ii = 0 ; ii < proccessblockList.getLength(); ii++) {
							if(proccessblockList.item(ii) instanceof Element) {
								el = (Element) proccessblockList.item(ii);
								String type = el.getTagName();
								String name = XMLParser.getAttribute(el, NAME);
								
								// get appropriate parser
								processblock = null;
								try {
									processblock = PLUGIN_PROCESSBLOCK_PARSER.parseElement(el, watchdogBaseDir, new Object[] { baseName, consts, validationMode, maxDepth});
									addProcessBlock(processblock, blocks, noExit);
								} catch(IllegalArgumentException ex) {
									LOGGER.error("ProcessBlock plugin for process block '"+name+"' of type <"+type+"> was not found.");
									ex.printStackTrace();
									if(!noExit) System.exit(1);
								}	
							}
						}
					}
						
					/********** check for the process block tag outside of base folders*/
					NodeList processBlockNodes = docEle.getElementsByTagName(PROCESS_BLOCK);
					if(processBlockNodes.getLength() == 1) {
						Element el = (Element) processBlockNodes.item(0);
						NodeList proccessblockList = el.getChildNodes();

						// go through each of the annotated process blocks
						for(int i = 0 ; i < proccessblockList.getLength(); i++) {
							if(proccessblockList.item(i) instanceof Element) {
								el = (Element) proccessblockList.item(i);
								String type = el.getTagName();
								String name = XMLParser.getAttribute(el, NAME);
								
								// this was already parsed 
								if(type.equals(XMLParser.BASE_FOLDER))
									continue;
								
								// get appropriate parser
								processblock = null;
								try {
									processblock = PLUGIN_PROCESSBLOCK_PARSER.parseElement(el, watchdogBaseDir, new Object[] { null, consts, validationMode, null});
									addProcessBlock(processblock, blocks, noExit);
								} catch(IllegalArgumentException ex) {
									LOGGER.error("ProcessBlock plugin for process block '"+name+"' of type <"+type+"> was not found.");
									ex.printStackTrace();
									if(!noExit) System.exit(1);
								}	
							}
						}
					}
					else if(processBlockNodes.getLength() > 1) {
						LOGGER.error("Only one '<" +  PROCESS_BLOCK + "'> tag is allowed but another one was found!");
						if(!noExit) System.exit(1);
					}
										
					/********** check for environments tag */
					NodeList environments = docEle.getElementsByTagName(ENVIRONMENTS);
					if(environments.getLength() == 1) {
						Element el = (Element) environments.item(0);
						NodeList environmentList = el.getChildNodes();
						
						// go through each of the annotated environments
						for(int i = 0 ; i < environmentList.getLength(); i++) {
							if(environmentList.item(i) instanceof Element) {
								el = (Element) environmentList.item(i);
								String type = el.getTagName();
								String name = null;
								if(ENVIRONMENT.equals(type)) {
									name = XMLParser.getAttribute(el, NAME);
									// missing name is not allowed within the environments tag!
									if(name == null || name.length() == 0) {
										LOGGER.error("'<"+ENVIRONMENT+">' tag without 'name' attribute is not allowed within '<"+ENVIRONMENTS+">'.");
										if(!noExit) System.exit(1);
									}									
									// save in list
									if(envs.containsKey(name)) {
										LOGGER.error("'<"+ENVIRONMENT+">' with the name '"+ name +"' is already defined!");
										if(!noExit) System.exit(1);
									}
									else {
										envs.put(name, el);
									}
								}
							}
						}
					}
					else if(environments.getLength() > 1) {
						LOGGER.error("Found multiple '<"+ENVIRONMENTS+">' elements!");
						if(!noExit) System.exit(1);
					}
					
					// ignore all the executor stuff and use only a local executor!
					if(ignoreExecutor != 0) {
						defaultExecutor = new LocalExecutorInfo(XMLParser.LOCAL, DEFAULT_LOCAL_NAME , true, false, null, ignoreExecutor, watchdogBaseDir, new Environment(XMLParser.DEFAULT_LOCAL_COPY_ENV, true, true), null);		
					}
					else {
						/********** check for executor tag */
						NodeList executors = docEle.getElementsByTagName(EXECUTORS);
						// just add the default local executor
						if(executors.getLength() == 0) {
							defaultExecutor = new LocalExecutorInfo(XMLParser.LOCAL, DEFAULT_LOCAL_NAME , true, false, null, 1, watchdogBaseDir, new Environment(XMLParser.DEFAULT_LOCAL_COPY_ENV, true, true), null);
						}
						else if(executors.getLength() == 1) {
							Element el = (Element) executors.item(0);
							NodeList executorsList = el.getChildNodes();
							
							// go through each of the annotated executors
							for(int i = 0 ; i < executorsList.getLength(); i++) {
								if(executorsList.item(i) instanceof Element) {
									el = (Element)executorsList.item(i);
									String type = el.getTagName();
									String name = XMLParser.getAttribute(el, NAME);

									// get environment is some is set
									String environment =  XMLParser.getAttribute(el, ENVIRONMENT);					
									Environment envExecutor = null;
									// check, if environment is there, if it is set and get the information if it is the case
									if(environment != null && environment.length() > 0) {
										if(!envs.containsKey(environment)) {
											LOGGER.error("Environment with name '" + environment + "' does not exist for executor with name '" + name + "'.");
											if(!noExit) System.exit(1);
										}
										else {
												envExecutor = XMLParser.parseEnvironment(environment, envs.get(environment), LOCAL.equals(type), null, null);
											}
									}
									
									
									// get appropriate parser
									ExecutorInfo exinfo = null;
									try {
										exinfo = PLUGIN_EXECUTOR_PARSER.parseElement(el, watchdogBaseDir, new Object[] { envExecutor });
									} catch(IllegalArgumentException ex) {
										LOGGER.error("Executor plugin for executor '"+name+"' of type <"+type+"> was not found.");
										ex.printStackTrace();
										if(!noExit) System.exit(1);
									}	
									// save in list
									exec.put(name, exinfo);
									
									// store default executor
									if(exinfo.isDefaultExecutor()) {
										if(defaultExecutor != null) {
											LOGGER.error("Only one default executor is allowed but a second one was found!");
											if(!noExit) System.exit(1);
										}
										defaultExecutor = exinfo;
									}
								}
							}
						}
						else {
							LOGGER.error("Found multiple '<"+EXECUTORS+">' elements!");
							if(!noExit) System.exit(1);
						}
					}
	
					/********** check for tasks */
					HashMap<String, HashSet<String>> usedExecutors = new HashMap<>();
					NodeList rootTask = docEle.getElementsByTagName(TASKS);
					if(rootTask.getLength() == 1) {
						ArrayList<XMLTask> parsedTasks = new ArrayList<>();
						
						Element el = (Element) rootTask.item(0);
						NodeList tasks = el.getChildNodes();
						String projectName = XMLParser.getAttribute(el, PROJECT_NAME);
						mail = XMLParser.getAttribute(el, MAIL);
						int biggestID = 1;
						
						// get to each of the annotated tasks
						for(int i = 0 ; i < tasks.getLength(); i++) {
							if(tasks.item(i) instanceof Element) {
								Element task = (Element) tasks.item(i);
								// first get the attributes
								String binName = task.getAttribute(BIN_NAME);
								String preBinName = task.getAttribute(PRE_BIN_COMMAND);
								boolean isWatchdogModule = Boolean.parseBoolean(task.getAttribute(IS_WATCHDOG_MODULE));
								int id;
								String name = XMLParser.getAttribute(task, NAME);
								
								// test if numeric ID is given
								if(allTasksHaveIDs)
									id = Integer.parseInt(task.getAttribute(ID));
								else {
									id = biggestID++;
				
									// test, if that name is already used!
									if(name2id.containsKey(name)) { 
										LOGGER.error("Task name '" + name + "' is already used in non-ID mode.");
										id = -1;
										if(!noExit) System.exit(1);
									}
									else {
										name2id.put(name, id);
										// also save ID if given for dependencies
										if(task.hasAttribute(ID))
											name2id.put(task.getAttribute(ID), id);
									}
								}
									
								String taskType = task.getTagName();
								String taskExecutor = XMLParser.getAttribute(task, EXECUTOR);
								String environment = XMLParser.getAttribute(task, ENVIRONMENT);
								String processBlock = XMLParser.getAttribute(task, PROCESS_BLOCK);
								String notify = XMLParser.getAttribute(task, NOTIFY);
								String checkpoint = (!disableCheckpoint) ? XMLParser.getAttribute(task, CHECKPOINT) : null;
								String confirmParam = XMLParser.getAttribute(task, CONFIRM_PARAM);
								int maxRunning = Integer.parseInt(XMLParser.getAttribute(task, MAX_RUNNING));
								
								ProcessBlock pb = null;
								OptionFormat globalOptionFormater = getFormater(task);
								ExecutorInfo executorInfo = null;
								Environment env = null;
								ArrayList<TaskAction> taskActions = new ArrayList<>();
								 
								// check, if executor info should be ignored!
								if(ignoreExecutor != 0) {
									executorInfo = defaultExecutor;
								}
								else {
									// check, if executor is set
									if(taskExecutor.length() > 0) {
										if(exec.containsKey(taskExecutor)) {
											executorInfo = exec.get(taskExecutor);
										}
										else {
											LOGGER.error("Executor with name '" + taskExecutor + "' does not exist for task with ID '" + id + "'.");
											if(!noExit) System.exit(1);
										}
									}
									// set default executor
									else if(defaultExecutor != null) {
										executorInfo = defaultExecutor;
									}
									else {
										LOGGER.error("No executor attribute was set for task with ID '" + id + "' and no default executor was defined.");
										if(!noExit) System.exit(1);
									}
								}
								// save executor that are really used
								String exType = executorInfo.getType();
								if(!usedExecutors.containsKey(exType))
									usedExecutors.put(exType, new HashSet<String>());
								usedExecutors.get(exType).add(executorInfo.getName());

								// check, if environment is there, if it is set and store the information
								if(environment.length() > 0) {
									if(!envs.containsKey(environment)) {
										LOGGER.error("Environment with name '" + environment + "' does not exist for task with ID '" + id + "'.");
										if(!noExit) System.exit(1);
									}
									else {
										env = XMLParser.parseEnvironment(environment, envs.get(environment), executorInfo instanceof LocalExecutorInfo, null, null);
									}
								}
								// check, if a default env should be used that is set via the executor
								else if(executorInfo.hasDefaultEnv()) {
								//		env = executorInfo.getEnv(); // DO not show this on GUI
								}
								
								// try to get the correct enums
								ActionType notifyEnum = getActionType(notify);
								ActionType checkpointEnum = getActionType(checkpoint);
								ActionType confirmParamEnum = getActionType(confirmParam);
																
								// update watchdog call if needed
								if(isWatchdogModule) {
									// make command relative to the module folder it is located in
									binName = new File(new File(moduleName2Path.get(taskType)).getParent() + File.separator + binName).getAbsolutePath();
								}
								if(preBinName != null && preBinName.length() > 0) {
									binName = preBinName + " " + binName;
								}
		
								// test, if id is valid
								if(ids.contains(id)) {
									LOGGER.error("Duplicate task entry for id '" + id + "'.");
									if(!noExit) System.exit(1);
								}

								// test, if process block is valid
								if(processBlock.length() > 0) {
									if(blocks.containsKey(processBlock)) {
										pb = blocks.get(processBlock);
										
										// check, if process block is a processInput and if yes -> create a copy of it!
										/*if(pb instanceof ProcessInput) {
											pb = new ProcessInput(pb.getName(), ((ProcessInput) pb).getGlobalSep(), ((ProcessInput) pb).getReplaceDefaultGroup());
										}*/ //TODO: why?!?!?!
									}
									else {
										// find it the hard way
										boolean valid = false;
										String withSuffix = processBlock + XMLParser.SUFFIX_SEP;
										for(String key : blocks.keySet()) {
											if(key.startsWith(withSuffix)) {
												valid = true;
												pb = blocks.get(key);
												break;
											}
										}
										if(!valid) {
											LOGGER.error("ProcessGroup '" + processBlock + "' was not defined before.");
											if(!noExit) System.exit(1);
										}
									}
								}			
 
								// create new task
								XMLTask x = new XMLTask(id, taskType, binName, name, projectName,  globalOptionFormater, executorInfo, pb);
								x.setMaxRunning(maxRunning);
								x.setNotify(notifyEnum);
								x.setCheckpoint(checkpointEnum);
								x.setConfirmParam(confirmParamEnum);

								if(retInfo.containsKey(taskType))
									x.setReturnParameter(retInfo.get(taskType).getKey());
								
								// get GUI info
								if(task.hasAttribute(POSX) || task.hasAttribute(POSY)) {
									GUIInfo gInfo = new GUIInfo();
									if(task.hasAttribute(POSX)) gInfo.setPosX(Integer.parseInt(task.getAttribute(POSX)));
									if(task.hasAttribute(POSY)) gInfo.setPosY(Integer.parseInt(task.getAttribute(POSY)));
									x.setGuiInfo(gInfo);
								}
								
								// get depends tag
								NodeList dependsTags = task.getElementsByTagName(DEPENDENCIES);
								if(dependsTags.getLength() == 1) {
									el = (Element) dependsTags.item(0);
									
									// read the parameters and add them
									NodeList depends = el.getChildNodes();
									for(int ii = 0 ; ii < depends.getLength(); ii++) {
										if(depends.item(ii) instanceof Element) {
											el = (Element) depends.item(ii);
											String type = el.getSchemaTypeInfo().getTypeName();
											if(DEPENDS_TYPE.equals(type)) {
												String sID = el.getTextContent();
												boolean separate = Boolean.parseBoolean(XMLParser.getAttribute(el, SEPARATE));
												String prefixName = null;
												String sep = null;
												int dId = -1;

												// test, if a normal numeric ID is given
												try {
													dId = Integer.parseInt(sID);
												}
												catch(Exception e) {
													// get ID from hashmap
													if(name2id.containsKey(sID))
														dId = name2id.get(sID);
												}
												if(dId == -1) {
													LOGGER.error("Dependency with ID '" + sID + "' was not defined before.");
													if(!noExit) System.exit(1);
												}
												
												// get the prefix name, if separate mode
												if(separate) {
													prefixName = XMLParser.getAttribute(el, PREFIX_NAME);
													sep = XMLParser.getAttribute(el, SEP);
												}
												
												// test, if dependency ID is valid
												if(!ids.contains(dId)) {
													LOGGER.error("Dependency with ID '" + dId + "' was not defined before.");
													if(!noExit) System.exit(1);
												}
												else {
													// get the return values, the task will be able to access
													String depType = XMLTask.getXMLTask(dId).getTaskType();
													HashSet<String> returnInfoValues = new HashSet<>();
													if(retInfo.containsKey(depType) && (pb != null && pb instanceof ProcessReturnValueAdder)) {
														returnInfoValues.addAll(retInfo.get(depType).getKey().keySet());
													}
													// add the stuff
													x.addDependencies(dId, separate, prefixName, sep, returnInfoValues);
												}
											}
										}
									}
								}
								else if(dependsTags.getLength() > 1) {
									LOGGER.error("Found multiple '<"+ENVIRONMENT+">' elements!");
									if(!noExit) System.exit(1);
								}
								
								// get checkers, if some are defined
								NodeList checkersList = task.getElementsByTagName(CHECKERS);
								if(checkersList.getLength() == 1) {
									el = (Element) checkersList.item(0);			
									// read the checker and add them
									NodeList checkers = el.getChildNodes();
									for(int ii = 0 ; ii < checkers.getLength(); ii++) {
										if(checkers.item(ii) instanceof Element) {
											el = (Element) checkers.item(ii);
											
											// get values
											String className = el.getAttribute(CLASS_NAME);
											String classPath = el.getAttribute(CLASS_PATH);
											String type = el.getAttribute(TYPE);
											ArrayList<Class> constructorArguments = new ArrayList<>();
											constructorArguments.add(Task.class);
											ArrayList<Pair<Class, String>> parsedArgs = new ArrayList<>();
											// get constructor arguments
											NodeList cargs = el.getElementsByTagName(C_ARG);
											for(int iii = 0 ; iii < cargs.getLength(); iii++) {
												if(cargs.item(iii) instanceof Element) {
													el = (Element) cargs.item(iii);
													String cType = el.getAttribute(TYPE);
													String aValue = el.getTextContent();
													Class c = null;
													
													if(cType.equals(STRING))
														c = String.class;
													else if(cType.equals(DOUBLE))
														c = Double.class;
													else if(cType.equals(INTEGER))
														c = Integer.class;
													else if(cType.equals(BOOLEAN))
														c = Boolean.class;
													else
														LOGGER.error("Invalid type '"+cType+"' in constructor arguments for value '"+aValue+"' for task with id '"+id+"'.");
													
													// store the information for later
													checkInputVariableNames(x, aValue);
													parsedArgs.add(Pair.of(c, aValue));
													constructorArguments.add(c);
												}
											}
											
											// check, if class file is there
											File classFile = new File(classPath);
											if(!classFile.exists()) {
												LOGGER.error("Class file for checker '"+classFile.getAbsolutePath()+"' could not be opened.");
												if(!noExit) System.exit(1);
											}
											// try to create a instance of the class
											try {
											    URL url = classFile.getParentFile().toURI().toURL();
											    URL[] urls = new URL[]{url};
											    ClassLoader cl = new URLClassLoader(urls);
											    Class cls = cl.loadClass(className);
											    Constructor construct = cls.getConstructor(constructorArguments.toArray(new Class[0]));
											    
											    // save the checker!
											    x.addChecker(construct, parsedArgs, ERROR.equals(type), classFile);
											    	
											} catch (Exception e) {
												LOGGER.error("Checker of type '"+type+"' with class '"+className+"' could not be instantiated. Perhaps there is no constructor matching the provided arguments.");
												e.printStackTrace();
												if(!noExit) System.exit(1);
											}
											
										}
									}
								}
								else if(checkersList.getLength() > 1) {
									LOGGER.error("Found multiple '<"+CHECKERS+">' elements!");
									if(!noExit) System.exit(1);
								}

								// get stream tag
								NodeList streamTag = task.getElementsByTagName(STREAMS);
								if(streamTag.getLength() == 1) {
									el = (Element) streamTag.item(0);
									boolean wasWorkingDirSet = false;
									boolean saveResourceUsage = Boolean.parseBoolean(XMLParser.getAttribute(el, SAVE_RESOURCE_USAGE));
									
									// read the parameters and add them
									NodeList streams = el.getChildNodes();
									for(int ii = 0 ; ii < streams.getLength(); ii++) {
										if(streams.item(ii) instanceof Element) {
											el = (Element) streams.item(ii);
											// get values
											String pname = el.getTagName();
											String paramValue = XMLParser.getTextContent(el);
											checkInputVariableNames(x, paramValue);
											
											// get working directory info
											if(WORKING_DIR.equals(pname)) {
													x.setWorkingDirectory(paramValue);
													wasWorkingDirSet = true;
											}
											// add stream settings
											else if(STD_ERR.equals(pname) || STD_OUT.equals(pname) || STD_IN.equals(pname)) {
												// check, if some of the path are relative but no working dir was set
												if(!wasWorkingDirSet && !paramValue.startsWith(File.separator) && !paramValue.matches("^[A-Z]:\\\\.*") && !paramValue.matches(REPLACE_CHARS)) {
													LOGGER.error("<" + pname + "> does only accept a relative path if a working directory was set.");
													if(!noExit) System.exit(1);
												}
												
												// stdin param
												if(STD_IN.equals(pname)) {
													boolean disableExistenceCheck = Boolean.parseBoolean(XMLParser.getAttribute(el, DISABLE_EXISTANCE_CHECK));
													x.setDisableExistenceCheckStdin(disableExistenceCheck);
													if(paramValue.length() > 0) {
														File path = new File(paramValue);
													
														// test, if folder exists
														if(!disableExistenceCheck && !(path.exists() && path.isDirectory() && path.canRead() && path.canExecute())) {
															LOGGER.error("Stdin file with path '" + path.getAbsolutePath() + "' was not found!");
															if(!noExit) System.exit(1);
														}
														x.setInputStream(paramValue);
													}
												}
												// stdout and stdin param
												else {
													boolean append = Boolean.parseBoolean(XMLParser.getAttribute(el, APPEND));
													
													if(STD_OUT.equals(pname)) {
														x.setOutputStream(paramValue, append);
													}
													else {
														x.setErrorStream(paramValue, append);
													}
												}
											}
										}
									}
									// ensure that output file is set
									if(x.getPlainStdOut() == null)
										saveResourceUsage = false;
									x.setSaveResourceUsage(saveResourceUsage);
								}
								
								// get local environment tag, might override the stuff which was set before
								NodeList localEnvTags = task.getElementsByTagName(ENVIRONMENT);
								if(localEnvTags.getLength() == 1) {
									el = (Element) localEnvTags.item(0);
									env = XMLParser.parseEnvironment("local environment", el, executorInfo instanceof LocalExecutorInfo, envs.get(environment), x);
								}
								else if(localEnvTags.getLength() > 1) {
									LOGGER.error("Found multiple '<"+ENVIRONMENT+">' elements!");
									if(!noExit) System.exit(1);
								}
								
								// get Argument tag
								NodeList argumentLists = task.getElementsByTagName(PARAMETER);
								if(argumentLists.getLength() == 1) {
									el = (Element) argumentLists.item(0);			
									// read the parameters and add them
									NodeList params = el.getChildNodes();
									for(int ii = 0 ; ii < params.getLength(); ii++) {
										if(params.item(ii) instanceof Element) {
											el = (Element) params.item(ii);
											
											// get values
											String pname = el.getTagName();
											String paramValue = XMLParser.getTextContent(el);
											String type = el.getSchemaTypeInfo().getTypeName();
											int retDepID = -1;
											
											// ensure that no ProcessInput syntax is used, when no ProcessInput block is given
											if(!(pb != null && pb instanceof ProcessMultiParam)) {
												if(ReplaceSpecialConstructs.PATTERN_TABLE_COL_NAME.matcher(paramValue).matches()) {
													LOGGER.error("Variables can only be used in the context for process tables or process input blocks. parameter: '"+pname+"'; value: '"+paramValue+"' of task with id '"+id+"'");
													if(!noExit) System.exit(1);
												}
											}
											// ensure that used return values match the variables
											String usedReturnName = checkInputVariableNames(x, paramValue);
											if(usedReturnName != null)
												retDepID = x.getTaskBasedOnNameOfReturnVariable(usedReturnName);
									
											// test, if parameter is a flag which must be called without argument and only with one "-"
											if(FLAG_TYPE.endsWith(type)) {
												// check, if flag is set to false, do not add it.
												if(Functions.isTrueXMLValue(paramValue)) {
													x.addFlag(pname, getFormater(el));
												}
												else { // add the flag
													x.addFlag(DISABLE_FLAG + pname, getFormater(el));
												}
											}
											// add normal parameter
											else {												
												x.addParameter(pname, paramValue, getFormater(el), retDepID);
											}
										}
									}
								}
								else if(argumentLists.getLength() > 1) {
									LOGGER.error("Found multiple '<"+PARAMETER+">' elements!");
									if(!noExit) System.exit(1);
								}
	
								// get actions tag
								NodeList actionTags = task.getElementsByTagName(ACTIONS);
								for(int ii = 0; ii < actionTags.getLength(); ii++) {
									el = (Element) actionTags.item(ii);
									// parse all the actions that are contained in that type
									NodeList childs = el.getChildNodes();
									Element action;
									TaskActionTime time = TaskActionTime.getTaskActionTime(XMLParser.getAttribute(el, TIME));
									boolean uncoupleFromExecutor = Boolean.parseBoolean(XMLParser.getAttribute(el, UNCOUPLE_FROM_EXECUTOR));
									
									for(int iii = 0 ; iii < childs.getLength(); iii++) {
										if(childs.item(iii) instanceof Element) {
											// check, if local executor or in slave mode
											if(!uncoupleFromExecutor && !executorInfo.isStick2Host()) {
												x.setForceSingleSlaveMode(true);
											}
											
											action = (Element) childs.item(iii);
											String type = action.getTagName();

											// check, which type we got!
											if(CREATE_FILE.equals(type) || CREATE_FOLDER.equals(type)) {
												boolean isFileType = CREATE_FILE.equals(type);
												String path = XMLParser.getAttribute(action, isFileType ? FILE : FOLDER);
												checkInputVariableNames(x, path);
												boolean override = Boolean.parseBoolean(XMLParser.getAttribute(action, OVERRIDE));
												boolean createParent = Boolean.parseBoolean(XMLParser.getAttribute(action, CREATE_PARENT));
												
												taskActions.add(new CreateTaskAction(path, override, createParent, isFileType, time, uncoupleFromExecutor));
											}
											else if(COPY_FILE.equals(type) || COPY_FOLDER.equals(type)) {
												boolean isFileType = COPY_FILE.equals(type);
												String src = XMLParser.getAttribute(action, isFileType ? FILE : FOLDER);
												String dest = XMLParser.getAttribute(action, DESTINATION);
												checkInputVariableNames(x, src);
												checkInputVariableNames(x, dest);
												boolean override = Boolean.parseBoolean(XMLParser.getAttribute(action, OVERRIDE));
												boolean createParent = Boolean.parseBoolean(XMLParser.getAttribute(action, CREATE_PARENT));
												boolean deleteSource = Boolean.parseBoolean(XMLParser.getAttribute(action, DELETE_SOURCE));
												 
												taskActions.add(new CopyTaskAction(src, dest, override, createParent, deleteSource, isFileType, time, uncoupleFromExecutor));
											}
											else if(DELETE_FILE.equals(type) || DELETE_FOLDER.equals(type)) {
												boolean isFileType = DELETE_FILE.equals(type);
												String path = XMLParser.getAttribute(action, isFileType ? FILE : FOLDER);
												checkInputVariableNames(x, path);
												taskActions.add(new DeleteTaskAction(path, isFileType, time, uncoupleFromExecutor));
											}
										}
									}
								}
								
								// add the task actions, if some were set
								if(taskActions.size() > 0) {
									x.addTaskActions(taskActions);
								}
								
								// set the environment variables, if a local type, otherwise use the default values got from the executor if one is set
								if(env != null)
									x.setEnvironment(env);
								
								// save id in ID list and XMLTask object
								ids.add(id);
								parsedTasks.add(x);
							}
						}
						// test all used executors
						for(String type : usedExecutors.keySet()) {
							HashSet<String> namesOfExecutors = (HashSet<String>) usedExecutors.get(type).clone();
							PLUGIN_EXECUTOR_PARSER.runAdditionalTest(type, namesOfExecutors);
						}
						
						HashMap<String, Environment> enivronments = new HashMap<>();
						// convert environments
						for(String n : envs.keySet()) {
							Element e = envs.get(n);
							enivronments.put(n, XMLParser.parseEnvironment(n, e, false, null, null));
						}
						return new Object[] {parsedTasks, mail, null, retInfo, name2id, dbf, blocks, enivronments, exec, watchdogBaseDir, consts}; // third element is not used anymore
					}
					else  {
						LOGGER.error("multiple '<"+TASKS+">' elements!");
						if(!noExit) System.exit(1);
					}
				}
				else {
					LOGGER.error("Invalid watchdog base path! Can not find file '"+FILE_CHECK+"' in ('" + watchdogBase.getCanonicalPath() + "')!");
					if(!noExit) System.exit(1);
				}
			}
			else {
				LOGGER.error("Attribute '"+WATCHDOG_BASE+"' does not point to a valid watchdog path ('" + watchdogBase.getCanonicalPath() + "')!");
				if(!noExit) System.exit(1);
			}
		}
		else {
			LOGGER.error("Root element '<"+ROOT+">' was not found!");
			if(!noExit) System.exit(1);
		}
		return null;
	}

	private static void addProcessBlock(ProcessBlock processblock, LinkedHashMap<String, ProcessBlock> blocks, boolean noExit) {
		if(processblock != null) {
			// TODO: check, if some checks were lost during  code mode
			if(!blocks.containsKey(processblock.getName())) {
				blocks.put(processblock.getName(), processblock);
			}
			else {
				LOGGER.error("ProcessBlock with name '" + processblock.getName() + "' was already defined before and can not be joined with others!");
				if(!noExit) System.exit(1);
			}
		}
		else {
			// TODO: problem here?
			System.out.println("NULL returned by pb");
		}
	}

	/**
	 * if a new plugin should be used it must be init() here!
	 */
	public static void initPlugins(String watchdogBaseDir, Logger l, boolean noExit, boolean isGUILoadAttempt) {
		HashSet<String> xsd = null;
		
		for(XMLPluginTypeLoaderAndProcessor<?> xp : LOADED_PLUGINS) {
			xsd = xp.init(l, noExit, isGUILoadAttempt, watchdogBaseDir);
			if(xsd != null) 
				XSD_PLUGIN_FILES.addAll(xsd);
		}
	}

	/**
	 * checks, if a folder seems to be a watchdog installation
	 * @param baseDir
	 * @return
	 */
	public static boolean checkWatchdogXSD(File baseDir) {
		if(baseDir == null || baseDir.getName().equals(null))
			return false;
		
		File check = new File(baseDir + File.separator + FILE_CHECK);
		return check.exists() && check.isFile() && check.canRead();
	}

	/**
	 * creates a temporary XSD file in which all that modules are loaded
	 * @param modules2load
	 * @return
	 */
	public static Pair<File, HashSet<String>> createTemporaryXSDFile(String defaultSchemaPath, HashSet<String> modules2load, HashMap<String, String> moduleName2Path, ArrayList<String> moduleFolders, HashSet<String> includePluginFiles) {
		boolean noExit = false;
		if(isGUILoadAttempt() || isNoExit())
			noExit = true;
		// ensure that default path is there
		File f = new File(defaultSchemaPath);
		if(!(f.exists() && f.canRead())) {
			LOGGER.error("Can not find 'xsd/watchdog.xsd' file in watchdog base folder: '" + f.getParentFile().getParent() + "'");
			if(!noExit) System.exit(1);
		}
		
		// load all plugin XSD files
		if(includePluginFiles == null) {
			includePluginFiles = XSD_PLUGIN_FILES;
		}
		
		ArrayList<String> includeMod = new ArrayList<>();
		ArrayList<String> missingMod = new ArrayList<>();
		HashSet<String> includedXSDFiles = new HashSet<>();
		// check, if all needed modules could be located
		for(String m : modules2load) {
			if(moduleName2Path.containsKey(m)) {
				// add the include info
				includeMod.add(INCLUDE_XSD.replace(REPLACE, moduleName2Path.get(m)));
				includedXSDFiles.add(moduleName2Path.get(m));
			}
			else
				missingMod.add(m);
		}

		// print an error if modules are missing
		if(missingMod.size() > 0) {
			LOGGER.error("The following modules could not be located in '" + StringUtils.join(moduleFolders, "','") + "':");
			for(String m : missingMod)
				LOGGER.error(m);
			if(!noExit) System.exit(1);
		}
		
		// prepare plugin import
		ArrayList<String> includePlugins = new ArrayList<>();
		for(String pluginFile : includePluginFiles) {
			includePlugins.add(INCLUDE_XSD.replace(REPLACE, pluginFile));
		}
		
		// prepare the temporary XSD file
		// get a path for a temporary file
		try {
			Path includedSchemaPath = Files.createTempFile("watchdog", ".xsd.tmp");
			includedSchemaPath.toFile().deleteOnExit();

			boolean beforePluginImport = true;
			ArrayList<String> buf = new ArrayList<>();
			for(String line : Files.readAllLines(Paths.get(defaultSchemaPath))) {
				if(line.contains(REPLACE_PLUGIN_IMPORT)) {
					
					line = line.replace(REPLACE_PLUGIN_IMPORT, StringUtils.join(includePlugins, NEWLINE));
					beforePluginImport = false;
				}
				else if(line.contains(REPLACE_MODULE_IMPORT)) {
					line = line.replace(REPLACE_MODULE_IMPORT, StringUtils.join(includeMod, NEWLINE));
				}
				if(beforePluginImport) 
					line = line.replace(REPLACE_IMPORT_PATH, REPLACE_IMPORT_PATH + new File(defaultSchemaPath).getParent() + File.separator);
				
				// add the line
				buf.add(line);
	
			}
			Functions.write(includedSchemaPath, StringUtils.join(buf, NEWLINE));
			return Pair.of(includedSchemaPath.toFile(), includedXSDFiles);
		}
		catch(Exception e) {
			e.printStackTrace();
			if(!noExit) System.exit(1);
		}
		return null;
	}

	/**
	 * finds all XSD files that implement valid modules in that folder
	 * @param dbf 
	 * @param moduleFolders
	 * @return
	 */
	public static HashMap<String, String> findModules(DocumentBuilderFactory dbf, ArrayList<String> moduleFolders) {
		boolean noExit = false;
		if(isGUILoadAttempt() || isNoExit())
			noExit = true;
		ArrayList<String> xsd = new ArrayList<>();
		for(String dir : moduleFolders) {
			for(File mmf : new File(dir).listFiles()) {
				if(mmf.isDirectory()) {
					for(File xsdmmf : mmf.listFiles(new PatternFilenameFilter(XSD_PATTERN, false))) {
						xsd.add(xsdmmf.getAbsolutePath());
					}
				}
			}
		}
		HashMap<String, String> res = new HashMap<>();
		// get the name of the task defined in that file
		for(String x : xsd) {
			try {
				String name = getTaskTypeOfModule(dbf, new File(x));
				
				// it is a valid module
				if(name != null) {
					if(res.containsKey(name)) {
						LOGGER.error("There are at least two modules with the name '"+name+"' in '"+res.get(name)+"' and '"+x+"'!");
						if(!noExit) System.exit(1);
					}
					res.put(name, x);
				}
			}
			catch(Exception e) {
				LOGGER.error("Failed to get name of task from xsd file '"+x+"'.");
				e.printStackTrace();
				if(!noExit) System.exit(1);
			}
		}
		return res;
	}
	
	/**
	 * ensure that used return values match the variables
	 * returns the name of the variable that is used or null if none
	 */
	private static String checkInputVariableNames(XMLTask x, String value) {
		if(x == null)
			return null;
		boolean noExit = false;
		if(isGUILoadAttempt() || isNoExit())
			noExit = true;
		
		if(x.hasProcessBlock() && x.getProcessBlock() instanceof ProcessReturnValueAdder) {
			Matcher m = ReplaceSpecialConstructs.PATTERN_TABLE_COL_NAME.matcher(value);
			// test, if a var inputProcess block var is set
			if(m.matches()) {
				String usedReturnName = m.group(3);
				if(!x.isReturnVariableAvail(usedReturnName)) {
					LOGGER.error("ProcessInput block does not contain a return variable with name '"+usedReturnName+"' for task with id '"+x.getXMLID()+"'.");
					if(!noExit) System.exit(1);
				}
				else {
					return usedReturnName;
				}
			}
		}
		return null;
	}

	/**
	 * Parses a environment XML tag
	 * @param environment
	 * @param isLocalExecutor
	 * @return
	 */
	public static Environment parseEnvironment(String envname, Element environment, boolean isLocalExecutor, Element init, XMLTask x) {
		boolean noExit = false;
		if(isGUILoadAttempt() || isNoExit())
			noExit = true;
		boolean useExternalCommand = Boolean.parseBoolean(XMLParser.getAttribute(environment, USE_EXTERNAL_EXPORT));
		String exportCommand = XMLParser.getAttribute(environment, EXPORT_COMMAND);
		String shebang = XMLParser.getAttribute(environment, SHEBANG);
		boolean copyGlobal = Boolean.parseBoolean(XMLParser.getAttribute(environment, COPY_LOCAL_VALUE));
		String color = XMLParser.getAttribute(environment, COLOR);
		Environment ret = new Environment(envname, isLocalExecutor, copyGlobal, useExternalCommand);
		ret.setColor(color);
		
		// set additional values if an external command should be used
		if(useExternalCommand) {
			if(exportCommand != null && exportCommand.length() > 0)
				ret.setCommand(exportCommand);
			if(!shebang.isEmpty())
				ret.setShebang(shebang);
		}
		
		// init with the values that are acutally part of the init element
		ArrayList<Element> lookAt = new ArrayList<>();
		if(init != null)
			lookAt.add(init);
		
		// add normal environment
		lookAt.add(environment);

		for(Element e : lookAt) {
			NodeList childs = e.getChildNodes();
			// add the individual values, if some are given.
			for(int i = 0 ; i < childs.getLength(); i++) {
				if(childs.item(i).getNodeType() == Node.ELEMENT_NODE) {
					Element el = (Element) childs.item(i);
					if(VAR.equals(el.getTagName())) {
						String name = XMLParser.getAttribute(el, NAME);
						String value = el.getTextContent();
						checkInputVariableNames(x, value);
						
						// check, if a value should be updated
						boolean update = Boolean.parseBoolean(XMLParser.getAttribute(el, UPDATE));
						boolean copy = Boolean.parseBoolean(XMLParser.getAttribute(el, COPY_LOCAL_VALUE));
						String sep = XMLParser.getAttribute(el, SEP);
						
						// check, if update is allowed
						if(!isLocalExecutor && !useExternalCommand && !copy && update) {
							LOGGER.error("Could not update environment variables on cluster or remote hosts without 'useExternalExport' set.");
							if(!noExit) System.exit(1);
						}
						// add the variable
						ret.storeData(name, value, sep, copy, update);
					}
				}
			}
		}
		return ret;
	}
	
	/**
	 * Returns a formater or null if none of the required attributes are set
	 * @param el
	 * @return
	 */
	private static OptionFormat getFormater(Element el) {
		// get argument format stuff
		String paramFormatString = el.getAttribute(PARAM_FORMAT);
		String spacingFormatString = el.getAttribute(SPACING_FORMAT);
		String separateFormatString = el.getAttribute(SEPARATE_FORMAT);
		String quoteFormatString = el.getAttribute(QUOTE_FORMAT);
		
		// check, if any option is set
		if(paramFormatString.length() > 0 || spacingFormatString.length() > 0 || quoteFormatString.length() > 0 || separateFormatString.length() > 0) {
			// get formater
			ParamFormat pf = ParamFormat.getFormater(paramFormatString);
			SpacingFormat sf = SpacingFormat.getFormater(spacingFormatString);
			QuoteFormat qf = QuoteFormat.getFormater(quoteFormatString);

			// get the global parameter formater
			return new OptionFormat(pf, qf, sf, separateFormatString);
		}
		return null;
	}
	
	
	/**
	 * replaces constants in values
	 * @param value
	 * @param consts
	 * @return
	 */
	public static String replaceConstants(String value, final HashMap<String, String> consts) {
		boolean noExit = false;
		if(isGUILoadAttempt() || isNoExit())
			noExit = true;
		// check, if some value is set
		if(value == null || consts == null) 
			return value;

		// check, if the value contains a constant which must be replaced
		Matcher matcher = PATTERN_CONST.matcher(value);
		while(matcher.matches()) {
			String key = matcher.group(1);

			// check, if constant is there
			if(!consts.containsKey(key) && !BLOCKED_CONST_NAMES.containsKey(key)) {
				LOGGER.error("A constant with name '${"+key+"}' was used which was not defined before in the <"+CONSTANTS+"> element.");
				if(!noExit) System.exit(1);
			}
			
			// do not try to replace the variables at this stage as they are replaced later!
			if(BLOCKED_CONST_NAMES.containsKey(key))
				break;
			
			// replace constant
			value = value.replaceFirst(REPLACE_CONST, consts.get(key));
			matcher = PATTERN_CONST.matcher(value);
		}
		return value;
	}
	
	/**
	 * returns the attribute of the element
	 * @param el
	 * @param attrName
	 * @return
	 */
	public static String getAttribute(final Element el, String attrName) {
		return el.getAttribute(attrName);
	}
	
	/**
	 * returns the text content of the element
	 * @param el
	 * @param consts
	 * @return
	 */
	private static String getTextContent(final Element el) {
		return el.getTextContent();
	}
	
	
	/**
	 * Parses constants from the root element of the document
	 * @param docEle
	 * @return
	 */
	public static LinkedHashMap<String, String> getConstants(Element docEle) {
		boolean noExit = false;
		if(isGUILoadAttempt() || isNoExit())
			noExit = true;
		LinkedHashMap<String, String> consts = new LinkedHashMap<>();
		/********** check for constants tag */
		NodeList constants = docEle.getElementsByTagName(CONSTANTS);
		if(constants.getLength() == 1) {
			Element el = (Element) constants.item(0);
			NodeList constantsList = el.getChildNodes();
			
			// go through each of the annotated constants
			for(int i = 0 ; i < constantsList.getLength(); i++) {
				if(constantsList.item(i) instanceof Element) {
					el = (Element) constantsList.item(i);
					String type = el.getTagName();
					if(CONST.equals(type)) {
						String name = XMLParser.getAttribute(el, NAME);
						// missing name is not allowed within the environments tag!
						if(name == null || name.length() == 0) {
							LOGGER.error("'<"+CONST+">' tag without 'name' attribute is not allowed within '<"+CONSTANTS+">'.");
							if(!noExit) System.exit(1);
						}
						if(BLOCKED_CONST_NAMES.containsKey(name)) {
							LOGGER.error("'<"+CONST+">' with the name '"+ name +"' can not be used:");
							LOGGER.info(BLOCKED_CONST_NAMES.get(name));
							if(!noExit) System.exit(1);
						}
						// save in list
						if(consts.containsKey(name)) {
							LOGGER.error("'<"+CONST+">' with the name '"+ name +"' is already defined!");
							if(!noExit) System.exit(1);
						}
						else {
							consts.put(name, replaceConstants(el.getTextContent().replace("\\", "\\\\"), consts));
						}
					}
				}
			}
		}
		else if(constants.getLength() > 1) {
			LOGGER.error("Found multiple '<"+CONSTANTS+">' elements!");
			if(!noExit) System.exit(1);
		}
		last_consts = consts;
		return consts;
	}
	
	/**
	 * returns the root element of a XML document
	 * @param dbf
	 * @param xmlFile
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public static Element getRootElement(DocumentBuilderFactory dbf, File xmlFile) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document dom = db.parse(xmlFile);

		return dom.getDocumentElement();
	}
	
	/**
	 * gets a action type based on a string or ends the program if parsing fails
	 * @param actionType
	 * @return
	 */
	private static ActionType getActionType(String actionType) {
		if(actionType != null && actionType.length() > 0) {
			ActionType enumType = ActionType.getType(actionType);
			if(enumType == null) {
				boolean noExit = false;
				if(isGUILoadAttempt() || isNoExit())
					noExit = true;
				LOGGER.error("Notify value '" + actionType + "' is not allowed!");
				if(!noExit) System.exit(1);
			}
			return enumType;
		}
		return null;
	}
	
	/**
	 * returns the modules that should be used for the search for modules
	 * @param dbf
	 * @param xmlFile
	 */
	@SuppressWarnings("unchecked")
	public static HashSet<String> getModules2Load(DocumentBuilderFactory dbf, File xmlFile) {
		if(!XML_MODULE_INFO.containsKey(xmlFile.getAbsolutePath())) {
			getModules2LoadAndCheckID(dbf, xmlFile);
		}
		return (HashSet<String>) XML_MODULE_INFO.get(xmlFile.getAbsolutePath())[0];
	}
	
	/**
	 * returns the modules that should be used for the search for modules
	 * @param dbf
	 * @param xmlFile
	 */
	public static boolean hasAllTasksNumericIDs(DocumentBuilderFactory dbf, File xmlFile) {
		if(!XML_MODULE_INFO.containsKey(xmlFile.getAbsolutePath())) {
			getModules2LoadAndCheckID(dbf, xmlFile);
		}
		return (boolean) XML_MODULE_INFO.get(xmlFile.getAbsolutePath())[1];
	}
	
	private static boolean areAllDependenciesNumeric(DocumentBuilderFactory dbf, File xmlFile) {
		boolean noExit = false;
		if(isGUILoadAttempt() || isNoExit())
			noExit = true;
		try {
			Element docRoot = getRootElement(dbf, xmlFile);
			NodeList dependencies = docRoot.getElementsByTagName(DEPENDS);
			for(int i = 0 ; i < dependencies.getLength(); i++) {
				Node n = dependencies.item(i);
				try { Integer.parseInt(n.getTextContent()); } 
				catch(Exception e) { return false; }
			}
			return true;
		} catch(Exception e) {
			LOGGER.error("Failed to get information about the dependencies.");
			e.printStackTrace();
			if(!noExit) System.exit(1);
		}
		return false;
	}
	
	/**
	 * gets the module folders that should be used for the search for modules
	 * and checks, if all IDs are numeric
	 * @return
	 */
	private static void getModules2LoadAndCheckID(DocumentBuilderFactory dbf, File xmlFile) {
		if(!XML_MODULE_INFO.containsKey(xmlFile.getAbsolutePath())) {
			boolean noExit = false;
			if(isGUILoadAttempt() || isNoExit())
				noExit = true;
			try {
				boolean allID = true;
				HashSet<String> modules = new HashSet<>();
				Element docRoot = getRootElement(dbf, xmlFile);
				NodeList tasks = docRoot.getElementsByTagName(TASKS);
				
				if(tasks.getLength() == 1) {
					Element el;
					NodeList childs = tasks.item(0).getChildNodes();
					for(int i = 0 ; i < childs.getLength(); i++) {
						if(childs.item(i) instanceof Element) {
							el = (Element) childs.item(i);
							// get name of module and add it.
							if(!el.getTagName().equals(XMLParser.SETTINGS)) {
								modules.add(el.getTagName());
							
								if(!el.hasAttribute(ID)) {
									allID = false;
								}
							}
						}
					}
					XML_MODULE_INFO.put(xmlFile.getAbsolutePath(), new Object[]{modules, allID});
				}
				// not valid XML file
				else {
					LOGGER.error("Only one <tasks> tag is allowed and required per XML workflow.");
					if(!noExit) System.exit(1);
				}
			}
			catch(Exception e) {
				LOGGER.error("Failed to get information about the required modules.");
				e.printStackTrace();
				if(!noExit) System.exit(1);
			}
		}
	}
	
	/**
	 * returns a list of modules that must be loaded
	 * @param dbf
	 * @param xmlFile
	 * @param watchdogBase
	 * @return
	 */
	public static ArrayList<String> getModuleFolders(DocumentBuilderFactory dbf, File xmlFile, String watchdogBase) {
		boolean noExit = false;
		if(isGUILoadAttempt() || isNoExit())
			noExit = true;
		try {
			ArrayList<String> dirs = new ArrayList<>();
			Element docRoot = getRootElement(dbf, xmlFile);
			NodeList modules = docRoot.getElementsByTagName(MODULES);
			
			// no module setting is given, use default folder modules/
			if(modules.getLength() == 0) {
				dirs.add(MODULES);
			}
			else if(modules.getLength() == 1) {
				if(modules.item(0) instanceof Element) {
					Element el = (Element) modules.item(0);
					// get default attribute
					if(el.hasAttribute(DEFAULT_FOLDER) && el.getAttribute(DEFAULT_FOLDER).length() > 0)
						dirs.add(el.getAttribute(DEFAULT_FOLDER));
					else
						dirs.add(MODULES);
					
					// get childs
					NodeList allFolders  = el.getElementsByTagName(FOLDER);
					Element elm;
					for(int i = 0 ; i < allFolders.getLength(); i++) {
						if(allFolders.item(i) instanceof Element) {
							elm = (Element) allFolders.item(i);
							dirs.add(elm.getTextContent());
						}
					}
				}
			}
			// not valid XML
			else {
				LOGGER.error("Only one <modules> tag is allowed per XML workflow.");
				if(!noExit) System.exit(1);
			}		
			
			// convert all found folders to absolute path
			HashSet<String> unique = new HashSet<>();
			ArrayList<String> abs = new ArrayList<>(); // we want a array list for constant order!
			for(String f : dirs) {
				if(f.startsWith(File.separator))
					f = new File(f).getAbsolutePath();
				else
					f = new File(watchdogBase + File.separator + f).getAbsolutePath();

				// save it, if not already in
				if(!unique.contains(f)) {
					abs.add(f);
					unique.add(f);
				}
			}
			
			return abs;
		}
		catch(Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
			LOGGER.error("Failed to get the modules setting info.");
			if(!noExit) System.exit(1);
		}
		return null;
	}
	
	/**
	 * gets the return variables of all modules which are included in the schema file and also saves their type
	 * @param dbf
	 * @param includedXSDFiles
	 * @return
	 */
	public static HashMap<String, Pair<HashMap<String, ReturnType>, String>> getReturnInformation(DocumentBuilderFactory dbf, HashSet<String> includedXSDFiles, String watchdogBase) {
		boolean noExit = false;
		if(isGUILoadAttempt() || isNoExit())
			noExit = true;
		try {
			HashMap<String, Pair<HashMap<String, ReturnType>, String>> ret = new HashMap<>();
			String defaultReturnName = getReturnParamName(dbf, new File(watchdogBase + File.separator + ABSTRACT_TASK_PATH));

			// test, which of them are modules
			for(String xsdFile : includedXSDFiles) {
				String taskType = getTaskTypeOfModule(dbf, new File(xsdFile));
				if(taskType == null) {
					LOGGER.error("Module '"+ new File(xsdFile).getAbsolutePath() +"' does not contain a valid task name given via a substitutionGroup.");
					if(!noExit) System.exit(1);
				}
				ret.put(taskType, getReturnTypeOfModule(dbf, new File(xsdFile)));
			}
			// update all the default names, if none are set
			for(String k : new ArrayList<String>(ret.keySet())) {
				Pair<HashMap<String, ReturnType>, String> p = ret.get(k);

				if(p.getValue() == null)
					p = Pair.of(p.getKey(), defaultReturnName);
				// save it, if we have any values for that module
				if(p.getKey().size() > 0)
					ret.put(k, p);
				else
					ret.remove(k);
			}
			return ret;
		}
		catch(Exception e) {
			LOGGER.error("Failed to get return information from xsd definition.");
			e.printStackTrace();
			if(!noExit) System.exit(1);
		}
		return null;
	}
	
	/**
	 * retuns the parameters of all module
	 * @param dbf
	 * @param includedXSDFiles
	 * @param watchdogBase
	 * @return
	 */
	public static HashMap<String, HashMap<String, Parameter>> getParameters(DocumentBuilderFactory dbf, HashSet<String> includedXSDFiles, String xsdRootDir) {
		boolean noExit = false;
		if(isGUILoadAttempt() || isNoExit())
			noExit = true;
		try {
			// get types defined in the schema
			ArrayList<Element> rootTypes = getComplexAndSimpleTypes(dbf, BASE_ROOT_TYPES, xsdRootDir);

			// get the individual types
			HashMap<String, HashMap<String, Parameter>> ret = new HashMap<>();
			for(String xsdFile : includedXSDFiles) {
				String taskType = getTaskTypeOfModule(dbf, new File(xsdFile));
				ret.put(taskType, getParametersOfModule(dbf, new File(xsdFile), rootTypes));
			}
			return ret;
		}
		catch(Exception e) {
			LOGGER.error("Failed to get return information from xsd definition.");
			e.printStackTrace();
			if(!noExit) System.exit(1);
		}
		return null;
	}
	
	/**
	 * checks, if a xsd file of a module contains return information and the name of the parameter the program accepts for the temporary output file
	 * @param dbf
	 * @param schemaFile
	 * @return
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public static Pair<HashMap<String, ReturnType>, String> getReturnTypeOfModule(DocumentBuilderFactory dbf, File schemaFile) throws ParserConfigurationException, SAXException, IOException {
		Element el = null;
		boolean notFound = false;
		HashMap<String, ReturnType> ret = new HashMap<>();
		
		// get all the XSD files, which are included
		Element docRoot = getRootElement(dbf, schemaFile);
		
		// find parameter name for return 
		String returnName = getReturnParamName(dbf, schemaFile);
		
		// get the expected parameters
		NodeList extension = docRoot.getElementsByTagName(X_EXTENSION);
		for(int i = 0 ; i < extension.getLength() && notFound == false; i++) {
			if(extension.item(i) instanceof Element) {
				el = (Element) extension.item(i);
				String base = el.getAttribute(BASE);
				
				if(base.startsWith(EXTENSION_RETURN)) {
					NodeList childs = el.getChildNodes();
					for(int iii = 0; iii < childs.getLength(); iii++) {
						if(childs.item(iii) instanceof Element) {
							el = (Element) childs.item(iii);
							NodeList params = el.getChildNodes();
							// read all the names of the return params and their type
							for(int ii = 0 ; ii < params.getLength(); ii++) {
								if(params.item(ii) instanceof Element) {
									el = (Element) params.item(ii);
									String name = el.getAttribute(NAME);
									String type = el.getAttribute(TYPE);
									if(name != null && type != null) {
										type = type.replace(X, "");
										ReturnType r = getReturnType(type);
										if(r != null)
											ret.put(name, r);
										else {
											LOGGER.error("Invalid return type '"+type+"' in module '"+schemaFile.getAbsolutePath()+"' with name '"+name+"'.");
										}		
									}
								}
							}
							notFound = true;
							break; // we found what we searched for
						}
					}
				}
			}
		}
		return Pair.of(ret, returnName);
	}
	
	/**
	 * cer
	 * @param type
	 * @return
	 */
	public static ReturnType getReturnType(String type) {
		// file type
		if(ReturnType.isFileBaseType(type))
			return new FileReturnType(type);
		if(type.equals(STRING) || type.toLowerCase().equals(PARAM + STRING))
			return new StringReturnType();
		else if(type.equals(DOUBLE) || type.toLowerCase().equals(PARAM + DOUBLE))
			return new DoubleReturnType();
		else if(type.equals(INTEGER) || type.toLowerCase().equals(PARAM + INTEGER))
			return new IntegerReturnType();
		else if(type.equals(BOOLEAN) || type.toLowerCase().equals(PARAM + BOOLEAN))
			return new BooleanReturnType();
		else 
			return null;
	}
	
	/**
	 * tries to find the return name definition a a XSD file or returns null if the definition could not be found
	 * @param dbf
	 * @param schemaFile
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static String getReturnParamName(DocumentBuilderFactory dbf, File schemaFile) throws ParserConfigurationException, SAXException, IOException {
		Element docRoot = getRootElement(dbf, schemaFile);

		// find parameter name for return 
		Element el;
		NodeList restriction = docRoot.getElementsByTagName(X_RESTRICTION);
		for(int i = 0 ; i < restriction.getLength(); i++) {
			if(restriction.item(i) instanceof Element) {
				el = (Element) restriction.item(i);
				String base = el.getAttribute(BASE);
				
				// find the base element
				if(base != null && BASE_ATTRIBUTE_TASK_TYPE.equals(base)) {
					NodeList childs = el.getChildNodes();
					for(int ii = 0; ii < childs.getLength(); ii++) {
						if(childs.item(ii).getNodeName().equals(X_ATTRIBUTE) && ((Element) childs.item(ii)).getAttribute(NAME).equals(RETURN_FILE_PARAMETER)) {
							return ((Element) childs.item(ii)).getAttribute(FIXED);
						}
					}
				}
			}
		}
		
		// search in abstract_task
		NodeList complex = docRoot.getElementsByTagName(X_COMPLEX_TYPE);
		for(int i = 0 ; i < complex.getLength(); i++) {
			if(complex.item(i) instanceof Element) {
				el = (Element) complex.item(i);
				String name = el.getAttribute(NAME);

				// find the base element
				if(name != null && BASE_ATTRIBUTE_TASK_TYPE.equals(name)) {
					NodeList childs = el.getChildNodes();
					for(int ii = 0; ii < childs.getLength(); ii++) {
						if(childs.item(ii).getNodeName().equals(X_ATTRIBUTE) && ((Element) childs.item(ii)).getAttribute(NAME).equals(RETURN_FILE_PARAMETER)) {
							return ((Element) childs.item(ii)).getAttribute(DEFAULT);
						}
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * extracts the task name of a xsd module file
	 * @param dbf
	 * @param schemaFile
	 * @return
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public static String getTaskTypeOfModule(DocumentBuilderFactory dbf, File schemaFile) throws ParserConfigurationException, SAXException, IOException {
		Element docRoot = getRootElement(dbf, schemaFile);
		
		// find parameter name for return 
		Element el;
		NodeList elements = docRoot.getElementsByTagName(X_ELEMENT);
		for(int i = 0 ; i < elements.getLength(); i++) {
			if(elements.item(i) instanceof Element) {
				el = (Element) elements.item(i);
				String base = el.getAttribute(SUBSTITUTION_GROUP);

				// find the base element
				if(base != null && ABSTRACT_TASK.equals(base)) {
					return el.getAttribute(NAME);
				}
			}
		}
		return null;
	}
	
	/**
	 * reads the parameters of the XSD file
	 * @param dbf
	 * @param schemaFile
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static HashMap<String, Parameter> getParametersOfModule(DocumentBuilderFactory dbf, File schemaFile, ArrayList<Element> rootTypes) throws ParserConfigurationException, SAXException, IOException {
		Element docRoot = getRootElement(dbf, schemaFile);
		HashMap<String, Parameter> p = new HashMap<>();
		String parameterDefinitionName = null;
		Element el = null;
		
		NodeList nl = docRoot.getElementsByTagName(X_ELEMENT);	
		for(int i = 0 ; i < nl.getLength(); i++) {
			if(nl.item(i) instanceof Element) {
				el = (Element) nl.item(i);
				String name = el.getAttribute(NAME);

				// find the parameter element
				if(name != null && PARAMETER.equals(name)) {
					parameterDefinitionName = el.getAttribute(TYPE);
					break;
				}
			}
		}

		// parameter element was found
		if(parameterDefinitionName != null) {
			nl = docRoot.getElementsByTagName(X_COMPLEX_TYPE);	
			for(int i = 0 ; i < nl.getLength(); i++) {
				if(nl.item(i) instanceof Element) {
					el = (Element) nl.item(i);
					String name = el.getAttribute(NAME);

					// find the parameter element
					if(name != null && parameterDefinitionName.equals(name)) {
						// run through parameters
						el = (Element) el.getElementsByTagName(X_ALL).item(0);

						nl = el.getElementsByTagName(X_ELEMENT);
						for(int ii = 0 ; ii < nl.getLength(); ii++) {
							if(nl.item(ii) instanceof Element) {
								el = (Element) nl.item(ii);
								String pName = el.getAttribute(NAME);
								ReturnType type = getBaseType(docRoot, el.getAttribute(TYPE), rootTypes);
								
								String minOccursS = el.getAttribute(MIN_OCCURS);
								String maxOccursS = el.getAttribute(MAX_OCCURS);
								Integer minOccurs = 1;
								Integer maxOccurs = 1;
								try { minOccurs = Integer.parseInt(minOccursS); } catch(Exception e) { };
								try { maxOccurs = Integer.parseInt(maxOccursS); } catch(Exception e) { maxOccurs = null; }; // unbounded

								// save the parameter
								p.put(pName, new Parameter(pName, minOccurs, maxOccurs, type));
							}
						}
						break;
					}
				}
			}
		}
		else
			LOGGER.debug("Element with name '"+PARAMETER+"' was not found in '"+schemaFile.toString()+"'. Perhaps the module does not accept any parameters.");
		return p;
	}
	
	/**
	 * retuns all simple and complex types with name attribute in all files
	 * @param dbf
	 * @param files
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	private static ArrayList<Element> getComplexAndSimpleTypes(DocumentBuilderFactory dbf, ArrayList<String> files, String xsdRootDir) throws ParserConfigurationException, SAXException, IOException {
		ArrayList<Element> all = new ArrayList<>();
		for(String f : files) {
			Element docRoot = getRootElement(dbf, new File(xsdRootDir + File.separator + f));
			all.addAll(getComplexAndSimpleTypes(docRoot));
		}
		return all;
	}

	/**
	 * returns simple and complex types with name attribute
	 * @param docRoot
	 * @return
	 */
	private static ArrayList<Element> getComplexAndSimpleTypes(Element docRoot) {
		ArrayList<Element> simpleAndComplexTypes = new ArrayList<>();
		Node n = null;
		NodeList complex = docRoot.getElementsByTagName(X_COMPLEX);
		NodeList simple = docRoot.getElementsByTagName(X_SIMPLE);

		for(int i = 0; i < complex.getLength(); i++) {
			n = complex.item(i);
			if(n != null && n instanceof Element && ((Element) n).hasAttribute(NAME))
				simpleAndComplexTypes.add((Element) n);
		}
		for(int i = 0; i < simple.getLength(); i++) {
			n = complex.item(i);
			if(n != null && n instanceof Element && ((Element) n).hasAttribute(NAME))
				simpleAndComplexTypes.add((Element) n);
		}
		return simpleAndComplexTypes;
	}

	private static ReturnType getBaseType(Element docRoot, String type, ArrayList<Element> rootTypes) {
		String bakType = type;
		ArrayList<Element> both = getComplexAndSimpleTypes(docRoot);
		both.addAll(rootTypes);
		String name = null;
		Node n = null;
		boolean newType = true;
		
		// true to identify base type recursively
		while(XMLParser.getReturnType(type) == null && newType == true && !ReturnType.isFileBaseType(type)) {
			newType = false;
			for(Element e : both) {
				name = e.getAttribute(NAME);
				ArrayList<Element> parent = new ArrayList<>();
				// name was found --> identify base class
				if(type.equals(name)) {
					NodeList nl = e.getChildNodes();
					for(int i = 0; i < nl.getLength(); i++) {
						n = nl.item(i);
						if(n instanceof Element) {
							NodeList rest = ((Element) e).getElementsByTagName(X_RESTRICTION);
							NodeList ext = ((Element) e).getElementsByTagName(X_EXTENSION);
							for(int iii = 0; iii < rest.getLength(); iii++) {
								n = rest.item(iii);
								if(n != null && n instanceof Element && ((Element) n).hasAttribute(BASE))
										parent.add((Element) n);
							}
							for(int iii = 0; iii < ext.getLength(); iii++) {
								n = ext.item(iii);
								if(n != null && n instanceof Element && ((Element) n).hasAttribute(BASE))
									parent.add((Element) n);
							}
						}
					}
					if(parent.size() == 1) {
						type = parent.get(0).getAttribute(BASE);
						newType = true;
						break;
					}
				}
			}
		}
		ReturnType r = XMLParser.getReturnType(type);
		if(r == null)
			LOGGER.debug("Could not determine base type of '" + bakType + "'.");
		return r;
	}
	
	private static String ensureAbsolutePath(String p, boolean isFile) {
		if(p == null)
			return null;
		
		if(!p.matches(".*" + XMLParser.REPLACE_CONST + ".*") && !p.matches(".*" + ReplaceSpecialConstructs.MATCH_TABLE_NAME_PATTERN + ".*"))
			return new File(p).getAbsolutePath() + (!isFile ? File.separator : "");
		return p;
	}
	
	public static String ensureAbsoluteFile(String p) {
		return ensureAbsolutePath(p, true);
	}
	
	public static String ensureAbsoluteFolder(String p) {
		return ensureAbsolutePath(p, false);
	}

	public static HashMap<String, String> getLastReadConstants() {
		return last_consts;
	}
	
	public static boolean testIfUnsafe(String xmlPath) {
		return XMLParser.testIfPattern(xmlPath, XMLParser.IS_NOT_VALID);
	}
	
	public static boolean testIfTemplate(String xmlPath) {
		return XMLParser.testIfPattern(xmlPath, XMLParser.IS_TEMPLATE);
	}
	
	/**
	 * tests, if a xml file contains a specific pattern
	 * @param xmlPath
	 * @param testPattern
	 * @return
	 */
	private static boolean testIfPattern(String xmlPath, Pattern testPattern) {
		try {
			BufferedReader bf = new BufferedReader(new FileReader(xmlPath));
			String line;		
			Matcher m;
			while((line = bf.readLine()) != null) {
				m = testPattern.matcher(line);
				if(m.matches()) {
					bf.close();
					return true;
				}
			}
			bf.close();
		}
		catch(Exception e) {}
		return false;
	}
}
