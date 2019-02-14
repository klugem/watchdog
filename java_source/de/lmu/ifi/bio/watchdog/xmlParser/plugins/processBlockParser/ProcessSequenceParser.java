package de.lmu.ifi.bio.watchdog.xmlParser.plugins.processBlockParser;

import java.io.File;

import org.w3c.dom.Element;

import de.lmu.ifi.bio.watchdog.GUI.properties.views.processblocks.ProcessBlockPropertyViewController;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.processblocks.SequenceGUIProcessBlockView;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessSequence;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

public class ProcessSequenceParser extends XMLProcessBlockParser<ProcessSequence> {
	private static final String XSD_DEF = "plugins" + File.separator + "processblock.sequence.xsd";
	
	static {
		// register the processblock plugins shipped with watchdog on GUI
		if(Functions.hasJavaFXInstalled()) {
			ProcessBlockPropertyViewController.registerWatchdogPluginOnGUI(ProcessSequence.class, SequenceGUIProcessBlockView.class);
		}
	}
	
	public ProcessSequenceParser(Logger l) {
		super(l);
	}

	@Override
	public String getNameOfParseableTag() {
		return XMLParser.PROCESS_SEQUENCE;
	}

	@Override
	public ProcessSequence parseElement(Element el, String watchdogBaseDir, Object[] additionalData) {
		// get the atrributes
		String name = XMLParser.getAttribute(el, XMLParser.NAME);
		double start = Double.parseDouble(XMLParser.getAttribute(el, XMLParser.START));
		double end = Double.parseDouble(XMLParser.getAttribute(el, XMLParser.END));
		double step = Double.parseDouble(XMLParser.getAttribute(el, XMLParser.STEP));
		boolean append = Boolean.parseBoolean(XMLParser.getAttribute(el, XMLParser.APPEND));
		String color = XMLParser.getAttribute(el, XMLParser.COLOR);
		
		ProcessSequence s = null;
		if(this.GUI_load_attempt) {
			s = new ProcessSequence(name, start, end, step, append);
			s.setColor(color);
			this.PARSED_INFO.put(name + XMLParser.SUFFIX_SEP + this.PARSED_INFO.size(), s);
		}
		else {								
			// check, if a process block with that name is already there
			if(this.PARSED_INFO.containsKey(name)) {
				if(!append) {
					LOGGER.error("ProcessBlock with name '" + name + "' was already defined before and append attribute is set to false!");
					if(!this.no_exit) System.exit(1);
				}
				// try to append it
				else if(this.PARSED_INFO.containsKey(name)) {
					ProcessSequence updateBlock = this.PARSED_INFO.get(name);
					updateBlock.append(start, end, step);
				}
			}
			else {
				s = new ProcessSequence(name, start, end, step);
				s.setColor(color);
			}
		}
		return s;
	}

	@Override
	public String getXSDDefinition() {
		return XSD_DEF;
	}

	@Override
	public void runAdditionalTestsOnElement(String name) {}
}
