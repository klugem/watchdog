package de.lmu.ifi.watchdog.docu.extractor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;

import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.watchdog.docu.Docu;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

/**
 * Finds docu extractor plugins
 * @author kluge
 *
 * @param <A>
 */
public class ExtractorFinder<A extends Docu> {
	
	private static HashSet<String> IGNORE = new HashSet<>();
	
	static {
		IGNORE.add(XSDParameterExtractor.class.getName());
		IGNORE.add(XSDReturnValueExtractor.class.getName());
	}
	
	/**
	 * finds a classes that can extract a specific type of docu element 
	 * @param template
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<Class<? extends Extractor<A>>> getImplementingExtractors(Class<A> generic) {
		ArrayList<Class<? extends Extractor<A>>> classes = new ArrayList<>();
		// collect all classes that implement the Extractor interface of a correct type
		ScanResult sr = new ClassGraph().enableClassInfo().whitelistPackages(".*").scan();
		ClassInfoList candidateClasses = sr.getClassesImplementing(Extractor.class.getName());
		for(ClassInfo c : candidateClasses) {
			String type = c.getTypeSignature().getSuperinterfaceSignatures().get(0).getTypeArguments().get(0).getTypeSignature().toString();
			// look at all non abstract classes
			if(!Modifier.isAbstract(c.getModifiers()) && type.equals(generic.getTypeName())) {
				// do not save classes we don't want to be included
				if(!IGNORE.contains(c.getName()))
					classes.add((Class<? extends Extractor<A>>) c.loadClass());
			}
		}
		sr.close();
		return classes;
	}
	
	/**
	 * asks, which plugins should be loaded and retuns an array with instances of that extractor plugins
	 * @param pluginClasses
	 * @param type
	 * @param log
	 * @return
	 */
	public ArrayList<Extractor<A>> askForPlugins(ArrayList<Class<? extends Extractor<A>>> pluginClasses, String type, Logger log) {
		ArrayList<Extractor<A>> results = new ArrayList<>();
		int i = 1;
		if(pluginClasses.size() > 0) {
			System.out.println("Please enter the ids of the "+type+" extractor plugins you want to load (in the same order you want them to be applied):");
			System.out.println("example: 2,1");
			for(Class<? extends Extractor<? extends Docu>> c : pluginClasses) {
				System.out.println(i+": '" + c.getName() + "'");
				i++;
			}
			try {
				String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
				if(line.length() > 0) {
					String[] ids = line.split(",\\W*");
					for(String s : ids) {
						int id = -1;
						try {
							id = Integer.parseInt(s) - 1;
						}
						catch(NumberFormatException ne) {
							log.error("Failed to parse '"+s+"' to integer.");
							System.exit(1);
						}
						
						// create the object of that class
						if(id >= 0 && pluginClasses.size() >= (id+1)) {
							Extractor<A> e =pluginClasses.get(id).getDeclaredConstructor().newInstance();
							results.add(e);
						}
						else {
							log.error("Invalid id of extractor plugin: '"+id+"'");
							System.exit(1);
						}
					}
				}
			}
			catch(Exception e) {
				e.printStackTrace();
				log.error("Failed to get line from stdin.");
				System.exit(1);
			}
		}
		return results;
	}
}
