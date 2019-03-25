package de.lmu.ifi.bio.watchdog.xmlParser.plugins.executorParser;

import org.w3c.dom.Element;

import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import de.lmu.ifi.bio.watchdog.xmlParser.plugins.XMLParserPlugin;

public abstract class XMLExecutorInfoParser<A extends ExecutorInfo> extends XMLParserPlugin<A> {
	
	public static final String PARENT_TAG = XMLParser.EXECUTOR;
		
	/**
	 * [IMPORTANT] Extending classes must implement exactly a constructor of the same type or reflection call will fail!
	 * @param l
	 */
	public XMLExecutorInfoParser(Logger l) {
		super(l);
	}
	
	@Override
	public String getNameOfParentTag() {
		return XMLExecutorInfoParser.PARENT_TAG;
	}

	/**
	 * should be used to parse mandatory arguments and use them later on 
	 * @param el
	 * @param watchdogBaseDir
	 * @param additionalData: first element must be of type Environment
	 * @return
	 */
	public DefaultExecutorInfo parseMandatoryParameter(Element el, String watchdogBaseDir, Object[] additionalData) {
		Environment envExecutor = null;
		if(additionalData.length > 0 && additionalData[0] instanceof Environment)
			envExecutor = (Environment) additionalData[0];

		// default attributes each executor has based on the XSD definition
		String name = XMLParser.getAttribute(el, XMLParser.NAME);
		boolean isDefault = Boolean.parseBoolean(el.getAttribute(XMLParser.DEFAULT));
		boolean isStick2Host = Boolean.parseBoolean(el.getAttribute(XMLParser.STICK2HOST));
		Integer maxSlaveRunning = Integer.parseInt(XMLParser.getAttribute(el, XMLParser.MAX_SLAVE_RUNNING));
		String workingDir = el.getAttribute(XMLParser.WORKING_DIR_EXC);
		int maxRunning = Integer.parseInt(XMLParser.getAttribute(el, XMLParser.MAX_RUNNING));
		String color = XMLParser.getAttribute(el, XMLParser.COLOR);
		String path2java = XMLParser.getAttribute(el, XMLParser.PATH2JAVA);
		String shebang = XMLParser.getAttribute(el, XMLParser.SHEBANG);

		// generate info class
		DefaultExecutorInfo exinfo = new DefaultExecutorInfo(name, isDefault, isStick2Host, maxSlaveRunning, path2java, maxRunning, watchdogBaseDir, envExecutor, workingDir, shebang);
		// set color, if some is set
		if(color != null)
			exinfo.setColor(color);
		
		return exinfo;
	}
}
