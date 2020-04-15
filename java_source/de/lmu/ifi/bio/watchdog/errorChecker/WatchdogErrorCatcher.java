package de.lmu.ifi.bio.watchdog.errorChecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Serializable;

import de.lmu.ifi.bio.watchdog.interfaces.ErrorChecker;
import de.lmu.ifi.bio.watchdog.task.Task;

/**
 * Catches the errors found in the stdout and stderr streams which were created using the core library of Watchdog
 * @author Michael Kluge
 *
 */
public class WatchdogErrorCatcher extends ErrorChecker implements Serializable {

	private static final long serialVersionUID = 3931343051645318056L;
	public static final String ERROR_START = "[ERROR]";
	public static final String ERROR_CHECKER_START = ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>";
	public static final String ERROR_CHECKER_END = "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<";
	private final static int WAIT = 10; // wait time in ms for files
	private final static int COUNTER = 1000; // counter
	
	
	/**
	 * Constructor
	 * @param t
	 */
	public WatchdogErrorCatcher(Task t) {
		super(t);
	}
		
	/**
	 * checks, if errors could be found in the file and if so adds them
	 * @param f
	 */
	private void checkFile(File f) {
		int i = 0;
		// check, if file is there and give it a few seconds until file is there
		while(!f.exists()) {
			try { Thread.sleep(WAIT); } catch(Exception e) { break; }
			i++;
			
			if(i > COUNTER) {
				LOGGER.error("File '"+f.getAbsolutePath()+"' could not be opend for error checking.");
				this.ERRORS.add("File '"+f.getAbsolutePath()+"' could not be opend for error checking.");
				return;
			}
		}
		if(f.isDirectory()) {
			LOGGER.error("Error file that should be checked is directory '("+f.getAbsolutePath()+")'.");
			this.ERRORS.add("Error file that should be checked is directory '("+f.getAbsolutePath()+")'.");
		} else if(!f.canRead()) {
			LOGGER.error("Error file that should be checked is not readable '("+f.getAbsolutePath()+")'.");
			this.ERRORS.add("Error file that should be checked is not readable '("+f.getAbsolutePath()+")'.");
		} else {
			StringBuffer buffer = new StringBuffer();
			boolean errorMode = false;
			String line;
			try {
				BufferedReader br = new BufferedReader(new FileReader(f));
				while((line = br.readLine()) != null) { // TODO: add dedicated error checker for docker
					if(line.startsWith(ERROR_START) || line.startsWith("Error:"))
						buffer.append(line);
					else if(line.contains(ERROR_CHECKER_START))
						errorMode = true;
					else if(line.contains(ERROR_CHECKER_END))
						errorMode = false;
					
					// add line, while in error mode
					if(errorMode)
						buffer.append(line);
				}
				br.close();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			if(buffer.length() > 0)
				this.ERRORS.add(buffer.toString());
		}
	}

	@Override
	public boolean hasTaskFailed() {
		if(!this.wasCheckPerformed()) {
			File out = T.getStdOut(false);
			File err = T.getStdErr(false);
			
			// find errors the in the files
			if(out != null)
				this.checkFile(out);
			if(err != null && (out == null || !out.getAbsolutePath().equals(err.getAbsolutePath())))
				this.checkFile(err);
			// mark that check was performed
			this.checkWasPerformed();
		}
		return ERRORS.size() > 0;
	}
}
