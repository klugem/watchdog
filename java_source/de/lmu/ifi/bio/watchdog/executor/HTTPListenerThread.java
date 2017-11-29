package de.lmu.ifi.bio.watchdog.executor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.helper.ControlAction;
import de.lmu.ifi.bio.watchdog.helper.HTMLHelper;
import de.lmu.ifi.bio.watchdog.helper.Mailer;
import de.lmu.ifi.bio.watchdog.helper.returnType.BooleanReturnType;
import de.lmu.ifi.bio.watchdog.helper.returnType.ReturnType;
import de.lmu.ifi.bio.watchdog.slave.Master;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.task.TaskStatus;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask;
import fi.iki.elonen.NanoHTTPD;
import sun.misc.Signal;

/**
 * Listens to commands received by http GET requests
 * @author Michael Kluge
 *
 */
public class HTTPListenerThread extends NanoHTTPD {
	private static final Signal SIGTERM = new Signal("TERM"); // kill request
	
	private static final String MINUS = "-";
    private static final String QUERY_STRING_PARAMETER = "NanoHttpd.QUERY_STRING";
	public static final String WEB_BASE = "webserver_data";
	public static final String MIME_JS = "application/javascript";
	public static final String MIME_CSS = "text/css";
	public static final String MIME_PNG = "image/png";
	public static final String CSS = ".css";
	public static final String JS = ".js";
	public static final String PNG = ".png";
	
	public static final String TASK_ID = "task";
	public static final String TASK_BLOCK = "block";
	public static final String ACTION = "action";
	public static final String XML_ID = "xml";
	public static final String SEP = ";";
	public static final String NEWLINE = System.lineSeparator();
	public static final String SETTINGS = "settings_";
	public static final String CHECK_BOX_IDENTIFIER = "#@CHECK|CHECK@#";
	public static final String ACTION_NAME = "actionName";
	public static final String NO_MODIFY = "noModifyAction";
	public static final String RELOAD = "reloadAfterAction";
	public static final String REFERER = "referer";
	
	public static final String LIST_ACTION = "listAction";
	private static final String RUNNING_TASKS = "runningTasks";
	private static final String COMPLETED_TASKS = "completedTasks";
	private static final String FAILED_TASKS = "failedTasks";
	private static final String ALL_TASKS = "allTasks";
	private static final String WAITING_TASKS = "waitingTasks";
	private static final String RESOLVED_TASKS = "resolvedTasks";
	private static final String IGNORED_TASKS = "ignoredTasks";
	
	public final String BASE;
	private final LinkedHashMap<String, Task> VALID_TASKS = new LinkedHashMap<>();
	private final ArrayList<XMLTask> XML_TASKS;
	private Response customResponse = null;
	private final HashSet<String> INVALID_URLS = new HashSet<>();
	
	/**
	 * Constructor
	 * @param port
	 * @param tasks
	 * @param xmlTasks
	 */
	public HTTPListenerThread(int port, final ArrayList<XMLTask> xmlTasks, String watchdogBase) {
		super(port);
		this.XML_TASKS = xmlTasks;
		this.BASE = watchdogBase;
	}
	
	public synchronized void registerNewTask(Task t) {
		this.VALID_TASKS.put(t.getID(), t);
	}
	
