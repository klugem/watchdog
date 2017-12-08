package de.lmu.ifi.bio.watchdog.task.actions.vfs.impl;

import java.util.LinkedHashMap;

import org.apache.commons.vfs2.provider.FileProvider;

import de.lmu.ifi.bio.watchdog.task.actions.vfs.VFSRegister;

/**
 * Abstract class that can be used to get a FileProvider based on a class name
 * @author kluge
 *
 */
public abstract class SimpleVFSRegister implements VFSRegister {
	
	private final FileProvider FP;
	private final String[] URL_SCHEMES;
	
	/**
	 * Can be used for classes that implement FileProvider and have a constructor without arguments 
	 * @param className
	 */
	public SimpleVFSRegister(String className, String[] urlSchemes) throws Exception {
		Class<?> c = this.getClass().getClassLoader().loadClass(className);
		// ensure that a class implementing FileProvider is given
		if(!FileProvider.class.isAssignableFrom(c))
			throw new IllegalArgumentException("Class '"+className+"' does not implement the interface FileProvider.");
		
		// ensure that this is not null
		if(urlSchemes == null)
			urlSchemes = new String[0];
		
		this.FP = (FileProvider) c.newInstance();
		this.URL_SCHEMES = urlSchemes;
	}

	@Override
	public FileProvider getFileProvider() {
		return this.FP;
	}

	@Override
	public String[] getURLSchemes() {
		return this.URL_SCHEMES;
	}

	@Override
	public LinkedHashMap<String, String> getMimeTypes() {
		return null;
	}

	@Override
	public LinkedHashMap<String, String> getExtensions() {
		return null;
	}
}

