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
	public static final String LAST_PART_OF_ENDING = "resume";
	public static final String LOG_ENDING = ".watchdog." + LAST_PART_OF_ENDING;
	
	private final Logger LOGGER = new Logger();
	private PrintWriter OUT;
	
	/**
	 * Constructor that will automatically open a file with a timestamp suffix
	 * @param file
	 * @param updateResumeFile if false, LOG_ENDING suffix will be added; if true, file will be updated
	 */
	public WorkflowResumeLogger(File file, boolean updateResumeFile) {
		String fname = generateResumeFilename(file, updateResumeFile);
		try {
			this.OUT = new PrintWriter(new BufferedWriter(new FileWriter(fname, updateResumeFile)));
		}
		catch(IOException e) {
			this.LOGGER.error("Failed to write to file '"+fname+"'.");
			System.exit(1);
		}
	}
	
	public static String generateResumeFilename(File file, boolean updateResumeFile) {
		String fname = file.getAbsolutePath();
		if(!updateResumeFile) {
			String time = SDF.format(new Date());
			String basename = file.getAbsolutePath().replaceFirst(ENDING, "");
			fname = basename + "_" + time + LOG_ENDING;
		}
		return fname;
	}

	@Override
	public void handle(Task task) {
		if(task.hasTaskFinishedWithoutBlockingInfo() && (task.getJobInfo() instanceof ResumeJobInfo == false)) {
			String jsonStatus = ResumeInfo.getResumeInfo(XMLTask.getXMLTask(task.getTaskID()), task);
			this.OUT.println(jsonStatus);
			this.OUT.flush();
		}
	}
}
