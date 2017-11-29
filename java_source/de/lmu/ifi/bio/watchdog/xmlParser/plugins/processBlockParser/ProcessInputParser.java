package de.lmu.ifi.bio.watchdog.xmlParser.plugins.processBlockParser;

import java.io.File;

import org.w3c.dom.Element;

import de.lmu.ifi.bio.watchdog.GUI.properties.views.processblocks.InputGUIProcessBlockView;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.processblocks.ProcessBlockPropertyViewController;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessInput;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

public class ProcessInputParser extends XMLProcessBlockParser<ProcessInput> {
	private static final String XSD_DEF = "plugins" + File.separator + "processblock.input.xsd";
	
	static {
		// register the processblock plugins shipped with watchdog on GUI
		ProcessBlockPropertyViewController.registerWatchdogPluginOnGUI(ProcessInput.class, InputGUIProcessBlockView.class);
	}
	
	public ProcessInputParser(Logger l) {
		super(l);
	}

	@Override
	public String getNameOfParseableTag() {
		return XMLParser.PROCESS_INPUT;
	}

	@Override
	public ProcessInput parseElement(Element el, String watchdogBaseDir, Object[] additionalData) {
		// get the atrributes
		String name = XMLParser.getAttribute(el, XMLParser.NAME);
		String sep = XMLParser.getAttribute(el, XMLParser.SEP);
		String compareName = XMLParser.getAttribute(el, XMLParser.COMPARE_NAME);
		String color = XMLParser.getAttribute(el, XMLParser.COLOR);
		
		// check, if a process block with that name is already there
		if(this.PARSED_INFO.containsKey(name)) {
				LOGGER.error("ProcessSequence with name '" + name + "' was already defined before and processInput blocks can not be joined with others!");
				if(!this.no_exit) System.exit(1);
		}
		else {
			ProcessInput i = new ProcessInput(name, sep, compareName);
			i.setColor(color);
			return i;
		}
		return null;
	}

	@Override
	public String getXSDDefinition() {
		return XSD_DEF;
	}

	@Override
	public void runAdditionalTestsOnElement(String name) {}
}
