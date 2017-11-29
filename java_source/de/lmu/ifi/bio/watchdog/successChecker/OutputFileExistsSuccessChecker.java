package de.lmu.ifi.bio.watchdog.successChecker;

import java.io.File;

import de.lmu.ifi.bio.watchdog.interfaces.SuccessChecker;
import de.lmu.ifi.bio.watchdog.task.Task;

/**
 * Tests if an output file was successfully created and is not empty.
 * @author Michael Kluge
 *
 */
public class OutputFileExistsSuccessChecker extends SuccessChecker {
	
	private final String OUT_PATH;
		
	/**
	 * Constructor with path to output file
	 * @param outputPath
	 */
	public OutputFileExistsSuccessChecker(Task t, String outputPath) {
		super(t);
		this.OUT_PATH = outputPath;
	}

	@Override
	public boolean hasTaskSucceeded() {
		File f = new File(this.OUT_PATH); 
		return f.exists() && f.isFile() && f.length() > 0;
	}
}
