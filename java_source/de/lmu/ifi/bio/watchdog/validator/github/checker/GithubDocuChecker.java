package de.lmu.ifi.bio.watchdog.validator.github.checker;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;

import de.lmu.ifi.bio.watchdog.docu.Docu;
import de.lmu.ifi.bio.watchdog.docu.DocuXMLParser;
import de.lmu.ifi.bio.watchdog.docu.Moduledocu;
import de.lmu.ifi.bio.watchdog.docu.Paramdocu;
import de.lmu.ifi.bio.watchdog.docu.Returndocu;
import de.lmu.ifi.bio.watchdog.docu.extractor.XSDParameterExtractor;
import de.lmu.ifi.bio.watchdog.docu.extractor.XSDReturnValueExtractor;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Tests if the the XML docu of a module is valid
 * 1) XML follows XSD specification
 * 2) All parameter / return parameter are documented but not more
 * @author kluge
 *
 */
public class GithubDocuChecker extends GithubCheckerDocuBased {
	
	private static String PARAMETER = "parameter";
	private static String RETURN_VALUE = "return value";
	
	public GithubDocuChecker(String name) {
		super(name);
	}

	@Override
	public boolean test() {
		if(super.test()) {
			DocumentBuilderFactory dbf = DocuXMLParser.prepareDBF(this.watchdogBase);
			String baseFolder = null;
			if(this.isLocalTestMode())
				baseFolder = this.getModuleFolderToValidate();
			else 
				baseFolder = this.compareInfo.getModuleFolder();
			
			// try to parse the docu
			Moduledocu md = DocuXMLParser.parseXMLFile(dbf, this.xmlDocuFile, this.xsdFile, true);
			if(md == null) {
				this.error("Faild to parse the XML documentation file '"+ (baseFolder+File.separator+this.xmlDocuFile.getName()) +"'.");
				return false;
			}
			else {
				try {
					boolean ret = true;
					// get parameter and return values from basic XSD extractor plugin
					String tmpBaseDir = this.watchdogBase + File.separator + XMLParser.TMP_FOLDER;
					String xsdRoot = this.watchdogBase + File.separator + XMLParser.XSD;
					HashMap<String, ArrayList<Paramdocu>> params = new XSDParameterExtractor(tmpBaseDir, xsdRoot).getDocu(this.xsdFile);
					HashMap<String, ArrayList<Returndocu>> returnValues = new XSDReturnValueExtractor().getDocu(this.xsdFile);
					//HashSet<Integer> versions = XMLParser.getVersionsOfModule(dbf, xsd);
					
					// now we have all data --> do the compare
					HashSet<String> allParamNames = new HashSet<String>();
					HashSet<String> allRetunNames = new HashSet<String>();
					allParamNames.addAll(params.keySet());
					allParamNames.addAll(md.getParameter().stream().map(x -> x.getName()).collect(Collectors.toCollection(ArrayList<String>::new)));
					allRetunNames.addAll(returnValues.keySet());
					allRetunNames.addAll(md.getReturnValues().stream().map(x -> x.getName()).collect(Collectors.toCollection(ArrayList<String>::new)));
					
					// check if parameter are equal
					HashMap<String, Docu[]> paramsConv = new HashMap<>();
					HashMap<String, Docu[]> returnValuesConv = new HashMap<>();
					for(Entry<String, ArrayList<Paramdocu>> e : params.entrySet())
						paramsConv.put(e.getKey(), e.getValue().toArray(new Docu[] {}));
					for(Entry<String, ArrayList<Returndocu>> e : returnValues.entrySet())
						returnValuesConv.put(e.getKey(), e.getValue().toArray(new Docu[] {}));
					
					ret = ret & checkVersions(allParamNames, md, paramsConv, PARAMETER);
					ret = ret & checkVersions(allRetunNames, md, returnValuesConv, RETURN_VALUE);
					if(ret) 
						this.info("Validation of XML documentation file '"+ (baseFolder+File.separator+this.xmlDocuFile.getName()) +"' succeeded.");
					return ret;
				}
				catch(Exception e) {
					this.error("Failed to get expected parameter and return values from XSD file '"+ (baseFolder+File.separator+this.xsdFile.getName()) +"'.");
					return false;
				}
			}
		}
		return false;
	}
	

	/**
	 * checks, if version of parameter / return values are ok
	 * @param namesToCheck
	 * @param md
	 * @param xsd
	 * @param type
	 * @return
	 */
	private boolean checkVersions(HashSet<String> namesToCheck, Moduledocu md, HashMap<String, Docu[]> xsd, String type) {
		boolean ret = true;
		for(String name : namesToCheck) {
			Docu[] px = xsd.get(name);
			Docu pd = type.equals(PARAMETER) ? md.getParameter(name) : md.getReturnValue(name);
			
			if(px == null || px.length == 0) {
				this.error("Parameter '"+name+"' is documented but not part of the XSD module definition.");
				ret = false;
			}
			else if(pd == null) {
				this.error("Parameter '"+name+"' is not documented.");
				ret = false;
			}
			// check versions and text
			else {
				// get version range defined in XSD format (normally it will be only one entry)
				int miV = Integer.MAX_VALUE;
				int maV = Integer.MIN_VALUE;
				for(Docu p  : px) {
					miV = Math.min(p.getMinVersion(), miV);
					maV = Math.max(p.getMaxVersion(), maV);
				}
				maV = Math.max(miV, maV);

				// check if both versions are equal
				if(pd.getMinVersion() != miV) {
					this.error("Minimal supported version is not equal for "+type+" '"+name+"' in XSD and documentation: ("+ miV +" vs. "+ pd.getMinVersion() +")");
					ret = false;
				}
				if(pd.getMaxVersion() != maV) {
					this.error("Maximal supported version is not equal for "+type+" '"+name+"' in XSD and documentation: ("+ maV +" vs. "+ pd.getMaxVersion() +")");
					ret = false;
				}
			}
		}
		return ret;
	}
}
