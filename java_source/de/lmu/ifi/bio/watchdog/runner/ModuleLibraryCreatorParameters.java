package de.lmu.ifi.bio.watchdog.runner;

import java.util.List;

import com.beust.jcommander.Parameter;

public class ModuleLibraryCreatorParameters extends DescriptionParameters {

	@Parameter(names={"-moduleFolder", "-m"}, description="path to the parent folder(s) of modules that should be included in the module reference book", required=true)
	protected List<String> module;
	
	@Parameter(names={"-outputFolder", "-o"}, description="path to a folder in which the module reference book should be stored", required=true)
	protected String outputDir;
	
	@Parameter(names={"-watchdogBase", "-w"}, description="uses a different watchdog installation directory; default: ${jarLocation}/..", required=false)
	protected String watchdogBase;
	
	@Parameter(names={"-version"}, description="prints the version number of Watchdog", required=false, help=true)
	protected boolean version = false;
	
	@Parameter(names={"-help", "-h", "--help", "--man", "-man"}, description="print usage message and exit", help=true)
	protected boolean help = false;

	@Override
	public String getDescription() {
		return "Generates a module reference book based on XML documentation files of modules.";
	}
}