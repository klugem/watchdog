package de.lmu.ifi.bio.watchdog.task.actions.vfs;

import java.util.LinkedHashMap;

import org.apache.commons.vfs2.provider.FileProvider;

/**
 * Classes that implement that interface are automatically added to the default FileSystemManager
 * of commons/vfs2 and hence can be used within task actions
 * @author kluge
 *
 */
public interface VFSRegister {
	
	/**
	 * must return a instance of the FileProvider that should be used within Watchdog
	 * DefaultFileSystemManager.addProvider(this.getURLSchemes(), this.getFileProvider())
	 * will be called
	 * @return
	 */
	public FileProvider getFileProvider();

	/**
	 * must return the URL schemes that should use the FileProvider
	 * DefaultFileSystemManager.addProvider(this.getURLSchemes(), this.getFileProvider())
	 * will be called
	 * @return
	 */
	public String[] getURLSchemes();
		
	/**
	 * must return the mimetypes that should be registered for that FileProvider
	 * key: mimeType; value: scheme
	 * DefaultFileSystemManager.addMimeTypeMap(mimeType, scheme)
	 * will be called for each entry of the HashMap
	 * @return
	 */
	public LinkedHashMap<String, String> getMimeTypes();
	
	/**
	 * must return the file extensions that should be registered for that FileProvider
	 * key: mimeType; extension: scheme
	 * DefaultFileSystemManager.addExtensionMap(extension, scheme)
	 * will be called for each entry of the HashMap
	 * @return
	 */
	public LinkedHashMap<String, String> getExtensions();
}
