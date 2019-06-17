package de.lmu.ifi.bio.watchdog.docu.extractor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.docu.Moduledocu;
import de.lmu.ifi.bio.watchdog.docu.Returndocu;
import de.lmu.ifi.bio.watchdog.helper.returnType.ReturnType;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Extracts the return values from the XSD file itself
 * uses internally the XMLParser (it's just a little wrapper)
 * @author kluge
 *
 */
public class XSDReturnValueExtractor implements Extractor<Returndocu> {
	
	private static String FILE_PATTERN = "*.xsd$";
	private static DocumentBuilderFactory DBF;
	
	static {
		DBF = DocumentBuilderFactory.newInstance();
		DBF.setIgnoringElementContentWhitespace(true);
		DBF.setNamespaceAware(true);
	}
	
	@Override
	public boolean canBeApplied(File f) {
		return f.getName().matches(FILE_PATTERN);
	}
	
	@Override
	public Moduledocu updateDocu(Moduledocu prev, LinkedHashMap<String, ArrayList<Returndocu>> extracted) {
		// this is always executed as first extractor
		return prev;
	}

	@Override
	public LinkedHashMap<String, ArrayList<Returndocu>>getDocu(File xsdFile) throws Exception {
		Pair<HashMap<String, ReturnType>, String> raw = XMLParser.getReturnTypeOfModule(DBF, xsdFile, true);

		// convert objects
		LinkedHashMap<String, ArrayList<Returndocu>> ret = new LinkedHashMap<>();
		HashMap<String, ReturnType> rts = raw.getKey();
		for(String name : rts.keySet()) {
			ReturnType r = rts.get(name); 
			String type = r.getType();
			Returndocu nr = new Returndocu(name, type, null);
			nr.setVersions(r.minVer, r.maxVer);
		
			// save in hash
			if(!ret.containsKey(name))
				ret.put(name, new ArrayList<>());
			// add element
			ArrayList<Returndocu> a = ret.get(name);
			a.add(nr);
		}
		return ret;
	}
}
