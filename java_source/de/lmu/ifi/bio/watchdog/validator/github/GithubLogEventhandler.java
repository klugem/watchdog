package de.lmu.ifi.bio.watchdog.validator.github;

import de.lmu.ifi.bio.watchdog.helper.LogMessageEvent;
import de.lmu.ifi.bio.watchdog.interfaces.BasicEventHandler;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.validator.github.checker.GithubCheckerBase;

public class GithubLogEventhandler implements BasicEventHandler<LogMessageEvent> {
	
	private final GithubCheckerBase C;
	
	public GithubLogEventhandler(GithubCheckerBase checker) {
		this.C = checker;
	}

	@Override
	public void handle(LogMessageEvent event) {
		LogLevel l = event.getLogLevel();
		String m = event.getMessage();
		switch(l) {
		case ERROR:
			this.C.error(m);
			break;
		case WARNING:
		case INFO:
			this.C.info(m);
			break;
			
		default:
			break;
		}
	}
}