	@Override
	public Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> parms, Map<String, String> files) {
		String referer = headers.get(REFERER);

		// check, if a normal document request
		try {
			if(uri.endsWith(JS)) {
				InputStream in = new FileInputStream(BASE + File.separator + WEB_BASE + File.separator + uri);
				return new NanoHTTPD.Response(Response.Status.OK, MIME_JS, in);
			}
			else if(uri.endsWith(CSS)) {
				InputStream in = new FileInputStream(BASE + File.separator + WEB_BASE + File.separator + uri);
				return new NanoHTTPD.Response(Response.Status.OK, MIME_CSS, in);
			}
			else if(uri.endsWith(PNG)) {   
				InputStream in = new FileInputStream(BASE + File.separator + WEB_BASE + File.separator + uri);
				return new NanoHTTPD.Response(Response.Status.OK, MIME_PNG, in);
			}
		}
		catch(Exception e) {
			return new NanoHTTPD.Response(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Resource '"+uri+"' was not found on the server.");
		}

		// check, if action should be modified because of submit button
		ControlAction action = null;
		for(ControlAction c :ControlAction.values()) {
			if(parms.containsKey(c.name())) {
				action = c;
				parms.remove(c.name());
				break;
			}
		}

		// get the settings
		HashMap<String, String> settings = new HashMap<>();
		// get the action and check if it is valid
		String actionString = null;
		HashMap<String, String> noLinkVerification = new HashMap<>(); // these values are not secured by the verification ID!
		if(action == null) {
			actionString = parms.get(ACTION);
			action = ControlAction.getType(actionString);
			
			// checks, if a user interface action is performed
			if(action != null && ControlAction.USERINTERFACE_ACTION.name().equals(action.name())) {
				noLinkVerification.put(ACTION_NAME, parms.remove(ACTION_NAME));
				// test, if we are in display mode
				if(ControlAction.DISPLAY.name().equals(noLinkVerification.get(ACTION_NAME))) {
					settings.put(NO_MODIFY, NO_MODIFY);
				}
				settings.put(RELOAD, null);
			}
		}
		else {
			actionString = action.name();
		}
		// remove settings because they are not secured by the hash
		for(String name : new ArrayList<String>(parms.keySet())) {
			if(name.startsWith(SETTINGS))
				settings.put(name.replaceFirst(SETTINGS, ""), parms.remove(name));
		}
		
		// test, if action command is there
		if(action == null && !parms.containsKey(ACTION)) 
			return new Response(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "No action was set using '"+ACTION+"'!");
		
		// test, if a hash is given
		if(!parms.containsKey(Mailer.HASH) && action != null)
			return new Response(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Verification id was not found. Do not use old links or manipulte them!");
	
		// test, if the KEY for the URI was generated by that instance or if a parameter was modified
		String hash = parms.remove(Mailer.HASH);
		parms.remove(QUERY_STRING_PARAMETER);

		if(action != null && !Mailer.verifyLink(uri, parms, hash)) {
			return new Response(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Verification id not valid. Do not use old links or manipulte them!");
		}
			
		// test, if the link is valid
		if(this.INVALID_URLS.contains(parms.get(Mailer.COUNTER))) {
			return new Response(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "The link you used was only valid one time!");
		}
	
		// re-add the parameters which are not secured by the verification ID
		parms.putAll(noLinkVerification);		
		
		// test, if task id is there
		if(!parms.containsKey(TASK_ID) && !parms.containsKey(XML_ID) && (action != null && !action.requiresNoID()))
				return new Response(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "No task ID or XML ID was given using '"+TASK_ID+"' or '"+XML_ID+"'!");
		
		if(action == null)
			return new Response(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Action '"+actionString+"' is not valid!");
		else {
			// check, if the URL should be only valid one time!
			if(Boolean.parseBoolean(parms.get(Mailer.INVALIDATE))) {
				INVALID_URLS.add(parms.get(Mailer.COUNTER));
			}
			
			boolean allOk = true;
			StringBuffer message = new StringBuffer();
			// display the settings because it is needed...
			if(action.isModifyAction() && settings.size() == 0) {
				action = ControlAction.DISPLAY;
			}

			Task performTask = null;
			// test, if a list action
			if(action.isListAction()) {
				this.performListAction(parms);
			}
			else if(action.isTerminateAction()) {
				Signal.raise(SIGTERM); // send kill request
			}
			// test, if a task ID is given
			else if(parms.containsKey(TASK_ID)) {
				String[] taskIDs = parms.get(TASK_ID).split(SEP);

				for(String tid : taskIDs) {
					// test, if task with that ID exists
					if(!this.VALID_TASKS.containsKey(tid)) {
						String groupFilename = parms.get(TASK_BLOCK);
						for(Task t : this.VALID_TASKS.values()) {
							if(Integer.toString(t.getTaskID()).equals(tid) && ((groupFilename == null && t.getGroupFileName() == null) || (groupFilename.equals(t.getGroupFileName())))) {
								performTask = t;
								continue;
							}
						}
					}
					// get the task directly from the list
					else 
						performTask = this.VALID_TASKS.get(tid);
					
					// test, if the task could be identified
					if(performTask != null) {
						// perform the action
						if(!this.performAction(performTask, action, settings, new HashMap<String, String>(parms))) {
							allOk = false;
							message.append("Action '"+action.name()+"' failed for task '"+tid+"'! Probably because that action was executed before or because the parameters are invalid." + NEWLINE);
						}
					}
					else 
						return new Response(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "No task with '"+tid+"' exists!");		
				}
				
			}
			// we got an XML ID or several
			else if(parms.containsKey(XML_ID)) {
				String xmlID = parms.get(XML_ID);
				int xid = Integer.parseInt(xmlID);
				// test, if xml task with that ID exists	
				XMLTask x = findXMLTask(xid);
				if(x == null)
					return new Response(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "No xml task with '"+xid+"' exists!");
												
				// perform the action
				if(!this.performAction(x, action, settings, null)) {
					allOk = false;
					message.append("Action '"+action.name()+"' failed for xml task '"+xid+"'! Probably because that action was executed before or because the parameters are invalid." + NEWLINE);
				}
			}
			
			// reset the response and return it
			if(this.customResponse != null) {
				Response r = this.customResponse;
				if(settings.containsKey(RELOAD)) {
					if(settings.get(RELOAD) != null) {
						referer = settings.get(RELOAD);
						String addReload = "<meta http-equiv=\"refresh\" content=\"3;URL="+referer+"\"/>";
						try { 
							int n = r.getData().available();
							byte[] bytes = new byte[n];
							r.getData().read(bytes, 0, n);
							String old = new String(bytes, StandardCharsets.UTF_8);
							r = new Response(Response.Status.BAD_REQUEST, MIME_HTML, new ByteArrayInputStream((addReload + old).getBytes("UTF-8"))); 
						}
						catch(Exception e) {};
					}
				}
				this.customResponse = null;
				return r;
			}
			else {
				// return something if no custom response was set
				if(allOk) {
					String addReload = "";
					if(settings.containsKey(RELOAD)) {
						if(settings.get(RELOAD) != null)
							referer = settings.get(RELOAD);
						addReload = "<meta http-equiv=\"refresh\" content=\"3;URL="+referer+"\"/>";
					}
						
					return new Response(Response.Status.OK, MIME_HTML, addReload + "Action '"+ (noLinkVerification.containsKey(ACTION_NAME) ? noLinkVerification.get(ACTION_NAME) : action.getActionName()) +"' was executed successfully.");
				}
				else
					return new Response(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, message.toString());
			}			
		}
	}
	
	/**
	 * Performs an action on a task
	 * @param t
	 * @param action
	 * @param settings
	 * @return true, if action could be performed successfully and fails if not
	 */
	public boolean performAction(Task t, ControlAction action, HashMap<String, String> settings, HashMap<String, String> params) {
		if(action.isUserinteraceAction()) {		
			// get the action that should be executed.
			action = ControlAction.getType(params.get(ACTION_NAME));
			if(action == null)
				return false;

			// 1) check, if action is valid
			if(!getPossibleActions(t).contains(action)) {
				return false;
			}
			
			// 2) perform it!
			if(ControlAction.RELEASE_RESOURCE_RESTRICTIONS.name().equals(action.name())) {
				t.setConsumeResources(false);
			}
			else if(ControlAction.TERMINATE_TASK.name().equals(action.name())) {
				t.terminateTask();
				return true;
			}
			else if(ControlAction.RESTART.name().equals(action.name())) {
				t.restartTask();
				return true;
			}
			else if(ControlAction.DISPLAY.name().equals(action.name())) {
				return this.performAction(t, ControlAction.DISPLAY, settings, params);
			}
			else if(ControlAction.IGNORE.name().equals(action.name())) {
				return this.performAction(t, ControlAction.IGNORE, settings, params);
			}
			else if(ControlAction.RESOLVE.name().equals(action.name())) {
				return this.performAction(t, ControlAction.RESOLVE, settings, params);
			}
			else if(ControlAction.RELEASE.name().equals(action.name())) {
				return t.releaseTask();
			}
			else if(ControlAction.MODIFY.name().equals(action.name())) {
				settings.remove(NO_MODIFY); // ensure that is parameter is not set
				return this.performAction(t, ControlAction.DISPLAY, settings, params);
			}
			// 3) display the list view again
			this.performListAction(params);
			return true;
		} 
		else if(action.isReleaseAction()) {
			return t.releaseTask();
		}
		else if(action.isRestartAction()) {
			return t.restartTask();
		}
		else if(action.isIgnoreAction()) {
			t.setStatus(TaskStatus.IGNORE);
			// remove it, if it is in slave mode
			XMLTask.deleteSlaveID(t);
			Master.unregisterTask(t);
			return true;
		}
		else if(action.isResolveAction()) {
			XMLTask x = findXMLTask(t.getTaskID());
			if(x != null) {
				// check, if return parameters are needed
				if(x.getReturnParameters().size() == 0) {
					t.setStatus(TaskStatus.RESOLVED);
					// remove it, if it is in slave mode
					XMLTask.deleteSlaveID(t);
					Master.unregisterTask(t);
					return true;
				}
				// display the formular that can be used to enter the stuff
				else {
					String link = Mailer.getLink(ControlAction.RESOLVE_RETURN_USERINTERFACE, true, t.getID(), true);
					String uri = link.substring(0, link.indexOf(Mailer.PARAMS)).replace(Mailer.HOST_PREFIX, "");
					link = link.replaceFirst("&"+Mailer.HASH+"=.{64}", "");
					// remove verification id
					// add the parameter which are already in use in the link
					HashMap<String, String> parameter = new HashMap<>();
					parameter.putAll(Mailer.getParameter4Link(ControlAction.RESOLVE_RETURN_USERINTERFACE, true, t.getID(), true));
	
					StringBuffer buf = new StringBuffer("<html><head>");
					buf.append("<link rel=\"stylesheet\" href=\"/css/style.css\">");
					buf.append("<script src=\"/js/functions.js\"></script>");
					buf.append("</head><body><form action=\""+link+"\" method=\"POST\">");
					
					// add task parameter to the form
					if(t != null) {
						buf.append("<input type=\"hidden\" name=\""+ TASK_ID+"\" value=\""+t.getTaskID()+"\">");
						buf.append("<input type=\"hidden\" name=\""+ TASK_BLOCK+"\" value=\""+t.getGroupFileName()+"\">");
						buf.append("<div class=\"warning\">Please enter the values of the return parameters.</div>");
						parameter.put(TASK_ID, Integer.toString(t.getTaskID()));
						parameter.put(TASK_BLOCK, t.getGroupFileName());
					}
					
					// start the table
					buf.append("<table id=\"paramTable\"><thead><tr><td>Name</td><td>Type</td><td>Value</td></tr></thead><tbody>");
	

					HashMap<String, ReturnType> retParams = x.getReturnParameters();					
					int i = 1;
					// add the parameters
					for(String name : retParams.keySet()) {
						ReturnType type = retParams.get(name);
						boolean flag = type instanceof BooleanReturnType;
						
						// add the name
						buf.append("<tr id=\""+i+"\">");
						buf.append("<td>");
						buf.append(name);
						buf.append("</td>");
						buf.append("<td>");
						buf.append(type.getType());
						buf.append("</td>");
						
						// add the value
						if(flag) 
							buf.append("<td><input type=\"checkbox\" name=\""+ SETTINGS + name+"\" checked=\"false\" value=\""+CHECK_BOX_IDENTIFIER+"\"></td>");
						else
							buf.append("<td><input type=\"text\" name=\""+ SETTINGS +name+"\" value=\"\"></td>");
						
						// add the end of the row
						buf.append("</tr>");
						i++;
					}
					buf.append("</tbody></table>");
					// get the hash
					long updateCounter = Long.parseLong(parameter.get(Mailer.COUNTER))-1;
					parameter.put(Mailer.COUNTER, Long.toString(updateCounter));
					String hash = Mailer.generateHash(uri, Mailer.getParamString(parameter, true));
	
					buf.append("<input type=\"hidden\" name=\""+ Mailer.HASH +"\" value=\""+hash+"\">");
					buf.append("<span id=\"counter\" style=\"visibility: hidden;\">"+i+"</span>");
					// add the submit buttons
					buf.append("<input type=\"submit\" value=\""+ControlAction.RESOLVE_RETURN_USERINTERFACE.getDescription()+"\" name=\""+ControlAction.RESOLVE_RETURN_USERINTERFACE+"\" />");
					buf.append("</form></body></html>");
					this.customResponse = new Response(Response.Status.OK, MIME_HTML, buf.toString());
					return false;
				}
			}
			return false;
		}
		else if(action.isResolveParameterEnterAction()) {
			XMLTask x = findXMLTask(t.getTaskID());
			if(x != null) {
				HashMap<String, ReturnType> retParams = x.getReturnParameters();
				// check the types
				for(String key : settings.keySet()) {
					String value = settings.get(key);
					ReturnType r = retParams.get(key);
					// set value to true in case of a checked checkbox!
					if(r instanceof BooleanReturnType && value.equals(CHECK_BOX_IDENTIFIER))	
						value = "true";
					
					if(r.checkType(value)) {
						// delete, the parameter, because all is ok
						retParams.remove(key);
					}
					// wrong format!
					else {
						this.customResponse = new Response(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Parameter with name '"+key+"' has not the correct type '"+r.getType()+"' ('"+value+"').");
						settings.put(RELOAD, Mailer.getLink(ControlAction.RESOLVE, false, t.getID(), true)); // redicrect after this set
						return false;
					}	
				}
				// delete the boolean values
				for(String key : new ArrayList<>(retParams.keySet())) {
					if(retParams.get(key) instanceof BooleanReturnType)
						retParams.remove(key);
				}
				
				// ensure that all params are there
				if(retParams.size() > 0) {
					this.customResponse = new Response(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Values for the following return parameters are missing: " + StringUtils.join(retParams.keySet(), ", "));
					settings.put(RELOAD, Mailer.getLink(ControlAction.RESOLVE, false, t.getID(), true)); // redicrect after this set
					return false;
				}

				t.setReturnParams(settings);
				t.setStatus(TaskStatus.RESOLVED);
				settings.put(RELOAD, getListUrl(ALL_TASKS)); // redicrect after this set
				// remove it, if it is in slave mode
				XMLTask.deleteSlaveID(t);
				Master.unregisterTask(t);
				return true;
			}
			return false;
		}
		else if(action.isDisplayAction() || action.isModifyAction()) {
			// find the corresponding XML task and execute the command in the XML section
			XMLTask x = this.findXMLTask(t.getTaskID());
			if(x != null)
				return this.performAction(x, action, settings, t);
			else 
				this.customResponse = new Response(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "No corresponding xml task for that task exists!");			
		}
		return false;
	}
	
	/**
	 * Performs an action on a XML task
	 * @param x
	 * @param action
	 * @param settings
	 * @param task t
	 * @return true, if action could be performed successfully and fails if not
	 */
	public boolean performAction(XMLTask x, ControlAction action, HashMap<String, String> settings, Task t) {
		if(action.isModifyAction()) {
			// try to modify the parameters and reschedule the task
			boolean ret = x.modifyParamsAndReschedule(settings, t, false);
			settings.put(RELOAD, getListUrl(ALL_TASKS)); // redicrect after this set
			return ret;
		}
		else if(action.isDisplayAction()) {
			String link = Mailer.getLink(ControlAction.MODIFY, true, Integer.toString(x.getXMLID()), true);
			String uri = link.substring(0, link.indexOf(Mailer.PARAMS)).replace(Mailer.HOST_PREFIX, "");
			link = link.replaceFirst("&"+Mailer.HASH+"=.{64}", "");
			// remove verification id
			// add the parameter which are already in use in the link
			HashMap<String, String> parameter = new HashMap<>();
			parameter.putAll(Mailer.getParameter4Link(ControlAction.MODIFY, true, Integer.toString(x.getXMLID()), true));
			
			StringBuffer buf = new StringBuffer("<html><head>");
			buf.append("<link rel=\"stylesheet\" href=\"/css/style.css\">");
			buf.append("<script src=\"/js/functions.js\"></script>");
			buf.append("</head><body><form action=\""+link+"\" method=\"POST\">");
			
			// add task parameter to the form
			if(t != null) {
				buf.append("<input type=\"hidden\" name=\""+ TASK_ID+"\" value=\""+t.getTaskID()+"\">");
				buf.append("<input type=\"hidden\" name=\""+ TASK_BLOCK+"\" value=\""+t.getGroupFileName()+"\">");
				if(!settings.containsKey(NO_MODIFY) && t.getGroupFileName().length() > 0)
					buf.append("<div class=\"warning\">Warning: The parameter change will only affect the task with the id '"+t.getID()+"'. All other tasks will stay unaffected!</div>");
				
				parameter.put(TASK_ID, Integer.toString(t.getTaskID()));
				parameter.put(TASK_BLOCK, t.getGroupFileName());
			}
			
			// start the table
			buf.append("<table id=\"paramTable\"><thead><tr>" + (!settings.containsKey(NO_MODIFY) ? "<td>Action</td>" : "") + "<td>Name</td><td>Value</td></tr></thead><tbody>");

			HashMap<String, Boolean> isFlag = new HashMap<>();
			LinkedHashMap<String, String> params = new LinkedHashMap<>();
			// get the parameter of that task
			LinkedHashMap<String, Pair<Pair<String, String>, String>> paramsDetail;
			if(t != null)
				paramsDetail = t.getDetailedArguments();
			// XML task
			else
				paramsDetail = x.getArguments();
			
			for(String name : paramsDetail.keySet()) {
				Pair<Pair<String, String>, String> p = paramsDetail.get(name);
				String value = p.getValue();
				isFlag.put(name, value == null);
				params.put(name, value);
			}
			
			int i = 1;
			// add the parameters
			for(String name : params.keySet()) {
				String value = params.get(name);
				boolean flag = isFlag.get(name);
				
				// add the name
				buf.append("<tr id=\""+i+"\">");
				buf.append("<td>");
				if(!settings.containsKey(NO_MODIFY)) {
					buf.append("<img src=\"/png/delete.png\" onclick=\"del('"+i+"')\" title=\"delete parameter\"/>");
					buf.append("</td><td>");
				}
				buf.append(name);
				buf.append("</td>");
				
				// add the value
				if(flag) 
					buf.append("<td><input type=\"checkbox\" name=\""+ SETTINGS + name+"\" checked=\"true\" value=\""+CHECK_BOX_IDENTIFIER+"\"></td>");
				else
					buf.append("<td><input type=\"text\" name=\""+ SETTINGS +name+"\" value=\""+value+"\"></td>");
				
				// add the end of the row
				buf.append("</tr>");
				i++;
			}
			buf.append("</tbody></table>");
			// get the hash
			long updateCounter = Long.parseLong(parameter.get(Mailer.COUNTER))-1;
			parameter.put(Mailer.COUNTER, Long.toString(updateCounter));
			String hash = Mailer.generateHash(uri, Mailer.getParamString(parameter, true));

			buf.append("<input type=\"hidden\" name=\""+ Mailer.HASH +"\" value=\""+hash+"\">");
			buf.append("<span id=\"counter\" style=\"visibility: hidden;\">"+i+"</span>");
			// add the submit buttons
			if(!settings.containsKey(NO_MODIFY)) {
				buf.append("<img src=\"/png/add.png\" onclick=\"add()\" title=\"add parameter\"/><br /><br />");
				buf.append("<input type=\"submit\" value=\""+ControlAction.MODIFY.getDescription()+"\" name=\""+ControlAction.MODIFY+"\" />");
			}
			buf.append("</form></body></html>");
			this.customResponse = new Response(Response.Status.OK, MIME_HTML, buf.toString());
			return true;
		}
		return false;
	}
	
	/**
	 * finds the corresponding XML task or returns null, if it was not found
	 * @param xid
	 * @return
	 */
	public XMLTask findXMLTask(int xid) {
		for(XMLTask x : this.XML_TASKS) {
			if(xid == x.getXMLID()) {
				return x;
			}
		}
		return null;
	}
	
	/**
	 * used to generate the list actions
	 * @param listAction
	 * @return
	 */
	private static String getListUrl(String listAction) {
		HashMap<String, String> map = new HashMap<>();
		map.put(LIST_ACTION, listAction);
		return Mailer.getLink(ControlAction.LIST, false, null, map, false); 
	}
	
	/**
	 * performs different kinds of list actions
	 * @param params
	 */
	public void performListAction(Map<String, String> params) {
		String type = params.get(LIST_ACTION);
		StringBuffer buf = new StringBuffer();
		
		// list the menu
		if(type == null) {
			buf.append("<a href=\"" + getListUrl(RUNNING_TASKS) + "\">List running tasks</a><br />");
			buf.append("<a href=\"" + getListUrl(COMPLETED_TASKS) + "\">List completed tasks</a><br />");
			buf.append("<a href=\"" + getListUrl(FAILED_TASKS) + "\">List failed tasks</a><br />");
			buf.append("<a href=\"" + getListUrl(WAITING_TASKS) + "\">List waiting tasks</a><br />");
			buf.append("<a href=\"" + getListUrl(IGNORED_TASKS) + "\">List ignored tasks</a><br />");
			buf.append("<a href=\"" + getListUrl(RESOLVED_TASKS) + "\">List manually resolved tasks</a><br />");
			buf.append("<a href=\"" + getListUrl(ALL_TASKS) + "\">List all tasks</a><br />");
		}
		else if(type.equals(ALL_TASKS) || type.equals(FAILED_TASKS) || type.equals(COMPLETED_TASKS) || type.equals(RUNNING_TASKS) || type.equals(WAITING_TASKS) || type.equals(IGNORED_TASKS) || type.equals(RESOLVED_TASKS)) {
			ArrayList<String> taskIDs = new ArrayList<>();
			if(type.equals(ALL_TASKS)) {
				taskIDs = this.filterTasks();
			}
			else if(type.equals(FAILED_TASKS)) {
				taskIDs = this.filterTasks(TaskStatus.KILLED, TaskStatus.FAILED, TaskStatus.FAILED_ERROR_CHECK, TaskStatus.FAILED_SUCCESS_CHECK, TaskStatus.FAILED_SYNTAX);
			}
			else if(type.equals(COMPLETED_TASKS)) {
				taskIDs = this.filterTasks(TaskStatus.FINISHED);
			}
			else if(type.equals(RUNNING_TASKS)) {
				taskIDs = this.filterTasks(TaskStatus.RUNNING);
			}
			else if(type.equals(WAITING_TASKS)) {
				taskIDs = this.filterTasks(TaskStatus.WAITING_DEPENDENCIES, TaskStatus.WAITING_QUEUE);
			}
			else if(type.equals(IGNORED_TASKS)) {
				taskIDs = this.filterTasks(TaskStatus.IGNORE);
			}
			else if(type.equals(RESOLVED_TASKS)) {
				taskIDs = this.filterTasks(TaskStatus.RESOLVED);
			}
			// get the table with that tasks
			buf = this.listTasks(taskIDs, type);
		}
		this.customResponse = new Response(Response.Status.OK, MIME_HTML, buf.toString());
	}
	
	/**
	 * get a HTML table for a list of task IDs
	 * @param taskIDs
	 * @return
	 */
	public StringBuffer listTasks(ArrayList<String> taskIDs, String listAction) {
		StringBuffer buf = new StringBuffer();
		
		if(taskIDs.size() > 0) {
			HTMLHelper.startHTML(buf, 15); 
			HTMLHelper.startTable(buf, "ID", "Name", "Block argument", "Executor", "Hostname", "Exec. counter", "Status", "Action");
			for(String id : taskIDs) {
				if(this.VALID_TASKS.containsKey(id)) { 
					Task t = this.VALID_TASKS.get(id);
					HTMLHelper.addRow(buf, listAction, getPossibleActions(t), t.getID(), t.getName(), t.getDisplayGroupFileName(), t.getExecutor().getName(), t.getHost(), Integer.toString(t.getExecutionCounter()), t.getStatus().toString() + (t.isBlocked() ? " (checkpoint)" : ""));
				}
			}
			HTMLHelper.endTable(buf);
			HTMLHelper.endHTML(buf);
		}
		else {
			buf.append("Currently no tasks with that status exist.");
		}
		return buf;
	}

	/**
	 * filters the tasks to display
	 * @param status
	 * @return
	 */
	public ArrayList<String> filterTasks(TaskStatus... status) {
		// init the valuid ids
		HashSet<Integer> valid = new HashSet<>();
		for(TaskStatus s : status) {
			valid.add(s.ordinal());
		}
		
		// check, which tasks match that condition
		ArrayList<String> ret = new ArrayList<>();
		synchronized(this.VALID_TASKS) { 
			for(Task t : this.VALID_TASKS.values()) {
				// ignore tasks with negative IDs because these are slave tasks!
				if(t.getID().startsWith(MINUS))
					continue;
				
				if(valid.size() == 0 || valid.contains(t.getStatus().ordinal())) 
					ret.add(t.getID());
			}
		}
		return ret;
	}
	
	/**
	 * returns the possible actions dependent on the state of the task
	 * @param t
	 * @return
	 */
	public static ArrayList<ControlAction> getPossibleActions(Task t) {
		TaskStatus s = t.getStatus();
		if(s == null)
			return null;
		
		ArrayList<ControlAction> l  = new ArrayList<>();
		if(TaskStatus.RUNNING.equals(s)) {
			l.add(ControlAction.TERMINATE_TASK);
			l.add(ControlAction.DISPLAY);
		}
		else if(TaskStatus.WAITING_RESTRICTIONS.equals(s)) {
			l.add(ControlAction.RELEASE_RESOURCE_RESTRICTIONS);
			l.add(ControlAction.DISPLAY);
		}
		else if(TaskStatus.WAITING_DEPENDENCIES.equals(s)) {
			l.add(ControlAction.DISPLAY);
		}
		else if(TaskStatus.TERMINATED.equals(s) || TaskStatus.FAILED.equals(s) || TaskStatus.FAILED_ERROR_CHECK.equals(s) || TaskStatus.FAILED_SUCCESS_CHECK.equals(s) || TaskStatus.FAILED_SYNTAX.equals(s)) {
			l.add(ControlAction.DISPLAY);
			l.add(ControlAction.MODIFY);
			l.add(ControlAction.RESTART);
			l.add(ControlAction.IGNORE);
			l.add(ControlAction.RESOLVE);
		}
		else {
			l.add(ControlAction.DISPLAY);
			if(t.isBlocked()) {
				l.add(ControlAction.RELEASE);
			}
		}
		return l;
	}
}
