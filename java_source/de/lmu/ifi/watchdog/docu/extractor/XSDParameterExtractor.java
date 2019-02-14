package de.lmu.ifi.watchdog.docu.extractor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Element;

import de.lmu.ifi.bio.watchdog.helper.Parameter;
import de.lmu.ifi.bio.watchdog.helper.returnType.ReturnType;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import de.lmu.ifi.watchdog.docu.Moduledocu;
import de.lmu.ifi.watchdog.docu.Paramdocu;

/**
 * Extracts the parameters from the XSD file itself
 * uses internally the XMLParser (it's just a little wrapper)
 * @author kluge
 *
 */
public class XSDParameterExtractor implements Extractor<Paramdocu> {
	
	private static String FILE_PATTERN = "*.xsd$";
	private final String XSD_ROOT;
	private final File TMP_BASE;
	private final ArrayList<Element> ROOT_TYPES = new ArrayList<>();
	
	private static DocumentBuilderFactory DBF;
	
	static {
		DBF = DocumentBuilderFactory.newInstance();
		DBF.setIgnoringElementContentWhitespace(true);
		DBF.setNamespaceAware(true);
	}
	
	/**
	 * Constructor
	 * @param tmpBaseDir
	 * @param xsdRootDir
	 */
	public XSDParameterExtractor(String tmpBaseDir, String xsdRootDir) {
		this.TMP_BASE = new File(tmpBaseDir);
		this.XSD_ROOT = xsdRootDir;
		try {
			ArrayList<Element> rootTypes = XMLParser.getComplexAndSimpleTypes(DBF, XMLParser.BASE_ROOT_TYPES, this.XSD_ROOT);
			this.ROOT_TYPES.addAll(rootTypes);
		}
		catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	@Override
	public boolean canBeApplied(File f) {
		return f.getName().matches(FILE_PATTERN);
	}
	
	@Override
	public Moduledocu updateDocu(Moduledocu prev, LinkedHashMap<String, ArrayList<Paramdocu>> extracted) {
		// this is always executed as first extractor
		return prev;
	}

	@Override
	public LinkedHashMap<String, ArrayList<Paramdocu>> getDocu(File xsdFile) throws Exception {
		// use functions from XMLParser
		String tmpCacheDir = XMLParser.getXSDCacheDir(this.TMP_BASE);
		String taskType = XMLParser.getTaskTypeOfModule(DBF, xsdFile);
		Pair<Pair<File, File>, HashMap<String, Parameter>> raw = XMLParser.getParametersOfModule(DBF, xsdFile, tmpCacheDir, ROOT_TYPES, taskType, 0, true, true);
				
		// convert objects
		LinkedHashMap<String, ArrayList<Paramdocu>> ret = new LinkedHashMap<>();
		HashMap<String, Parameter> pp = raw.getValue();
		for(Parameter p : pp.values()) {
			String name = p.getName();
			ReturnType t = p.getType();
			String type = "TODO";
			if(t != null) 
				type = t.getType();
			
			Paramdocu np = new Paramdocu(name, type, null, null, null);
			np.setOccurs(p.getMin(), p.getMax());
			np.setVersions(p.minVer, p.maxVer);
			
			// save in hash
			if(!ret.containsKey(name))
				ret.put(name, new ArrayList<>());
			// add element
			ArrayList<Paramdocu> a = ret.get(name);
			a.add(np);
		}
		return ret;
	}
}
