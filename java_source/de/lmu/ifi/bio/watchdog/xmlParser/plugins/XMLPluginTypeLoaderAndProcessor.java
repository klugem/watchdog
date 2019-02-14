package de.lmu.ifi.bio.watchdog.xmlParser.plugins;

import java.io.File;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.w3c.dom.Element;

import de.lmu.ifi.bio.watchdog.interfaces.XMLPlugin;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

public abstract class XMLPluginTypeLoaderAndProcessor<A extends XMLPlugin> {
	
	protected final HashMap<String, XMLParserPlugin<A>> PARSER = new HashMap<>();
	private final ArrayList<String> XSD_FILES = new ArrayList<>();
	private boolean init = false;
	
	private Class<?> getBaseType() throws ClassNotFoundException {
		ParameterizedType pt = (ParameterizedType) this.getClass().getGenericSuperclass();
		String cname = pt.getActualTypeArguments()[0].getTypeName().replaceAll("^class\\s+", "");
	    return Class.forName(cname);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public HashSet<String> init(Logger l, boolean noExit, boolean isGUILoadAttempt, String watchdogBase) {
		if(this.init == false) {
			// load all XML plugins of that type that are installed
			try {
				Class<?> bt = this.getBaseType();
				ScanResult sr = new ClassGraph().enableClassInfo().whitelistPackages(".*").scan();
				ClassInfoList candidateClasses = sr.getSubclasses(XMLParserPlugin.class.getName());
				for(ClassInfo c : candidateClasses) {
					try {
						// look at all non abstract classes
						if(!Modifier.isAbstract(c.getModifiers())) {
							// get type of class

							ParameterizedType pt = (ParameterizedType) c.loadClass().getGenericSuperclass();
							String cname = pt.getActualTypeArguments()[0].getTypeName().replaceAll("^class\\s+", "");

							Class<?> parsesPluginsOfType = Class.forName(cname);
							// load all classes that have the correct parent
							if(bt.isAssignableFrom(parsesPluginsOfType)) {
								l.info("Loading XML-Plugin: '"+c.loadClass().getCanonicalName()+"'");
								XMLParserPlugin<A> xp = XMLParserPlugin.getInstance((Class<? extends XMLParserPlugin>) c.loadClass(), l, noExit, isGUILoadAttempt);
								this.PARSER.put(xp.getNameOfParseableTag(), xp);
							}
						}
					}
					catch(Exception e) { 
						l.error("Failed to load XML Plugins!");
						e.printStackTrace();
						System.exit(1);
					}
				}
				sr.close();
			}
			catch(ClassNotFoundException e) {
				l.error("Failed find base class for '"+this.getClass().getGenericSuperclass().getTypeName()+"'!");
           		e.printStackTrace();
           		System.exit(1);
			}
			this.init = true;
			return this.getALLXSDFiles(watchdogBase + File.separator + "xsd");
		}
		// clear parses for this run
		for(XMLParserPlugin<A> xp : PARSER.values()) {
			xp.setExitAndGUIInfoAndClear(noExit, isGUILoadAttempt);
		}
		return null;
	}
	
	public HashSet<String> getALLXSDFiles(String watchdogBase) {
		HashSet<String> xsd = new HashSet<>();
		for(XMLParserPlugin<A> xp : this.PARSER.values()) {
			xsd.add(watchdogBase + File.separator + xp.getXSDDefinition());
		}
		return xsd;
	}
	/**
	 * can only be called once after a parseElement cycle
	 * @return
	 */
	public HashSet<String> getPrefixNamesOfXSDFilesToLoad() {
		HashSet<String> copy = new HashSet<>();
		copy.addAll(this.XSD_FILES);
		this.XSD_FILES.clear();
		return copy;
	}
	
	public A parseElement(Element el, String watchdogBaseDir, Object[] additionalData) {
		String type = el.getTagName();
		if(this.PARSER.containsKey(type)) {
			XMLParserPlugin<A> xp = this.PARSER.get(type);
			this.XSD_FILES.add(xp.getXSDDefinition());
			return xp.parse(el, watchdogBaseDir, additionalData);
			
		}
		throw new IllegalArgumentException("No XML Parser plugin is loaded for type '"+type+"' of parent type " + this.getClass().getSimpleName());
	}
	
	public void runAdditionalTest(String type, HashSet<String> namesOfExecutors) {
		this.PARSER.get(type).runAdditionalTest(namesOfExecutors);
	}
}
