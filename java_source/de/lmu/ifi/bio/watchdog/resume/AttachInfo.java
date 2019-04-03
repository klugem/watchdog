package de.lmu.ifi.bio.watchdog.resume;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;

import de.lmu.ifi.bio.watchdog.logger.Logger;

/**
 * Class that stores some info that must persist detach and attach
 *
 */
public class AttachInfo {

	public static final String BAK = ".bak";
	public static final HashMap<String, Object> DATA_TO_SAVE = new HashMap<>(); // info that should be saved
	public static final HashMap<String, Object> LOADED_DATA = new HashMap<>(); // data that was loaded
	public static final HashSet<String> ATTACH_TEST = new HashSet<>(); // mandatory fields that must exist
	
	// key names for detach/attach mode
	public static final String ATTACH_RESUME_FILE = "ATTACH_RESUME_FILE";
	public static final String ATTACH_RUNNING_TASKS = "ATTACH_RUNNING_TASKS";
	public static final String ATTACH_INITIAL_START_TIME = "ATTACH_INITIAL_START_TIME";
	
	static {
		// these fields are automatically tested for existence after de-serialization
		ATTACH_TEST.add(ATTACH_RESUME_FILE);
		ATTACH_TEST.add(ATTACH_RUNNING_TASKS);
		ATTACH_TEST.add(ATTACH_INITIAL_START_TIME);
	}
	
	/**
	 * adds a object that should persist detach
	 * @param name
	 * @param data
	 * @return
	 */
	public static boolean setValue(String name, Object data) {
		// check, if data exists already
		if(AttachInfo.DATA_TO_SAVE.containsKey(name))
			return false;
		
		// save the data
		AttachInfo.DATA_TO_SAVE.put(name, data);
		return true;
	}
	
	/**
	 * returns the data that was loaded
	 * @param name
	 * @return
	 */
	public static Object getLoadedData(String name) {
		return LOADED_DATA.get(name);
	}
	
	/**
	 * Tests if some data with that name was loaded
	 * @param name
	 * @return
	 */
	public static boolean hasLoadedData(String name) {
		return LOADED_DATA.containsKey(name);
	}


	/**
	 * reads the attach info of running jobs and other data for detach&attach mode from a file
	 * @param attachFile
	 * @param Logger log
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static HashMap<String, Object> loadAttachInfoFromFile(File attachFile, Logger log) {
		if(!(attachFile.isFile() && attachFile.exists() && attachFile.canRead() && attachFile.length() > 0))
			return null;

		// get object input stream from file
		HashMap<String, Object> o = null;
		ObjectInputStream ois = null;
		try { 
			byte[] buffer = Files.readAllBytes(Paths.get(attachFile.getAbsolutePath()));
			InputStream bais = new ByteArrayInputStream(buffer, 0, buffer.length);
	        ois = new ObjectInputStream(bais); 
	        
	        // try to deserialize the data
        	o = (HashMap<String, Object>) ois.readObject();
        	LOADED_DATA.putAll(o);
        } 
        catch(Exception e) {
        	log.error("File '"+attachFile.getAbsolutePath()+"' does not contain a valid attach info.");
        	e.printStackTrace();
        }
        try { if(ois != null) ois.close(); } catch(Exception e) {}
        
        // test, if the required fields are there
        for(String key : ATTACH_TEST) {
        	if(!o.containsKey(key)) {
        		log.error("File '"+attachFile.getAbsolutePath()+"' does not contain a value for required attach key '"+key+"'.");
        		o = null;
        		break;
        	}
        }
		return o;
	}
	
	/**
	 * writes the saved attach info to a File
	 * @param outFile
	 * @return true, if file was written successfully
	 */
	public static boolean saveAttachInfoToFile(String outFile) {
		try {
			ByteArrayOutputStream baos = AttachInfo.convertHashMapToByteStream(AttachInfo.DATA_TO_SAVE);
		
			// write it first and then move replace it in order to (nearly) ensure that a complete file exists
			String outFileBak = outFile + BAK;
			File ofb = new File(outFileBak);
			while(ofb.exists()) {
				outFileBak = outFileBak + "_";
				ofb = new File(outFileBak);
			}
			Files.write(Paths.get(outFileBak), baos.toByteArray());
			
			// rename the file
			File detachFile = new File(outFile);
			new File(outFileBak).renameTo(detachFile);
			// test if write was successful
			if(!detachFile.exists()) {
				return false;
			}
			else {
				return true;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Converts a HashMap to a ByteArrayOutputStream
	 * @param data
	 * @return
	 * @throws IOException
	 */
	private static ByteArrayOutputStream convertHashMapToByteStream(HashMap<String, Object> data) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(data);
		return baos;
	}
}