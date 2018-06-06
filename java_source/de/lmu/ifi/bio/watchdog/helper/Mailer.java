package de.lmu.ifi.bio.watchdog.helper;

import java.io.File;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;

import de.lmu.ifi.bio.watchdog.executor.HTTPListenerThread;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.runner.XMLBasedWatchdogRunner;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.task.TaskStatus;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask;

/**
 * Does inform the user when tasks are finished / failed...
 * @author Michael Kluge
 *
 */
public class Mailer {
	
	public static final String INSTANCE_KEY =  new BigInteger(130, new SecureRandom()).toString(32);
	private static final String PROTOCOL = "http://";
	public static final String PARAMS = "?";
	private static final String PORT_SEP = ":";
	public static final String IS = "=";
	public static final String HASH = "verify";
	public static final String COUNTER = "counter";
	public static final String INVALIDATE = "invalidate";
	private static final String AND = "&";
	private static final String TAB = "\t";
	private static final String SLASH = "/";
	private static final String NEWLINE = System.lineSeparator();
	private static final String SMTP_PROP = "mail.smtp.host";
	private static final String SMTP_PW = "mail.smtp.pw";
	private static final String SMTP_USER = "mail.smtp.user";
	private static final String SMTP_FROM = "mail.smtp.from";
	private static String HOST = "localhost";
	private static final String MAIL_HOST = "localhost";
	private final String FROM;
	private final String MAIL;
	private final Session SESSION;
	private boolean wasShutdownCommanded = false;
	public static String HOST_PREFIX = PROTOCOL + HOST + PORT_SEP + XMLBasedWatchdogRunner.PORT;
	private final static Logger LOGGER = new Logger();
	private static long generatedLinks = 0;
	
	static {
		try {
			HOST = InetAddress.getLocalHost().getHostName();
			HOST_PREFIX = PROTOCOL + HOST + PORT_SEP + XMLBasedWatchdogRunner.PORT;
		}
		catch (Exception e) { }
	}
	
	
	public static void updatePort(int p) {
		HOST_PREFIX = HOST_PREFIX.replaceFirst(PORT_SEP + "[0-9]+$", PORT_SEP + p);
	}


	/**
	 * Constructor
	 * @param mail
	 */
	public Mailer(String mail) {
		this.MAIL = mail;
		this.FROM = "watchdog@bio.ifi.lmu.de";
		Properties properties = System.getProperties();
	    properties.setProperty(SMTP_PROP, MAIL_HOST);
		this.SESSION = Session.getDefaultInstance(properties);
	}
	
	/**
	 * Constructor with more advanced config
	 * @param mail
	 * @param mailConfig
	 */
	public Mailer(String mail, File mailConfig) {
		this.MAIL = mail;
		String user = null;
		String from = null;
		String pw = null;
		Properties properties = new Properties();
		
		// try to read all lines
		try {
			List<String> lines = Files.readAllLines(mailConfig.toPath());
			for(String l : lines) {
				String[] t = l.split(TAB);
				if(t.length != 2) { 
					LOGGER.error("Lines in the mail config file must match the pattern key<TAB>value. Line that does not match that pattern: ");
					LOGGER.error(l);
					System.exit(1);
				}
				else {
					// do not take the pw in the prop. object
					if(SMTP_PW.equals(t[0])) {
						pw = t[1];
					}
					else if(SMTP_USER.equals(t[0])) {
						user = t[1];
					} 
					else {
						properties.put(t[0], t[1]);
						
						if(SMTP_FROM.equals(t[0])) {
							from = t[1];
						} 
					}
				}
			}
		}
		catch(Exception e) {
			LOGGER.error("Failed to open mail config file '"+mailConfig+"'.");
			e.printStackTrace();
			System.exit(1);
		}
		
		if(from == null) {
			LOGGER.error("Please enter the from attribute using '"+SMTP_FROM+"'.");
			this.FROM = null;
			System.exit(1);
		}	
		else
			this.FROM = from;

		// create authenticator if needed
		Authenticator auth = null;
		if(user != null && pw != null) {
			auth = new MailPWStore(user, pw);
		}
		
		this.SESSION = Session.getDefaultInstance(properties, auth);
	}
	
