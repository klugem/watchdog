package de.lmu.ifi.bio.watchdog.runner;

import com.beust.jcommander.Parameter;

public abstract class DescriptionParameters {

	@Parameter(names={"-info", "-desc"}, description="prints a short description of the tool and exit", help=true)
	public boolean desc = false;
	
	
	/**
	 * short description of the function of the tool
	 * @return
	 */
	public abstract String getDescription();
}
