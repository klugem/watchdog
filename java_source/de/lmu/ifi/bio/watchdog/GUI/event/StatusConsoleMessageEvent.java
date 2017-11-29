package de.lmu.ifi.bio.watchdog.GUI.event;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.MessageType;
import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.StatusConsole;
import javafx.event.EventType;

/** Event which is sent when a new message should be added to the console */
public class StatusConsoleMessageEvent extends TabEvent {

	private static final long serialVersionUID = 4042214691440779871L;
	public static final EventType<StatusConsoleMessageEvent> NEW_MESSAGE_EVENT_TYPE = new EventType<>("NEW_MESSAGE_EVENT_TYPE");
	
	private final String MESSAGE;
	private final MessageType TYPE;
	private final boolean DISABLE_AUTO_SCROLL;
	
	public StatusConsoleMessageEvent(MessageType type, String message, boolean disableAutoScroll) {
		super(StatusConsole.NAME, StatusConsoleMessageEvent.NEW_MESSAGE_EVENT_TYPE);
		this.MESSAGE = message;
		this.TYPE = type;
		this.DISABLE_AUTO_SCROLL = disableAutoScroll;
	}
	
	public String getMessage() {
		return this.MESSAGE;
	}
	
	public MessageType getMessageType() {
		return this.TYPE;
	}
	
	public boolean isAutoScrollDisabled() {
		return this.DISABLE_AUTO_SCROLL;
	}
}
