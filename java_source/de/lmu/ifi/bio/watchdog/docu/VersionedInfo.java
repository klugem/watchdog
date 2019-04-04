package de.lmu.ifi.bio.watchdog.docu;

public class VersionedInfo<T extends Object> {
	public int MAX_VER = 1;
	public int MIN_VER = 1;
	public final T VALUE;
	
	public VersionedInfo(T value, int min, int max) {
		this.VALUE = value;
		this.MIN_VER = min;
		this.MAX_VER = max;
	}
}
