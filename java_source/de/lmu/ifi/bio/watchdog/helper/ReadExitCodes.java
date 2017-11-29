package de.lmu.ifi.bio.watchdog.helper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import de.lmu.ifi.bio.watchdog.logger.Logger;

/**
 * Reads the exit codes from a file in order to display them in the mail notifications
 * @author Michael Kluge
 *
 */
public class ReadExitCodes {
	public static final String EXIT_CODE_NOT_KNOWN = "n.a.";
	public static final String STRIP = "^EXIT_";
	public static final String EQUAL = "=";
	public static final HashMap<Integer, String> EXIT_CODES = new HashMap<>(); 
	public static final Logger LOGGER = new Logger();
	
	/**
	 * reads the exit codes from a bash file; format: EXIT_CODE_NAME=EXIT_CODE
	 * @param f
	 * @return
	 */
	public static boolean readExitCodes(Path exitCodePath) {
		String tmp[];
		
		try {
			for(String line : Files.readAllLines(exitCodePath)) {
				// try to split the line at '='
				tmp = line.split(EQUAL);
				
				if(tmp.length == 2) {
					EXIT_CODES.put(Integer.parseInt(tmp[1]), tmp[0].replaceAll(STRIP, "").toLowerCase().replace("_", " "));
				}
			}
			return EXIT_CODES.size() > 0;
		} catch (NumberFormatException | IOException e) {
			LOGGER.error("Failed to open exit code file '"+ exitCodePath.toFile().getAbsolutePath() +"'!");
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * returns the name of that EXIT code
	 * @param exitCode
	 * @return
	 */
	public static String getNameOfExitCode(int exitCode) {
		if(EXIT_CODES.containsKey(exitCode))
			return EXIT_CODES.get(exitCode);
		else
			return EXIT_CODE_NOT_KNOWN;
	}
}
