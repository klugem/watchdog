package de.lmu.ifi.bio.watchdog.task.actions.vfs;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Map.Entry;

import de.lmu.ifi.bio.watchdog.logger.Logger;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

/**
 * loads all registered Apache Commons VFS 
 * @author kluge
 *
 */
public class VFSLoader {

	/**
	 * searches for classes that implement the VFS Register interface and loads all registered FileProviders
	 */
	public static void configureVFS(WatchdogFileSystemManager fsm, Logger log) {
		for(Class<? extends VFSRegister> c : getImplementingClasses()) {
			log.info("Registering VFS provider defined in '" +c.getName()+ "'.");
			try {
				VFSRegister reg = c.getConstructor().newInstance();
				
				// set default provider
				if(reg.getURLSchemes().length == 0) 
					fsm.setDefaultProvider(reg.getFileProvider());
				else // add the file provider
					fsm.addProvider(reg.getURLSchemes(), reg.getFileProvider());
				
				// add mime-type
				if(reg.getMimeTypes() != null) {
					for(Entry<String, String> e : reg.getMimeTypes().entrySet())
						fsm.addMimeTypeMap(e.getKey(), e.getValue());
				}
				
				// add the extensions
				if(reg.getExtensions() != null) {
					for(Entry<String, String> e : reg.getExtensions().entrySet())
						fsm.addExtensionMap(e.getKey(), e.getValue());
				}
			}
			catch(Exception e) {
				log.error("Failed to register provider defined in class '" +c.getName()+ "'.");
				e.printStackTrace();
			}
		}
	}
	
	public static ArrayList<Class<? extends VFSRegister>> getImplementingClasses() {
		ArrayList<Class<? extends VFSRegister>> classes = new ArrayList<>();
		// collect all classes that implement the VFSRegister 
		new FastClasspathScanner("").matchClassesImplementing(VFSRegister.class, 
		c -> {
			// look at all non abstract classes
			if(!Modifier.isAbstract(c.getModifiers()))
				classes.add(c);
		}).scan();
		return classes;
	}
}
