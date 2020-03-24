package de.lmu.ifi.bio.watchdog.executionWrapper;

import org.apache.commons.lang3.SystemUtils;

public enum OS {
	WIN, MAC, UNIX, OTHER;
	
	public boolean isWin() {
		return WIN.equals(this);
	}
	
	public boolean isMac() {
		return MAC.equals(this);
	}
	
	public boolean isUnix() {
		return UNIX.equals(this);
	}
	
	public boolean isOther() {
		return OTHER.equals(this);
	}
	
	/**
	 * OS the program is executed on
	 * @return
	 */
	public static OS getOS() {
		if(SystemUtils.IS_OS_UNIX) return OS.UNIX;
		else if(SystemUtils.IS_OS_MAC) return OS.MAC;
		else if(SystemUtils.IS_OS_WINDOWS) return OS.WIN;
		return OS.OTHER;
	}
}