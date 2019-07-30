package de.lmu.ifi.bio.watchdog.validator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import de.lmu.ifi.bio.watchdog.helper.LogMessageEvent;
import de.lmu.ifi.bio.watchdog.interfaces.BasicEventHandler;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Validates syntax of the XSD file of a module
 * @author kluge
 *
 */
public class XSDModuleValidator extends ModuleValidator {

	private static final long serialVersionUID = -2849130147418164441L;
	private static final String NAME = "XSDModuleValidator";
	private Document xsdModule;
	
	public XSDModuleValidator(String modulePath, File watchdogBase, BasicEventHandler<LogMessageEvent> eh) {
		super(NAME, modulePath, watchdogBase, null);
	}

	@Override
	public boolean validate() {
		File current = null;
		try {
			int tested = 0;
			int validXSDTypes = 0;
			// create a new document factory for documents without schema...
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setIgnoringElementContentWhitespace(true);
			dbf.setNamespaceAware(true);
			
			// ... with schema
			/*DocumentBuilderFactory dbfSchema = DocumentBuilderFactory.newInstance();
			dbfSchema.setIgnoringElementContentWhitespace(true);
			dbfSchema.setNamespaceAware(true);
			SchemaFactory schemaFac = SchemaFactory.newInstance(XMLParser.XML_1_1);
			Schema schema = schemaFac.newSchema(new File(this.getWatchdogBaseDir() + File.separator + XMLParser.ABSTRACT_TASK_PATH));
			dbfSchema.setSchema(schema);*/
			
			// find the XSD module file
			ArrayList<File> candidates = this.identifyXSDFiles(false);
			
			for(File xsdFile : candidates) {
				current = xsdFile;
				String taskType = XMLParser.getTaskTypeOfModule(dbf, xsdFile);
				if(taskType == null) {
					continue;
				}
				validXSDTypes++;
				HashSet<Integer> versions = XMLParser.getVersionsOfModule(dbf, xsdFile);
				for(int v : versions) {
					// ensure that a cached version of the module is there 
					String fileToLoad = XMLParser.getCacheFileNameForXSDModule(this.getWatchdogBaseDir() + File.separator + XMLParser.TMP_FOLDER, xsdFile, taskType, v);
					File fileToLoadFile = new File(fileToLoad);
					
					// create the file, if it does not exist
					if(!fileToLoadFile.exists())
						XMLParser.createdCachedXSDModuleVersion(dbf, xsdFile, fileToLoadFile, v, true);
	
					// validate the resulting file
					DocumentBuilder db = dbf.newDocumentBuilder();
					this.xsdModule = db.parse(fileToLoadFile);
	
					// if there is no error --> it is valid
					tested++;
				}
			}
			// only one XSD per folder is allowed
			if(validXSDTypes == 0) {
				LOGGER.error("Found no XSD module file in '"+ this.getModulePath() +"'.");
				return false;
			} else if(validXSDTypes > 1) {
				LOGGER.error("Found more than one XSD module file in '"+ this.getModulePath() +"'.");
				return false;
			}
			if(tested > 0) {
				LOGGER.info("Validation of XSD file '"+ current.getAbsolutePath() +"' succeeded.");
				return true;
			}
		}
		catch(Exception e) {
			if(current != null)
				LOGGER.error("Exception during validation of '"+ current.getAbsolutePath() +"'.");
			else
				LOGGER.error("Exception during validation of '"+ this.getModulePath() +"'.");
			
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * checks, if an XSD module info is associated with that validator
	 * @return
	 */
	public boolean hasXSDModuleInfo() {
		return this.xsdModule != null;
	}
	
	/**
	 * returns the modules XSD Document after validation succeeded
	 * @return
	 */
	public Document getXSDModuleInfo() {
		return this.xsdModule;
	}
}
