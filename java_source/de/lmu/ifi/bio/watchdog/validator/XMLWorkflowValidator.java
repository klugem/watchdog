package de.lmu.ifi.bio.watchdog.validator;

import java.io.File;

import de.lmu.ifi.bio.watchdog.helper.LogMessageEvent;
import de.lmu.ifi.bio.watchdog.helper.PatternFilenameFilter;
import de.lmu.ifi.bio.watchdog.interfaces.BasicEventHandler;
import de.lmu.ifi.bio.watchdog.runner.XMLBasedWatchdogRunner;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Checks, if a XML workfow is valid
 * @author kluge
 *
 */
public class XMLWorkflowValidator extends Validator {

	private static final long serialVersionUID = 3234618059359092829L;
	private static final String NAME = "XMLValidatorChecker";
	private final File FOLDER;
	
	public XMLWorkflowValidator(String folderPath, File watchdogBase, BasicEventHandler<LogMessageEvent> eh) {
		super(NAME, watchdogBase, null);
		this.FOLDER = new File(folderPath);
	}

	@Override
	public boolean validate() {
		if(!this.FOLDER.exists()) {
			this.error("Folder '" + this.FOLDER.getAbsolutePath() + "' does not exist.");
			return false;
		}
		if(!this.FOLDER.isDirectory()) {
			this.error("Path to '" + this.FOLDER.getAbsolutePath() + "' is not a directory.");
			return false;
		}
		if(!this.FOLDER.canRead()) {
			this.error("Unable to read from '" + this.FOLDER.getAbsolutePath() + "'.");
			return false;
		}
		boolean ok = false;
		String xsdSchema = this.getWatchdogBaseDir().getAbsolutePath() +  File.separator + XMLParser.FILE_CHECK;
		int val = 0;
		for(File xmlFile : this.FOLDER.listFiles(new PatternFilenameFilter(XMLBasedWatchdogRunner.XML_PATTERN, false))) {
			val++;
			String xmlFilename = xmlFile.getAbsolutePath();
			this.info("Validating '" + xmlFilename + "'...");
			try {
				XMLParser.parse(xmlFilename, xsdSchema, null, 0, false, true, true, false, false, true);
				ok = true;
			}
			catch(Exception e) {
				this.error(e.getMessage());
				return false;
			}
		}
		
		// test if one XML file was validated
		if(ok && val == 1) {
			this.info("Workflow syntax is valid!");
			return true;
		}
		else {
			if(val == 0)
				this.error("No XML workflow file was found in '" + this.FOLDER.getAbsolutePath() + "'.");
			else
				this.error("More than one XML workflow file was found in '" + this.FOLDER.getAbsolutePath() + "'.");
			return false;
		}
	}
}
