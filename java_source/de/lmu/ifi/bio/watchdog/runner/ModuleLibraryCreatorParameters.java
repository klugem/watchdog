package de.lmu.ifi.bio.watchdog.runner;

import java.util.List;

import com.beust.jcommander.Parameter;

public class ModuleLibraryCreatorParameters {

	@Parameter(names={"-moduleFolder", "-m"}, description="path to the module folder(s) for which documentation templates should be created", required=true)
	protected List<String> module;
	
	@Parameter(names={"-outputFolder", "-o"}, description="output folder for the generated html page", required=true)
	protected String outputFolder;
	
	@Parameter(names={"-watchdogBase", "-w"}, description="uses a different watchdog installation directory; default: ${jarLocation}/..", required=false)
	protected String watchdogBase;
	
	@Parameter(names={"-version"}, description="prints the version number of Watchdog", required=false, help=true)
	protected boolean version = false;
	
	@Parameter(names={"-help", "-h", "--help", "--man", "-man"}, description="print usage message and exit", help=true)
	protected boolean help = false;
}