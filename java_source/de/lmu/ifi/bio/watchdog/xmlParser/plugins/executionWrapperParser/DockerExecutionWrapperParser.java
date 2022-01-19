package de.lmu.ifi.bio.watchdog.xmlParser.plugins.executionWrapperParser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.lmu.ifi.bio.watchdog.GUI.properties.views.executor.ExecutorPropertyViewController;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.wrapper.GUIDockerExecutionWrapperView;
import de.lmu.ifi.bio.watchdog.executionWrapper.container.DockerExecutionWrapper;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Execution wrapper that is based on the package manager Conda
 * @author kluge
 *
 */
public class DockerExecutionWrapperParser extends XMLExecutionWrapperParser<DockerExecutionWrapper> {
	private static final String XSD_DEF = "plugins" + File.separator + "wrapper.docker.xsd";
	public static final String DOCKER = "docker";
	
	public static final String PATH2DOCKER = "path2docker";
	public static final String IMAGE = "image";
	public static final String EXEC_KEYWORD = "execKeyword";
	public static final String ADD_PARAMS = "addCallParams";
	public static final String DISABLE_AUTODETECT_MOUNT = "disableAutodetectMount";
	public static final String MOUNT = "mount";
	public static final String HOST_DIR = "host";
	public static final String CONTAINER_DIR = "container";
	public static final String BLACKLIST = "blacklist";
	public static final String PATTERN = "pattern";
	public static final String LOAD_MODULE_SPECIFIC_IMAGE = "loadModuleSpecificImage";
	public static final String DISABLE_TERMINATE_WRAPPER = "disableTerminateWrapper";
	
	static {
		// register the execution wrapper plugins shipped with watchdog on GUI
		if(Functions.hasJavaFXInstalled()) {
			ExecutorPropertyViewController.registerWatchdogPluginOnGUI(DockerExecutionWrapper.class, GUIDockerExecutionWrapperView.class);
		}
	}

	public DockerExecutionWrapperParser(Logger l) {
		super(l);
	}

	@Override
	public String getNameOfParseableTag() {
		return DOCKER;
	}

	@Override
	public DockerExecutionWrapper parseElement(Element el, String watchdogBaseDir, Object[] additionalData) {
		@SuppressWarnings("unchecked")
		ArrayList<String> constants = new ArrayList<>(((LinkedHashMap<String, String>) additionalData[0]).values());
		String name = XMLParser.getAttribute(el, XMLParser.NAME);
		String path = XMLParser.getAttribute(el, PATH2DOCKER);
		String image = XMLParser.getAttribute(el, IMAGE);
		String execKeyword = XMLParser.getAttribute(el, EXEC_KEYWORD);
		String addParams = XMLParser.getAttribute(el, ADD_PARAMS);
		boolean loadModuleSpecificImage = Boolean.parseBoolean(XMLParser.getAttribute(el, LOAD_MODULE_SPECIFIC_IMAGE));
		boolean disableAutoD = Boolean.parseBoolean(XMLParser.getAttribute(el, DISABLE_AUTODETECT_MOUNT));
		boolean disableTerminateWrapper = Boolean.parseBoolean(XMLParser.getAttribute(el, DISABLE_TERMINATE_WRAPPER));
		HashMap<String, String> mounts = new HashMap<>();
		
		// get mapping
		NodeList mountNodes = el.getElementsByTagName(MOUNT);
		for(int i = 0 ; i < mountNodes.getLength(); i++) {
			Element m = (Element) mountNodes.item(i);
			NodeList childs = m.getChildNodes();
			String h = null;
			String c = null;
			// get host and container dir
			for(int ii = 0 ; ii < childs.getLength(); ii++) {
				Node n = childs.item(ii);
				if(n instanceof Element) {
					Element e = (Element) n;
					if(HOST_DIR.equals(e.getNodeName())) 
						h = e.getTextContent();
					else if(CONTAINER_DIR.equals(e.getNodeName()))
						c = e.getTextContent();
				}
			}
			
			// mount under same name
			if(c == null)
				c = h;
			// save the mount
			if(h != null) 
				mounts.put(h, c);
		}
		
		// get blacklist
		ArrayList<String> blackList = new ArrayList<>();
		NodeList blacklistNodes = el.getElementsByTagName(BLACKLIST);
		for(int i = 0 ; i < blacklistNodes.getLength(); i++) {
			Element m = (Element) blacklistNodes.item(i);
			String p = XMLParser.getAttribute(m, PATTERN);
			blackList.add(p);
		}

		DockerExecutionWrapper w = new DockerExecutionWrapper(name, watchdogBaseDir, path, image, execKeyword, addParams, disableAutoD, mounts, blackList, constants, loadModuleSpecificImage, disableTerminateWrapper);		
		return w;
	}

	@Override
	public String getXSDDefinition() {
		return XSD_DEF;
	}

	@Override
	public void runAdditionalTestsOnElement(String name) {}
}
