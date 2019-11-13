package de.lmu.ifi.bio.watchdog.docu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.lmu.ifi.bio.watchdog.helper.PatternFilenameFilter;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

public class DocuXMLParser {
	private static final String DOCUMENT_XSD = XMLParser.XSD + File.separator + "documentation.xsd";
	private static final String DOC_PREFIX = "<documentation ";
	public static final String REPLACE_SUFFIX = "Task$";

	// section elements
	public static final String MAINTAINER = "maintainer";
	public static final String PARAMETER = "parameter";
	public static final String INFO = "info";
	public static final String RETURN = "return";
	
	// elements
	public static final String AUTO_ID = "iID"; // internal, not stable id
	public static final String AUTHOR = "author";
	public static final String UPDATED = "updated";
	public static final String CATEGORY = "category";
	public static final String WEBSITE = "website";
	public static final String PMID = "PMID";
	public static final String PAPER_DESC = "paperDescription";
	public static final String DESCRIPTION = "description";
	public static final String DESCRIPTION_SEARCH = "description_search";
	public static final String DEPENDENCIES = "dependencies";
	public static final String COMMENTS = "comments";
	public static final String NAME = "name";
	public static final String USERNAME = "username";
	public static final String TYPE = "type";
	public static final String RESTRICTIONS = "restrictions";
	public static final String DEFAULT = "default";
	public static final String PARAM = "param";
	public static final String VAR = "var";
	public static final String MIN_OCCURS = "minOccurs";
	public static final String MAX_OCCURS = "maxOccurs";
	public static final String MIN_VERSION = "minVersion";
	public static final String MAX_VERSION = "maxVersion";
	
	/**
	 * Writes a finished docu to disk
	 * @param dbf
	 * @param m
	 * @param outputFile
	 * @throws TransformerException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static void writeDocuToDisk(DocumentBuilderFactory dbf, Moduledocu m, File outputFile, boolean isTemplate) throws TransformerException, ParserConfigurationException, SAXException, IOException {
		Element root = XMLParser.getRootElement(dbf, m.toXML(isTemplate));
		XMLParser.writePrettyXML(root, outputFile);
	}

	/**
	 * prepares the document builder factory for parsing of module docu files
	 * @param watchdogBase
	 * @return
	 */
	public static DocumentBuilderFactory prepareDBF(String watchdogBase) {
		try {
			// init the document builder
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setIgnoringElementContentWhitespace(true);
			dbf.setNamespaceAware(true);
			
			// load the schema in XSD 1.1 format
			File includedSchemaPath = new File(watchdogBase + File.separator + DOCUMENT_XSD);
			SchemaFactory schemaFac = SchemaFactory.newInstance(XMLParser.XML_1_1);
			Schema schema = schemaFac.newSchema(includedSchemaPath);
			dbf.setSchema(schema);
			return dbf;
		}
		catch(Exception e) {
			e.printStackTrace();
			System.out.println("Failed to create instance of DocumentBuilderFactory.");
			System.exit(1);
		}
		return null;
	}
	
	/**
	 * finds all modules that contain a XML file that is might be valid according to the documentation XSD schema 
	 * @param dbf
	 * @param moduleFolders
	 * @oaram isModuleBaseFolder
	 * @return
	 */
	public static ArrayList<Pair<File, File>> findAllDocumentedModules(String watchdogBase, ArrayList<String> moduleFolders, boolean isModuleBaseFolder) {
		DocumentBuilderFactory dbf = prepareDBF(watchdogBase);
		HashMap<String, String> modules = XMLParser.findModules(dbf, moduleFolders, isModuleBaseFolder);
		ArrayList<Pair<File, File>> xmlDocFiles = new ArrayList<>();
		for(String xsdFileString : modules.values()) {
			File xsd = new File(xsdFileString);
			File modFolder = xsd.getParentFile();
			PatternFilenameFilter patternMatcher = new PatternFilenameFilter("*.xml$", true);
			// run through all file
			for(File f : modFolder.listFiles(patternMatcher)) {
				if(f.canRead() && f.isFile()) {
					// test if it is a documentation file - kind of dirty
					try { 
						List<String> lines = Files.readAllLines(Paths.get(f.getAbsolutePath())); 
						if((lines.size() > 0 && lines.get(0).startsWith(DOC_PREFIX)) || (lines.size() > 1 && lines.get(1).startsWith(DOC_PREFIX))) {
							// locate XSD file in that folder
							xmlDocFiles.add(Pair.of(f, xsd));
						}
					}
					catch(IOException e) {
						System.out.println("Failed to read possible module docu file '"+f.getAbsolutePath()+"'.");
					}
				}
			}
		}
		return xmlDocFiles;
	}
	
