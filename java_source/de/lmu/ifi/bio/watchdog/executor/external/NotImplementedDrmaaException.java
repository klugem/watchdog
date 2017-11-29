package de.lmu.ifi.bio.watchdog.executor.external;

import org.ggf.drmaa.DrmaaException;

public class NotImplementedDrmaaException extends DrmaaException {
	private static final long serialVersionUID = -6121763151867932836L;
	
	public NotImplementedDrmaaException() {
		super("not implemented feature was called!");
	}
}
