package de.lmu.ifi.bio.watchdog.xmlParser.plugins.executionWrapperParser;

import de.lmu.ifi.bio.watchdog.executionWrapper.ExecutionWrapper;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import de.lmu.ifi.bio.watchdog.xmlParser.plugins.XMLParserPlugin;

public abstract class XMLExecutionWrapperParser<A extends ExecutionWrapper> extends XMLParserPlugin<A> {
	
	public static final String PARENT_TAG = XMLParser.EXECUTION_WRAPPER;
		
	/**
	 * [IMPORTANT] Extending classes must implement exactly a constructor of the same type or reflection call will fail!
	 * @param l
	 */
	public XMLExecutionWrapperParser(Logger l) {
		super(l);
	}
	
	@Override
	public String getNameOfParentTag() {
		return XMLExecutionWrapperParser.PARENT_TAG;
	}
}