	/**
	 * parses all the XML modules files
	 * @param watchdogBase
	 * @param xmlDocuFiles
	 * @return
	 */
	public static ArrayList<Moduledocu> parseAllXMLFiles(String watchdogBase, ArrayList<String> moduleFolders) {
		try {
			DocumentBuilderFactory dbf = prepareDBF(watchdogBase);
			
			ArrayList<Pair<File, File>> xmlDocuFiles = findAllDocumentedModules(watchdogBase, moduleFolders, false);
			ArrayList<Moduledocu> md = new ArrayList<>();
			for(Pair<File, File> x : xmlDocuFiles)
				md.add(parseXMLFile(dbf, x.getLeft(), x.getRight(), false)); 
			
			return md;
		}
		catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	
	/**
	 * parses one xsd module file
	 * @param db
	 * @param xmlDocuFile
	 * @return
	 */
	public static Moduledocu parseXMLFile(DocumentBuilderFactory dbf, File xmlDocuFile, File xsdFile, boolean noExit) {
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse(xmlDocuFile);

			// get data from XSD module file
			String name = XMLParser.getTaskTypeOfModule(dbf, xsdFile).replaceFirst(REPLACE_SUFFIX, "");
			HashSet<String> includedXSDFiles = new HashSet<>();
			includedXSDFiles.add(xsdFile.getAbsolutePath());
			HashSet<Integer> versions = XMLParser.getVersionsOfModule(dbf, xsdFile);
			int minV = versions.stream().min(Comparator.comparing(Integer::valueOf)).orElse(1).intValue();
			int maxV = versions.stream().max(Comparator.comparing(Integer::valueOf)).orElse(1).intValue();
			
			// create structures that will hold the data
			ArrayList<String> authors = null;
			ArrayList<String> pmid = null;
			ArrayList<String> user = new ArrayList<>();
			ArrayList<String> website = new ArrayList<>();
			ArrayList<Paramdocu> params = new ArrayList<>();
			ArrayList<Returndocu> returnValues = new ArrayList<>();
			ArrayList<String>  categories = null;
			String paperDesc = null;
			ArrayList<VersionedInfo<String>> dependencies = null;
			ArrayList<VersionedInfo<String>> comments = null;
			ArrayList<VersionedInfo<String>> description = null;
			String updated = null;
			String github = null;
			
			// get section parent elements
			NodeList infoList = dom.getElementsByTagName(INFO);
			NodeList maintainerList = dom.getElementsByTagName(MAINTAINER);
			NodeList parameterList = dom.getElementsByTagName(PARAMETER);
			NodeList returnList = dom.getElementsByTagName(RETURN);
			
			// if XSD is valid we need no further checks here
			Element info = (Element) infoList.item(0);
			Element maintainer = (Element) maintainerList.item(0);
			Element parameter = (Element) parameterList.item(0);
			Element returnV = (Element) returnList.item(0);
		
			// fill info
			authors = new NodeListIterator(info.getElementsByTagName(AUTHOR)).stream().map(e -> e.getTextContent()).collect(Collectors.toCollection(ArrayList::new));
			pmid = new NodeListIterator(info.getElementsByTagName(PMID)).stream().map(e -> e.getTextContent()).collect(Collectors.toCollection(ArrayList::new));
			website = new NodeListIterator(info.getElementsByTagName(WEBSITE)).stream().map(e -> e.getTextContent()).collect(Collectors.toCollection(ArrayList::new));
			paperDesc = new NodeListIterator(info.getElementsByTagName(PAPER_DESC)).stream().map(e -> e.getTextContent()).collect(Collectors.joining(""));
			dependencies = new NodeListIterator(info.getElementsByTagName(DEPENDENCIES)).stream().map(e -> getVersioned(e, minV, maxV)).collect(Collectors.toCollection(ArrayList::new));
			comments = new NodeListIterator(info.getElementsByTagName(COMMENTS)).stream().map(e -> getVersioned(e, minV, maxV)).collect(Collectors.toCollection(ArrayList::new));
			description =  new NodeListIterator(info.getElementsByTagName(DESCRIPTION)).stream().map(e -> getVersioned(e, minV, maxV)).collect(Collectors.toCollection(ArrayList::new));
			updated = new NodeListIterator(info.getElementsByTagName(UPDATED)).stream().map(e -> e.getTextContent()).collect(Collectors.joining(""));
			github = new NodeListIterator(info.getElementsByTagName(MAINTAINER)).stream().map(e -> e.getTextContent()).collect(Collectors.joining(""));
			categories = new NodeListIterator(info.getElementsByTagName(CATEGORY)).stream().map(e -> e.getTextContent()).collect(Collectors.toCollection(ArrayList::new));

			// fill maintainer
			if(maintainer != null)
				user = new NodeListIterator(maintainer.getElementsByTagName(USERNAME)).stream().map(e -> e.getTextContent()).collect(Collectors.toCollection(ArrayList::new));

			// fill parameter and return values
			if(parameter != null)
				params = new NodeListIterator(parameter.getElementsByTagName(PARAM)).stream().map(e -> parseParam(e)).collect(Collectors.toCollection(ArrayList::new));
			if(returnV != null)
				returnValues = new NodeListIterator(returnV.getElementsByTagName(VAR)).stream().map(e -> parseReturnValue(e)).collect(Collectors.toCollection(ArrayList::new));

			// create the object
			Moduledocu m = new Moduledocu(name, categories, updated, authors, pmid, website, paperDesc, dependencies, comments, description, versions, params, returnValues, user, github);
			return m;
		}
		catch(Exception e) {
			e.printStackTrace();
			if(!noExit)
				System.exit(1);
		}
		return null;
	}

