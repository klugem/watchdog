package de.lmu.ifi.bio.watchdog.GUI.AdditionalBar;


import java.util.ArrayList;

import de.lmu.ifi.bio.watchdog.GUI.WorkflowDesignController;
import de.lmu.ifi.bio.watchdog.GUI.event.StatusConsoleMessageEvent;
import de.lmu.ifi.bio.watchdog.GUI.fxml.FXMLRessourceLoader;
import de.lmu.ifi.bio.watchdog.GUI.interfaces.TabableNode;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.util.Pair;

public class StatusConsole extends TabableNode {
		
	public static final String NAME = "Status Messages";
	public static final String IMAGE = null;
	private final ArrayList<LogMessageEventHandler> HANDLERS = new ArrayList<>();
	private StatusConsoleController controller;
	private static boolean disableAutoScroll = false;
	private boolean lastAutoScrollValue = false;
	
	/** hide constructor */
	private StatusConsole() {}

	public static StatusConsole getStatusConsole(String name) {
		try {
			FXMLRessourceLoader<StatusConsole, StatusConsoleController> l = new FXMLRessourceLoader<>("StatusConsole.fxml", new StatusConsole());
			Pair<StatusConsole, StatusConsoleController> pair = l.getNodeAndController();
			StatusConsole c = pair.getKey();
			c.controller = pair.getValue();
				
			// set properties on GUI
			c.controller.setName(name);
			return c;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void addInfoMessage(String text) {
		this.controller.addMessage(MessageType.INFO, text, disableAutoScroll);
	}
	
	public void addWarningMessage(String text) {
		this.controller.addMessage(MessageType.WARNING, text, disableAutoScroll);
	}
	
	public void addErrorMessage(String text) {
		this.controller.addMessage(MessageType.ERROR, text, disableAutoScroll);
	}

	public void addDebugMessage(String text) {
		this.controller.addMessage(MessageType.DEBUG, text, disableAutoScroll);
	}
	
	public void addMessage(MessageType type, String text, boolean disableAutoScroll) {
		this.lastAutoScrollValue = disableAutoScroll;
		this.controller.addMessage(type, text, disableAutoScroll);
	}
	
	public void addMessage(MessageType type, String text) {
		this.controller.addMessage(type, text, this.lastAutoScrollValue);
	}
	
	public void clear() {
		this.controller.clear();
	}
	
	public ArrayList<String> getMessages() {
		return this.controller.getMessages();
	}

	@Override
	public String getName() {
		return this.controller.getName();
	}

	@Override
	public ImageView getImage() {
		return this.controller.getImage();
	}

	@Override
	protected Node getRoot() {
		return this.controller.getRoot();
	}
	
	public static boolean addGlobalMessage(MessageType type, String text) {
		return WorkflowDesignController.sendEventToAdditionalBarController(new StatusConsoleMessageEvent(type, text, disableAutoScroll));
	}

	@Override
	public void setWidth(double w) {
		this.controller.setWidth(w);
	}

	@Override
	public void setHeight(double h) {
		this.controller.setHeight(h);
	}

	public void setAutoScrollLock(boolean disableScroll) {
		disableAutoScroll = disableScroll;
	}

	public void addMessageHandler(LogMessageEventHandler logMessageEventHandler) {
		this.HANDLERS.add(logMessageEventHandler);
	}
	
	public ArrayList<LogMessageEventHandler> getMessageHandlers() {
		return this.HANDLERS;
	}

	public int size() {
		return this.controller.getMessages().size();
	}
}
