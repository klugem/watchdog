package de.lmu.ifi.bio.watchdog.helper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import de.lmu.ifi.bio.multithreading.TimedExecution;
import de.lmu.ifi.bio.watchdog.executor.WatchdogThread;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;

/**
 * Some simple helper functions which could be reused at some other place.
 * @author Michael Kluge
 *
 */
public class Functions {
	
	private static final String DATE_FORMAT = "MM/dd/YY-HH:mm:ss";
	private static final String UTF8 = "UTF-8";
	public static MessageDigest MD;
	private static final Logger LOGGER = new Logger(LogLevel.WARNING);
	private static final String ONE = "1";
	private static final String TRUE= "true";
	private static final String POINT = ".";
	private static final String TMP_ENDING = ".tmp";
	private static String temporaryFolder = "/tmp";
	private static final String WORKING_DIR_WATCHDOG = "watchdogWork_";
	private static final String JAVA_FX = "javafx.runtime.version"; 
	private static boolean hasJavaFX = false;
	
	static {
		try {
			MD = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			LOGGER.error("Can not find sha-256");
			System.exit(1);
		}
		
		// test for javafx availability
		if(System.getProperties().containsKey(JAVA_FX)) {
			hasJavaFX = true;
		}
	}
	
	/**
	 * returns a current date in a specific format
	 * @return
	 */
	public static String getCurrentDateAndTime() {
		return new SimpleDateFormat(DATE_FORMAT).format(new Date()); 
	}
	
	/**
	 * true, if javafx can be loaded on the system
	 * @return
	 */
	public static boolean hasJavaFXInstalled() {
		return hasJavaFX;
	}
	
	/**
	 * gets the first x parts of a name (parts are separated by '.') and removes the rest of the string
	 * @param name
	 * @param prefixLength
	 * @return
	 */
	public static String getPrefixName(String name, int prefixLength, String sep) {
		if(prefixLength > 0) {
			if(sep == null) 
				sep = POINT;

			String tmp[] = StringUtils.split(name, sep, prefixLength+1);
			tmp[tmp.length-1] = "";
			String result = StringUtils.join(tmp, sep);
			return result.substring(0, result.length()-1);
		}
		return name;
	}
	
	/**
	 * can be used for "lite debugging"
	 */
	public static void throwException() {
		try { throw new Exception(); } catch(Exception e) {e.printStackTrace();};
	}

	/**
	 * Checks if the value is an XML true value ('true' or '1') is accepted
	 * @param value
	 * @return
	 */
	public static boolean isTrueXMLValue(String value) {
		return value.equals(ONE) || value.equals(TRUE);
	}
	
	/**
	 * removes n endings of a input string splitted by separator
	 * @param input
	 * @param n
	 * @return
	 */
	public static String removeEndings(String input, int n, String separator) {
		if(n == 0)
			return input;
		
		StringBuffer buf = new StringBuffer();
		int i = 0;
		if(input.endsWith(separator) && n > 0)
			input=input + " ";
		
		String splitSep = separator;
		if(splitSep.equals("."))
			splitSep = "\\.";
		
		String tmp[] = input.split(splitSep);
		for(String t : tmp) {
			// test if rest of the stuff should be removed
			if(i < tmp.length - n) { 
				// add separator char
				if(i > 0) 
					buf.append(separator);
				
				// add name
				buf.append(t);
			}
			else
				break;

			i++;
		}
		// if no stuff is left in the buffer
		if(buf.length() == 0) {
			LOGGER.warn("End removal of the last '"+n+"' endings of '"+input+"' with '"+separator+"' as separator resulted in an empty string.");
			try {
				throw new IllegalArgumentException("");
			}
			catch(Exception e) { e.printStackTrace(); }
		}
		return buf.toString();
	}
	
	/** 
	 * sets a new temporary base folder
	 * @param tmpBaseFolder
	 */
	public static void setTemporaryFolder(String tmpBaseFolder) {
		temporaryFolder = tmpBaseFolder;
	}
	 
	/**
	 * generates a temporary file
	 * @return
	 */
	public static File generateRandomTmpExecutionFile(String prefix, boolean noDelete) {
		try {
			File f = Files.createTempFile(prefix, TMP_ENDING).toFile();
			f.delete();
			File ff = new File(temporaryFolder + File.separator + f.getName());
			//if(!noDelete) // TODO: remove comment
			//	ff.deleteOnExit(); 
			return ff;
		}
		catch(Exception e) {
			LOGGER.error("Could not create temporary file.");
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	
	/**
	 * generates a temporary file
	 * @return
	 */
	public static File generateRandomLogFile(boolean err, boolean noDelete) {
		return generateRandomTmpExecutionFile(err ? "watchError" : "watchOut", noDelete);
	}
	
	/**
	 * creates a random working dir
	 * @param prefix
	 * @return
	 */
	public static File generateRandomWorkingDir(String prefix) {
		try {
			if(prefix.contains(WORKING_DIR_WATCHDOG))
				prefix = prefix.replaceFirst(WORKING_DIR_WATCHDOG + ".*$", "");
			
			File f = new File(prefix + File.separator + Files.createTempDirectory(WORKING_DIR_WATCHDOG).toFile().getAbsolutePath().replace("/tmp", ""));
			// remove the folder on termination using commons.io
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {try { FileUtils.deleteDirectory(f); } catch(Exception e) { e.printStackTrace(); }}));
			return f;
		}
		catch(Exception e) {
			LOGGER.error("Could not create temporary folder.");
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	public static void filterErrorStream() {
		// hide errors that are caused by apache.xerces (more specifically cvc-elt.1.a which is not an real error as stated in XMLSchemaValidator)
		PrintStream err = System.err;
		// let them copy the "wrong" printStream that can be read by us in order to print other errors
		ErrorParserFilter filter = new ErrorParserFilter(err);
		System.setErr(new PrintStream(filter));
		// reset it
		TimedExecution.addRunableNamed(() -> resetErrorStream(filter, err), 2, TimeUnit.SECONDS, "resetErrorStream");
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {resetErrorStream(filter, err); try {Thread.sleep(2500);} catch(Exception e) {} }));
		// TODO!
	}
	
	public static void resetErrorStream(ErrorParserFilter filter, PrintStream err) {
		System.setErr(err);
		try { filter.close(); } catch(Exception e) { }
	}

	public static void write(Path file, String data) {
		BufferedWriter output = null;
		try {
			output = new BufferedWriter(new FileWriter(file.toFile()));
			output.write(data);
			output.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (output != null) {
				try { output.close(); } catch(Exception e) {}
			}
		}
	}
	
	public static String getHash(String stringToHash) throws UnsupportedEncodingException {
		return getHash(stringToHash.toString().getBytes(UTF8));
	}
	
	public static String getHash(byte[] dataToHash) {
		return Hex.encodeHexString(Functions.MD.digest(dataToHash));
	}
	
	public static String getFileHash(File filename) throws IOException {
		return getHash(Files.readAllBytes(filename.toPath()));
	}
	
}