	private static VersionedInfo<String> getVersioned(Element e, int minV, int maxV) {
		int minVersion = 1;
		int maxVersion = 1;
		try { minVersion = Integer.parseInt(e.getAttribute(MIN_VERSION)); minV = minVersion;} catch(Exception ex) {}
		try { maxVersion = Integer.parseInt(e.getAttribute(MAX_VERSION)); maxV = maxVersion;} catch(Exception ex) {}		
		return new VersionedInfo<>(StringEscapeUtils.escapeHtml4(e.getTextContent()), minV, maxV);
	}

	/**
	 * parses a param docu element
	 * @param e
	 * @return
	 */
	private static Paramdocu parseParam(Element e) {
		String name = e.getAttribute(NAME);
		String type = e.getAttribute(TYPE);
		String valueRestrictions = e.getAttribute(RESTRICTIONS);
		String defaultValue = e.getAttribute(DEFAULT);
		String description = new NodeListIterator(e.getElementsByTagName(DESCRIPTION)).stream().map(x -> x.getTextContent()).collect(Collectors.joining(""));
		int minVersion = 0;
		int maxVersion = 0;
		Integer minOccurs = -1;
		Integer maxOccurs = -1;
		
		try { minVersion = Integer.parseInt(e.getAttribute(MIN_VERSION)); } catch(Exception ex) {}
		try { maxVersion = Integer.parseInt(e.getAttribute(MAX_VERSION)); } catch(Exception ex) {}
		try { minOccurs = Integer.parseInt(e.getAttribute(MIN_OCCURS)); } catch(Exception ex) {}
		try { maxOccurs = Integer.parseInt(e.getAttribute(MAX_OCCURS)); } catch(Exception ex) { maxOccurs = null; } // unbounded

		Paramdocu p = new Paramdocu(name, type, description, defaultValue, valueRestrictions);
		if(minVersion != 0 || maxVersion != 0)
			p.setVersions(minVersion, maxVersion);
		if(minOccurs != -1 || maxOccurs == null || maxOccurs != -1)
			p.setOccurs(minOccurs, maxOccurs);
		return p;
	}
	
	/**
	 * parses a return value docu element
	 * @param e
	 * @return
	 */
	private static Returndocu parseReturnValue(Element e) {
		String name = e.getAttribute(NAME);
		String type = e.getAttribute(TYPE);
		String description = new NodeListIterator(e.getElementsByTagName(DESCRIPTION)).stream().map(x -> x.getTextContent()).collect(Collectors.joining(""));
		int minVersion = 0;
		int maxVersion = 0;
		try { minVersion = Integer.parseInt(e.getAttribute(MIN_VERSION)); } catch(Exception ex) {}
		try { maxVersion = Integer.parseInt(e.getAttribute(MAX_VERSION)); } catch(Exception ex) {}

		Returndocu r= new Returndocu(name, type, description);
		
		if(minVersion != 0 || maxVersion != 0)
			r.setVersions(minVersion, maxVersion);
		return r;
	}
}
