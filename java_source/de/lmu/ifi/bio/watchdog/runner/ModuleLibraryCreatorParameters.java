package de.lmu.ifi.bio.watchdog.runner;

import java.util.ArrayList;

import com.beust.jcommander.Parameter;

public class ModuleLibraryCreatorParameters {

	@Parameter(names={"-moduleFolder", "-m"}, description="path to the module folder(s) that should be validated", required=true)
	protected ArrayList<String> module;
	
	@Parameter(names={"-watchdogBase", "-w"}, description="uses a different watchdog installation directory; default: ${jarLocation}/..", required=false)
	protected String watchdogBase;
	
	@Parameter(names={"-version"}, description="prints the version number of Watchdog", required=false, help=true)
	protected boolean version = false;
	
	@Parameter(names={"-help", "-h", "--help", "--man", "-man"}, description="print usage message and exit", help=true)
	protected boolean help = false;
}