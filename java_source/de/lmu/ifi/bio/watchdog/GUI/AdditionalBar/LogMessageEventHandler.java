package de.lmu.ifi.bio.watchdog.GUI.AdditionalBar;

import de.lmu.ifi.bio.watchdog.helper.LogMessageEvent;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import javafx.event.EventHandler;

public class LogMessageEventHandler implements EventHandler<LogMessageEvent> {
	
	private final StatusConsole C;
	private final int ID;
	private static int counter = 0;
	
	public LogMessageEventHandler(StatusConsole c) {
		this.C = c;
		this.C.addMessageHandler(this);
		this.ID = counter++;
	}
	
	@Override
	public int hashCode() {
		return this.ID;
	}

	@Override
	public void handle(LogMessageEvent event) {
		LogLevel l = event.getLogLevel();
		String m = event.getMessage();
		switch(l) {
		case ERROR:
			this.C.addErrorMessage(m);
			break;
		case WARNING:
			this.C.addWarningMessage(m);
			break;
		case INFO:
			this.C.addInfoMessage(m);
			break;
		case DEBUG:
			this.C.addDebugMessage(m);
			break;
			
		default:
			break;
		}
	}
}
