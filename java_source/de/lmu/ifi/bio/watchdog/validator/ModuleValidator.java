package de.lmu.ifi.bio.watchdog.validator;

import java.io.File;
import java.util.ArrayList;

import de.lmu.ifi.bio.watchdog.helper.LogMessageEvent;
import de.lmu.ifi.bio.watchdog.helper.PatternFilenameFilter;
import de.lmu.ifi.bio.watchdog.interfaces.BasicEventHandler;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Validators that validates different properties of modules
 * @author kluge
 *
 */
public abstract class ModuleValidator extends Validator {

	private static final long serialVersionUID = -1849130147418180446L;
	private final String MODULE_PATH;
	
	public ModuleValidator(String name, String modulePath, File watchdogBase, BasicEventHandler<LogMessageEvent> eh) {
		super(name, watchdogBase, eh);
		this.MODULE_PATH = modulePath;
	}

	/**
	 * path to the module folder that should be validated
	 * @return
	 */
	public String getModulePath() {
		return this.MODULE_PATH;
	}
	
	/**
	 * path to all XSD files in the given folder or
	 * @param searchInFirstSubFolderLevel
	 * @return
	 */
	public ArrayList<File> identifyXSDFiles(boolean searchInFirstSubFolderLevel) {
		ArrayList<File> xsd = new ArrayList<>();
		File dir = new File(this.getModulePath());
		
		// test if folder can be read
		if(dir.isDirectory() && dir.canRead()) {
			ArrayList<File> folders = new ArrayList<>();
			folders.add(dir);
			// add first sub-folder level
			if(searchInFirstSubFolderLevel) {
				for(File mmf : dir.listFiles()) {
					if(mmf.isDirectory() && mmf.canRead())
						folders.add(mmf);
				}
			}
			// look for XSD files
			for(File mmf : folders) {
				File[] res = mmf.listFiles(new PatternFilenameFilter(XMLParser.XSD_PATTERN, false));
				if(res != null) {
					for(File xsdmmf : res) {
						if(xsdmmf.canRead())
							xsd.add(xsdmmf);
					}
				}
			}
		}
		else {
			this.error("Can not read module folder '"+dir+"'!");
		}
		if(xsd.size() == 0) {
			this.warn("Failed to load any module from folder '"+dir.getAbsolutePath()+"'.");
		}
		return xsd;
	}
}
