package de.lmu.ifi.bio.watchdog.helper;

import java.util.ArrayList;
import java.util.HashMap;

import de.lmu.ifi.bio.watchdog.executor.HTTPListenerThread;

public class HTMLHelper {
	
	private static final String NEW = System.lineSeparator();
	
	public static void startHTML(StringBuffer buf, int refresh) {
		buf.append("<html><head>");
		buf.append(NEW);
		buf.append("<link rel=\"stylesheet\" href=\"/css/style.css\">");
		buf.append(NEW);
		buf.append("<script src=\"/js/functions.js\"></script>");
		buf.append(NEW);
		if(refresh > 0) {
			buf.append("<meta http-equiv=\"refresh\" content=\"10\"/>");
			buf.append(NEW);
		}
		buf.append("</head><body>");
		buf.append(NEW);
	}
	
	public static void endHTML(StringBuffer buf) {
		buf.append("</body></html>");
		buf.append(NEW);
	}
	
	public static void startTable(StringBuffer buf, String... header) {
		buf.append(NEW);
		buf.append("<table cellpadding=\"0\" cellspacing=\"0\">");
		buf.append(NEW);	
		buf.append("<thead><tr>");
		buf.append(NEW);	
		for(String h : header) {
			buf.append("<th>");
			buf.append(h);
			buf.append("</th>");
		}
		buf.append(NEW);
		buf.append("</tr></thead><tbody>");
		buf.append(NEW); 
	}
	
	public static void addRow(StringBuffer buf, String listAction, ArrayList<ControlAction> actions, String... rows) {
		buf.append("<tr>");
		for(String h : rows) {
			buf.append("<td>");
			if(h != null)
				buf.append(h);
			buf.append("&nbsp;");
			buf.append("</td>");
		}
		buf.append("<td>");
		// add the control actions if some are set
		if(actions.size() > 0) {
			HashMap<String, String> addParams = new HashMap<>();
			addParams.put(HTTPListenerThread.LIST_ACTION, listAction);
			String link = Mailer.getLink(ControlAction.USERINTERFACE_ACTION, false, rows[0], addParams, true);
			
			buf.append("<form action=\""+link+"\" method=\"POST\">");
			buf.append("<input type=\"hidden\" name=\""+ HTTPListenerThread.TASK_ID+"\" value=\""+ rows[0] +"\">");
			buf.append("<input type=\"hidden\" name=\""+ HTTPListenerThread.LIST_ACTION+"\" value=\""+ listAction +"\">");
			
			buf.append("<select name=\""+ HTTPListenerThread.ACTION_NAME +"\">");
			for(ControlAction a : actions) {
				buf.append("<option value=\""+a.name()+"\">");
				buf.append(a.getActionName());
				buf.append("</option>");
			}
			buf.append("</select>");
			buf.append("<input type=\"submit\" value=\""+ControlAction.USERINTERFACE_ACTION.getActionName()+"\"/>");
			buf.append("</form>");
		}
		else
			buf.append("&nbsp;");
		buf.append("</td>");
		buf.append("</tr>");
		buf.append(NEW);
	}
	
	public static void endTable(StringBuffer buf) {
		buf.append("</tbody></table>");
		buf.append(NEW);
	}

}