	/**
	 * Tries to send a mail
	 * @param subject
	 * @param text
	 * @return
	 */
	private boolean sendMail(String subject, String text) {
		try {
			MimeMessage message = new MimeMessage(this.SESSION);
			message.setFrom(new InternetAddress(this.FROM));
			message.setSentDate(new Date());
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(this.MAIL));
			message.setSubject(subject);
			message.setText(text);
			Transport.send(message);
			return true;
		}
		catch (MessagingException mex) {
			LOGGER.error("Mailer: " + mex.getMessage());
			mex.printStackTrace();
		}
		return false;
	}
	
	/**
	 * informs the user when a XML task is ignore cause by ignored dependencies
	 * @param x
	 * @return
	 */
	public boolean informIgnoreDependencies(XMLTask x) {
		String main = "[WATCHDOG] XML-Task " + x.getXMLID() + " is ignored";
		StringBuffer info = new StringBuffer();
		info.append("XML-Task with ID " + x.getXMLID() + " (" + x.getTaskName() + ") is ignored caused by ignored dependencies and will not spawn any more tasks.");
		info.append(NEWLINE);
		info.append("Spawned tasks: " + x.getNumberOfSpawnedTasks());
		return this.sendMail(main, info.toString());
	}
	
	/**
	 * informs the user about the task
	 * @param t
	 * @return
	 */
	public boolean inform(Task t) {
		if(this.wasWatchdogShutdown())
			return true;
		TaskStatus s = t.getStatus();
		if(s == null)
			return false;
		
		String host = t.getHost();
		if(host == null)
			host = "n.a.";

		StringBuffer info = new StringBuffer();
		info.append("Task: " + t.getID() + ", " + t.getName() + NEWLINE);
		info.append("Status: " + s + (t.isBlocked() ? " - is blocked" : "") + NEWLINE);
		info.append("Host: " + host + NEWLINE); 
		info.append("Command: " + t.getBinaryCall() + " " + String.join(" ", t.getArguments()) + NEWLINE + NEWLINE);
		
		// add request to unblock that job
		if(t.isBlocked()) { 
			String link = getLink(ControlAction.RELEASE, false, t.getID(), true);
			info.append("All jobs which depend on this task are on hold because a checkpoint is defined for this task." + NEWLINE);
			info.append("Release the block: "+ link + NEWLINE + NEWLINE);
		}		

		// add more detailed information if job failed
		if(!TaskStatus.FINISHED.equals(s) || t.hasErrors()) {
			try {
				info.append("Aborted job: " + t.getJobInfo().wasAborted() + NEWLINE);
				if(!t.getJobInfo().wasAborted()) {
					if(t.getJobInfo().getExitStatus() >= 0) 
						info.append("Exit code: " + t.getJobInfo().getExitStatus() + " (" + ReadExitCodes.getNameOfExitCode(t.getJobInfo().getExitStatus()) + ")" + NEWLINE);
					
					if(t.getJobInfo().hasSignaled())
						info.append("Signal: " + t.getJobInfo().getTerminatingSignal() + NEWLINE);
				}
			}
			catch(Exception ex) { ex.printStackTrace();}
			
			// add option to restart the job
			String linkRestart = getLink(ControlAction.RESTART, false, t.getID(), true);
			String linkModify = getLink(ControlAction.DISPLAY, false, t.getID(), false);
			String linkIgnore = getLink(ControlAction.IGNORE, false,t.getID(), true);
			String linkResolve = getLink(ControlAction.RESOLVE, false,t.getID(), true);
			info.append("You can fix the errors, if possible, and then restart the job. Alternatively you can change the parameters for this job and schedule it again or ignore it completely during the further processing." + NEWLINE);
			info.append("Restart the failed task: "+ linkRestart + NEWLINE);
			info.append("Modify the parameters of the failed task: "+ linkModify + NEWLINE);
			info.append("Ignore the task: "+ linkIgnore + NEWLINE);
			info.append("Mark the task as resolved: "+ linkResolve + NEWLINE + NEWLINE);
	
			info.append("Errors: (" + t.getErrors().size() + ")" + NEWLINE);
			for(String e : t.getErrors()) 
				info.append(e + NEWLINE);
			
			info.append(NEWLINE);
		}
		
		// add info about the used resources
		if(t.getUsedResources().size() > 0) {
			info.append("Used resources:" + NEWLINE);
			for(String key : t.getUsedResources().keySet()) {
				if(!(key.equals("start_time") || key.equals("end_time") || key.equals("submission_time"))) {
					Double v = t.getUsedResources().get(key);
					if(v > 0.0) {
						info.append(key + ": " + v + NEWLINE);			
					}
				}
			}
		}
		String main = "[WATCHDOG] " + t.getID() + ", " + t.getName() + " " + s;
		if(t.isBlocked())
			main = "[WATCHDOG] " + t.getID() + ", " + t.getName() + " " + "needs to be released";
		return this.sendMail(main, info.toString());
	}

	private boolean wasWatchdogShutdown() {
		return this.wasShutdownCommanded;
	}
	
	/**
	 * can be called from outside to avoid status mails after a watchdog run was canceled by the user
	 */
	public void setOrderedShutdown() {
		this.wasShutdownCommanded = true;
	}

	/**
	 * informs about a complete process block
	 * @param tasks
	 */
	public boolean inform(ArrayList<Task> tasks) {
		if(this.wasWatchdogShutdown())
			return true;
		
		StringBuffer info = new StringBuffer();
		StringBuffer error = new StringBuffer();
		LinkedHashMap<String, Double> res = new LinkedHashMap<>();
		LinkedHashMap<Task, String> commands = new LinkedHashMap<>();
		TaskStatus overallStatus = TaskStatus.FINISHED;
		int failed = 0;
		boolean isBlocked = false;
		ArrayList<String> blockedTaskIDs = new ArrayList<>();
		ArrayList<String> errorTaskIDs = new ArrayList<>();
		
		for(Task t : tasks) {
			TaskStatus s = t.getStatus();
			if(s == null)
				continue;
			
			commands.put(t, t.getBinaryCall() + " " + String.join(" ", t.getArguments()));
			
			if(t.isBlocked()) {
				blockedTaskIDs.add(t.getID());
				isBlocked = true;
			}
			
			// add more detailed information if job failed
			if((!t.hasTaskFinished() && !t.isBlocked()) || t.hasErrors()) {
				errorTaskIDs.add(t.getID());
				failed++;
				overallStatus = TaskStatus.FAILED;
				error.append("Job with ID " + t.getID() + ", " + t.getName() + " failed:" + NEWLINE);
				String host = t.getHost();
				if(host == null)
					host = "n.a.";
				try {
					error.append("Status: " + s + NEWLINE);
					error.append("Host: " + host + NEWLINE);
					error.append("Aborted job: " + t.getJobInfo().wasAborted() + NEWLINE);
					if(!t.getJobInfo().wasAborted()) {
						if(t.getJobInfo().getExitStatus() >= 0) 
							error.append("Exit code: " + t.getJobInfo().getExitStatus()  + " (" + ReadExitCodes.getNameOfExitCode(t.getJobInfo().getExitStatus()) + ")" + NEWLINE);
					
						if(t.getJobInfo().hasSignaled())
							error.append("Signal: " + t.getJobInfo().getTerminatingSignal() + NEWLINE);
					}
				}
				catch(Exception ex) { ex.printStackTrace();}
				
				error.append("Errors: (" + t.getErrors().size() + ")" + NEWLINE);
				for(String e : t.getErrors()) 
					error.append(e + NEWLINE);	
				
				error.append("------------------------------------" + NEWLINE);
			}
			// try to sum up the info on used resources
			for(String key : t.getUsedResources().keySet()) {
				if(!(key.equals("start_time") || key.equals("end_time") || key.equals("submission_time"))) {
					
					if(!res.containsKey(s))
						res.put(key, 0.0);
					
					// sum up the values
					Double value = t.getUsedResources().get(key);
					res.put(key, res.get(key) + value);
				}
			}
		}

		// build up the final message
		if(TaskStatus.FINISHED.equals(overallStatus)) {
			info.append("All " + tasks.size() + " jobs finished successfully." + NEWLINE + NEWLINE);
		}
		else {
			info.append(failed + " of " + tasks.size() + " jobs have NOT finished successfully."+ NEWLINE + NEWLINE);
		}
		
		// add request to unblock that job
		if(isBlocked) { 
			String taskIDs = StringUtils.join(blockedTaskIDs, HTTPListenerThread.SEP);
			String link = getLink(ControlAction.RELEASE, false, taskIDs, true);
			info.append("All jobs which depend on this task are on hold because a checkpoint is defined for this task." + NEWLINE);
			info.append("Release the block: "+ link + NEWLINE + NEWLINE);
		}

		for(Task t : commands.keySet())
			info.append("block parameter: " + t.getDisplayGroupFileName() + " [" + (!TaskStatus.FINISHED.equals(t.getStatus()) ? "FAILED" : "OK") + "]; command: " + commands.get(t) + NEWLINE);
		
		info.append(NEWLINE);
		// add info about the used resources
		if(res.size() > 0) {
			info.append("Used resources:" + NEWLINE);
			for(String key : res.keySet()) {
				if(res.get(key) > 0.0) {
					info.append(key + ": " + res.get(key) + NEWLINE);
				}
			}
			
			info.append(NEWLINE);
		}
		
		if(error.length() > 0) {
			info.append("You can fix the errors, if possible, and then restart the job. Alternatively you can change the parameters for this job and schedule it again." + NEWLINE);
			for(String taskID : errorTaskIDs) {
				// add option to restart the job
				String linkRestart = getLink(ControlAction.RESTART, false,taskID, true);
				String linkModify = getLink(ControlAction.DISPLAY, false, taskID, false);
				String linkIgnore = getLink(ControlAction.IGNORE, false, taskID, true);
				String linkResolve = getLink(ControlAction.RESOLVE, false, taskID, true);
								
				info.append(taskID + ": restart the failed task: "+ linkRestart + NEWLINE);
				info.append(taskID + ": modify the parameters of the failed task: "+ linkModify + NEWLINE);
				info.append(taskID + ": ignore the task: "+ linkIgnore + NEWLINE);
				info.append(taskID + ": mark the task as resolved: "+ linkResolve + NEWLINE + NEWLINE);
			}
			// add the errors		
			info.append(error);
		}

		String main = "";
		if(tasks.iterator().hasNext()) {
			if(isBlocked)
				main = "[WATCHDOG] " + tasks.iterator().next().getTaskID() + ", " + tasks.iterator().next().getName() + " " + "needs to be released";
			else 
				main = "[WATCHDOG] " + tasks.iterator().next().getTaskID() + ", " + tasks.iterator().next().getName() + " " + overallStatus;
		}
		else
			return true; // do not send any mail!
		
		// send the mail!
		return this.sendMail(main, info.toString());
	}
	
	/**
	 * notify the user that a XML task is waiting for parameter confirmation
	 * @param x
	 */
	public void notifyParamConfirmation(XMLTask x) {
		String main = "[WATCHDOG] " + x.getXMLID() + ", " + x.getTaskName() + " is waiting for parameter confirmation";
		StringBuffer info = new StringBuffer("Task with the name is blocked for execution until you confirm the parameters of it:" + NEWLINE + NEWLINE);
		info.append(getLink(ControlAction.DISPLAY, true, Integer.toString(x.getXMLID()), false));
		this.sendMail(main, info.toString());
	}
	
	/**
	 * send a welcome message
	 */
	public boolean hello(String xmlFile) {
		String main = "[WATCHDOG] Processing was started";
		return this.sendMail(main, Mailer.getHelloTxt(xmlFile));
	}
	
	/**
	 * text of the welcome message
	 * @param xmlFile
	 * @return
	 */
	public static String getHelloTxt(String xmlFile) {
		StringBuffer text = new StringBuffer("Your workflow is currently processed: " + xmlFile);
		text.append(NEWLINE);
		text.append(NEWLINE);
		text.append("Check status of tasks: ");
		text.append(getLink(ControlAction.LIST, true, null, false));
		text.append(NEWLINE);text.append(NEWLINE);
		text.append("Terminate watchdog and abort all running tasks: ");
		text.append(getLink(ControlAction.TERMINATE, true, null, false));
		return text.toString();
	}
	
	/**
	 * sends a goodbye message, when processing of the XML file ends
	 * @param xmlTasks
	 */
	public void goodbye(ArrayList<XMLTask> xmlTasks) {
		String main = "[WATCHDOG] Processing ended";
		this.sendMail(main, Mailer.getGoodbyeTxt(xmlTasks));
	}
	
	/**
	 * text of the welcome message
	 * @param xmlFile
	 * @return
	 */
	public static String getGoodbyeTxt(ArrayList<XMLTask> xmlTasks) {
		StringBuffer text = new StringBuffer("Processing ended!");
		text.append(NEWLINE);
		text.append(NEWLINE);
		text.append("Number of spawned and executed tasks: ");
		text.append(NEWLINE);
		// get information from all tasks
		for(XMLTask x : xmlTasks) {
			text.append(x.getTaskName());
			text.append(" (id: " + x.getXMLID() + "): ");
			text.append(x.getNumberOfSpawnedTasks());
			if(x.getNumberOfSpawnedTasks() > 0)
				text.append(" (" + x.getSummedStatus().toString().replaceAll("[\\{\\}]", "").replace("=", ": ") + ")");
			text.append(NEWLINE);
		}
		return text.toString();
	}
	
	/**
	 * adds the parameters which are needed for a link
	 * @param action
	 * @param isXmlID
	 * @param ids
	 * @return
	 */
	public static HashMap<String, String> getParameter4Link(ControlAction action, boolean isXmlID, String ids, boolean invalidateAfterFirstUse) {
		HashMap<String, String> parameter = new HashMap<>();
		if(ids != null)
			parameter.put(isXmlID ? HTTPListenerThread.XML_ID : HTTPListenerThread.TASK_ID, ids);
		parameter.put(HTTPListenerThread.ACTION, action.name());
		
		long counter = Mailer.generatedLinks++;
		parameter.put(COUNTER, Long.toString(counter));
		parameter.put(INVALIDATE, Boolean.toString(invalidateAfterFirstUse));
		return parameter;
	}

	/**
	 * constructs a link
	 * @param action
	 * @param isXmlID
	 * @param ids
	 * @return
	 */
	public static String getLink(ControlAction action, boolean isXmlID, String ids, boolean invalidateAfterFirstUse) {
		return getLink(action, isXmlID, ids, null, invalidateAfterFirstUse);
	}

	/**
	 * constructs a link
	 * @param action
	 * @param isXmlID
	 * @param ids
	 * @param additionalParams
	 * @return
	 */
	public static String getLink(ControlAction action, boolean isXmlID, String ids, HashMap<String, String> additionalParams, boolean invalidateAfterFirstUse) {
		String uri = SLASH + action.name() + SLASH;
		HashMap<String, String> parameter = getParameter4Link(action, isXmlID, ids, invalidateAfterFirstUse);
		if(additionalParams != null)
			parameter.putAll(additionalParams);
		
		// get hash and add it
		String params = getParamString(parameter, true);
		String hash = generateHash(uri, params);
		parameter.put(HASH, hash);
		String link = HOST_PREFIX + uri + PARAMS + getParamString(parameter, false);
		String url = link;
		try { url = new URL(link).toURI().toASCIIString(); } catch(Exception e) {}
		return url;
	}
	
	/**
	 * Build the parameter part of the string
	 * @param parameter
	 * @return
	 */
	public static String getParamString(Map<String, String> parameter, boolean exculdeSettings) {
		StringBuffer params = new StringBuffer();
		String n = null;
		ArrayList<String> keys = new ArrayList<>(parameter.keySet());
		
		// exclude the setting parameters from the hash as they may change
		if(exculdeSettings) {
			for(String name : parameter.keySet()) {
				if(name.startsWith(HTTPListenerThread.SETTINGS))
					keys.remove(name);
			}
		}
		
		Collections.sort(keys);
		for(Iterator<String> it = keys.iterator(); it.hasNext();) {
			n = it.next();
			params.append(n);
			params.append(IS);
			params.append(parameter.get(n));
			if(it.hasNext())
				params.append(AND);
		}
		return params.toString();
	}
	
	/**
	 * generates a sha2 has which is only valid in that instance
	 * @param uri
	 * @param parameter
	 * @return
	 */
	public static String generateHash(String uri, String parameter) {
		StringBuffer testUri = new StringBuffer();
		// add uri
		testUri.append(uri);
		// add parameter
		testUri.append(parameter);
		// add instance specific key
		testUri.append(Mailer.INSTANCE_KEY);
		String hash = Mailer.INSTANCE_KEY;
		try { hash = Functions.getHash(testUri.toString()); } catch(Exception e) { e.printStackTrace(); }
		return hash;
	}
	
	public boolean hasMail() {
		return this.MAIL != null && this.MAIL.length() > 0;
	}
	
	public String getMail() {
		return this.MAIL;
	}
	
	/**
	 * tests, if that uri was generated by the instance of the program which is currently running
	 * @param uri
	 * @param params
	 * @param given hash
	 * @return
	 */
	public static boolean verifyLink(String uri, Map<String, String> params, String givenHash) {
		return givenHash.equals(generateHash(uri, getParamString(params, true)));
	}
	
	public static boolean validateMail(String mail) {
		try {
			InternetAddress internetAddress = new InternetAddress(mail);
			internetAddress.validate();
		} catch(Exception ex) { return false; }
		return true;
	}
}
