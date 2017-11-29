package de.lmu.ifi.bio.watchdog.xmlParser.plugins.processBlockParser;

import java.io.File;
import java.util.HashMap;

import org.w3c.dom.Element;

import de.lmu.ifi.bio.watchdog.GUI.properties.views.processblocks.FolderGUIProcessBlockView;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.processblocks.ProcessBlockPropertyViewController;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessFolder;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

public class ProcessFolderParser extends XMLProcessBlockParser<ProcessFolder> {
	private static final String XSD_DEF = "plugins" + File.separator + "processblock.folder.xsd";
	
	static {
		// register the processblock plugins shipped with watchdog on GUI
		ProcessBlockPropertyViewController.registerWatchdogPluginOnGUI(ProcessFolder.class, FolderGUIProcessBlockView.class);
	}
	
	public ProcessFolderParser(Logger l) {
		super(l);
	}

	@Override
	public String getNameOfParseableTag() {
		return XMLParser.PROCESS_FOLDER;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ProcessFolder parseElement(Element el, String watchdogBaseDir, Object[] additionalData) {
		String baseName = (String) additionalData[0];
		HashMap<String, String> consts = (HashMap<String, String>) additionalData[1];
		boolean validationMode = (boolean) additionalData[2];
		Integer maxDepthBaseFolder = (Integer) additionalData[3];
		
		// get attributes
		File path = null;
		String name = XMLParser.getAttribute(el, XMLParser.NAME);
		String folder = XMLParser.getAttribute(el, XMLParser.FOLDER);
		String pattern = XMLParser.getAttribute(el, XMLParser.PATTERN);
		String ignore = XMLParser.getAttribute(el, XMLParser.IGNORE);
		boolean disableExistenceCheck = Boolean.parseBoolean( XMLParser.getAttribute(el, XMLParser.DISABLE_EXISTANCE_CHECK));
		boolean append = Boolean.parseBoolean(XMLParser.getAttribute(el, XMLParser.APPEND));
		Integer maxDepth = Integer.parseInt(XMLParser.getAttribute(el, XMLParser.MAX_DEPTH));
		String color = XMLParser.getAttribute(el, XMLParser.COLOR);

		// test which maxDepth value to set
		if(maxDepthBaseFolder != null) {
			// no explicit value was set for the process folder --> set maxDepth of base folder
			if(maxDepth == -1)
				maxDepth = maxDepthBaseFolder;
		}
		// ensure that no negative values are allowed
		maxDepth = Math.max(0, maxDepth);

		if(!this.GUI_load_attempt) {
			// replace base name if there is a constant.
			baseName = XMLParser.replaceConstants(baseName, consts);
			// test, if path begins relative
			if(baseName != null && (folder.startsWith(File.separator) || folder.matches("^[A-Z]:\\\\.*"))) {
				LOGGER.error("A processFolder within a baseFolder can not start with '"+File.separator+"' or '[A-Z]:\\' because it must be a relative path ('" + folder + "')!");
				if(!this.no_exit) System.exit(1);
			}
			else if(baseName == null && !(folder.startsWith(File.separator) || folder.matches("^[A-Z]:\\\\.*"))) {
				LOGGER.error("A processFolder outside a baseFolder must start with '"+File.separator+"' or '[A-Z]:\\' because it must be a absolute path ('" + folder + "')!");
				if(!this.no_exit) System.exit(1);	
			}
		
			// set the path to the folder
			if(baseName != null)
				path = new File(baseName + File.separator + folder + File.separator);
			else
				path = new File(folder + File.separator);
		}
		
		// test, if folder exists
		if(!this.GUI_load_attempt) {
			if(!(path.exists() && path.isDirectory() && path.canRead() && path.canExecute())) {
				if(!disableExistenceCheck) {
					if(!validationMode)
						LOGGER.error("ProcessFolder with path '" + path.getAbsolutePath() + "' was not found!");
					else
						LOGGER.warn("ProcessFolder with path '" + path.getAbsolutePath() + "' was not found. This folder must be created before the workflow is executed or the existence check must be disabled.");
					if(!this.no_exit) System.exit(1);
				}
				// create the folder
				else
					path.mkdirs();
			}
		}
		ProcessFolder f = null;
		if(this.GUI_load_attempt) {
			f = new ProcessFolder(name, folder, baseName, pattern, ignore, maxDepth, append, disableExistenceCheck);
			f.setColor(color);
			this.PARSED_INFO.put(name + XMLParser.SUFFIX_SEP + this.PARSED_INFO.size(), f);
		}
		else {
			// check, if a process block with that name is already there
			if(this.PARSED_INFO.containsKey(name)) {
				if(!append) {
					LOGGER.error("ProcessFolder with name '" + name + "' was already defined before and append attribute is set to false!");
					if(!this.no_exit) System.exit(1);
				}
				// try to append it
				else if(this.PARSED_INFO.containsKey(name)) {
					ProcessFolder updateBlock = this.PARSED_INFO.get(name);
					updateBlock.append(path, pattern, ignore, maxDepth);
				}
				else {
					LOGGER.error("ProcessBlock with name '" + name + "' was already defined before and is not of type ProcessFolder!");
					if(!this.no_exit) System.exit(1);
				}
			}
			// all ok, store it!
			else {
				f = new ProcessFolder(name, path, pattern, ignore, maxDepth);
				f.setColor(color);
			}
		}
		return f;
	}

	@Override
	public String getXSDDefinition() {
		return XSD_DEF;
	}

	@Override
	public void runAdditionalTestsOnElement(String name) {}
}
