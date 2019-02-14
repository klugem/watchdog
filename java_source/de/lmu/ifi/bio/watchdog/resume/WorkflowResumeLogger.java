package de.lmu.ifi.bio.watchdog.resume;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.task.StatusHandler;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask;

/**
 * Writes the status of the workflow execution to a file
 * @author kluge
 *
 */
public class WorkflowResumeLogger implements StatusHandler {
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
	private static final String ENDING = ".xml$";
	private static final String LOG_ENDING = ".watchdog.status.log";
	
	private final Logger LOGGER = new Logger();
	private PrintWriter OUT;
	
	/**
	 * Constructor that will automatically open a file with a timestamp suffix
	 * @param workflowXMLFile
	 */
	public WorkflowResumeLogger(File workflowXMLFile) {
		String time = SDF.format(new Date());
		String basename = workflowXMLFile.getAbsolutePath().replaceFirst(ENDING, "");
		String fname = basename + "_" + time + LOG_ENDING;
		
		try {
			this.OUT = new PrintWriter(new BufferedWriter(new FileWriter(fname)));
		}
		catch(IOException e) {
			this.LOGGER.error("Failed to write to file '"+fname+"'.");
			System.exit(1);
		}
	}

	@Override
	public void handle(Task task) {
		if(task.hasTaskFinishedWithoutBlockingInfo()) {
			String jsonStatus = ResumeInfo.getResumeInfo(XMLTask.getXMLTask(task.getTaskID()), task);
			this.OUT.println(jsonStatus);
			this.OUT.flush();
		}
	}
}
