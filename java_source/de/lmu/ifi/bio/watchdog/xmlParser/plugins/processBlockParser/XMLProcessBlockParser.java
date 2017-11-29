package de.lmu.ifi.bio.watchdog.xmlParser.plugins.processBlockParser;

import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessBlock;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import de.lmu.ifi.bio.watchdog.xmlParser.plugins.XMLParserPlugin;

public abstract class XMLProcessBlockParser<A extends ProcessBlock> extends XMLParserPlugin<A> {
	
	public static final String PARENT_TAG = XMLParser.PROCESS_BLOCK;
		
	/**
	 * [IMPORTANT] Extending classes must implement exactly a constructor of the same type or reflection call will fail!
	 * @param l
	 */
	public XMLProcessBlockParser(Logger l) {
		super(l);
	}
	
	@Override
	public String getNameOfParentTag() {
		return XMLProcessBlockParser.PARENT_TAG;
	}
}
