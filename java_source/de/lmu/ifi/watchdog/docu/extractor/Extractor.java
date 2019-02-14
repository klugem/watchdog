package de.lmu.ifi.watchdog.docu.extractor;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.xml.parsers.DocumentBuilderFactory;

import de.lmu.ifi.watchdog.docu.Docu;
import de.lmu.ifi.watchdog.docu.Moduledocu;

/**
 * Generic interface for docu element extractor classes
 * @author kluge
 *
 * @param <A>
 */
public interface Extractor<A extends Docu> {
	
	/**
	 * test, if the extractor can be applied on that file
	 * @param f
	 * @return
	 */
	public boolean canBeApplied(File f);
	
	/**
	 * tries to identify the target file based on the module directory
	 * @param dbfXSD
	 * @param moduleDir
	 * @param xsdFile
	 * @return
	 */
	public default File findTarget(DocumentBuilderFactory dbfXSD, File moduleDir, File xsdFile) {
		return xsdFile;
	}
	
	/**
	 * tries to get some parameter for the docu from a file
	 * might throw any kind of exception
	 * at some time we might want to have multiple docu entries for the same name (with different versions)
	 * @param f file to parse
	 * @return
	 * @throws Exception
	 */
	public LinkedHashMap<String, ArrayList<A>> getDocu(File f) throws Exception;
	
	/**
	 * updates an existing module documentation
	 * @param prev docu as it was before
	 * @param extracted docu extracted by this extractor
	 * @return
	 */
	public Moduledocu updateDocu(Moduledocu prev, LinkedHashMap<String, ArrayList<A>> extracted);
}
