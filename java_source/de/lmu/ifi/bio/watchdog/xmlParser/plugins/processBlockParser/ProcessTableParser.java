package de.lmu.ifi.bio.watchdog.xmlParser.plugins.processBlockParser;

import java.io.File;
import java.util.HashMap;

import org.w3c.dom.Element;

import de.lmu.ifi.bio.watchdog.GUI.properties.views.processblocks.ProcessBlockPropertyViewController;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.processblocks.TableGUIProcessBlockView;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessTable;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

public class ProcessTableParser extends XMLProcessBlockParser<ProcessTable> {
	private static final String XSD_DEF = "plugins" + File.separator + "processblock.table.xsd";
	
	static {
		// register the processblock plugins shipped with watchdog on GUI
		ProcessBlockPropertyViewController.registerWatchdogPluginOnGUI(ProcessTable.class, TableGUIProcessBlockView.class);
	}
	
	public ProcessTableParser(Logger l) {
		super(l);
	}

	@Override
	public String getNameOfParseableTag() {
		return XMLParser.PROCESS_TABLE;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ProcessTable parseElement(Element el, String watchdogBaseDir, Object[] additionalData) {
		File path = null;
		
		String baseName = (String) additionalData[0];
		HashMap<String, String> consts = (HashMap<String, String>) additionalData[1];
		boolean validationMode = (boolean) additionalData[2];
		
		// get the atrributes
		String name = XMLParser.getAttribute(el, XMLParser.NAME);
		String table = XMLParser.getAttribute(el, XMLParser.TABLE);
		String compareName = XMLParser.getAttribute(el, XMLParser.COMPARE_NAME);
		boolean disableExistenceCheck = Boolean.parseBoolean( XMLParser.getAttribute(el, XMLParser.DISABLE_EXISTANCE_CHECK));
		String color = XMLParser.getAttribute(el, XMLParser.COLOR);
		
		ProcessTable t = null;
		// test, if path begins relative
		if(!XMLParser.isGUILoadAttempt()) {
			// replace base name if there is a constant.
			baseName = XMLParser.replaceConstants(baseName, consts);
			if(baseName != null && table.startsWith(File.separator)) {
				LOGGER.error("A processTable within a baseFolder can not start with '"+File.separator+"' because it must be a relative path ('" + table + "')!");
				if(!this.no_exit) System.exit(1);
			}
			else if(baseName == null && !table.startsWith(File.separator)) {
				LOGGER.error("A processTable outside a baseFolder must start with '"+File.separator+"' because it must be a absolute path ('" + table + "')!");
				if(!this.no_exit) System.exit(1);	
			}
			// set the path to the folder
			if(baseName != null)
				path = new File(baseName + File.separator + table + File.separator);
			else
				path = new File(table + File.separator);
		}
					
		// test, if file exists
		if(!XMLParser.isGUILoadAttempt() && !disableExistenceCheck && !(path.exists() && path.isFile() && path.canRead())) {
			if(!validationMode)
				LOGGER.error("File for processTable with path '" + path.getAbsolutePath() + "' was not found!");
			else
				LOGGER.warn("File for processTable with path '" + path.getAbsolutePath() + "' was not found during validation. This file must be created before the workflow is executed or the existence check must be disabled.");
			if(!this.no_exit) System.exit(1);
		}
		
		// check, if a process block with that name is already there
		if(this.PARSED_INFO.containsKey(name)) {
			LOGGER.error("A processBlock with name '" + name + "' was already defined before!");
			if(!this.no_exit) System.exit(1);
		}
		// all ok, store it!
		else {
			if(XMLParser.isGUILoadAttempt())
				t = new ProcessTable(name, table, baseName, compareName, disableExistenceCheck);
			else
				t = new ProcessTable(name, path, compareName, disableExistenceCheck);
			t.setColor(color);
		}
		return t;
	}

	@Override
	public String getXSDDefinition() {
		return XSD_DEF;
	}

	@Override
	public void runAdditionalTestsOnElement(String name) {}
}
