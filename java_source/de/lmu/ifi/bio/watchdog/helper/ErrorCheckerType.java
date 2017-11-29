package de.lmu.ifi.bio.watchdog.helper;

public enum ErrorCheckerType {
	ERROR("error"), SUCCESS("success");
	
	private final String TYPE;

	private ErrorCheckerType(String type) {
		this.TYPE = type;
	}
	
	public String toString() {
		return this.TYPE;
	}
}
