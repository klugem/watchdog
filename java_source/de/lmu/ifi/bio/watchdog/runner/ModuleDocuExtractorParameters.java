package de.lmu.ifi.bio.watchdog.runner;

import java.util.List;

import com.beust.jcommander.Parameter;

public class ModuleDocuExtractorParameters {

	@Parameter(names={"-moduleFolder", "-m"}, description="path to parent folder(s) of modules for which documentation templates should be created", required=true)
	protected List<String> module;
	
	@Parameter(names={"-authors", "-a"}, description="name of authors can be added to the template if the same for all modules", required=false)
	protected List<String> authors;
	
	@Parameter(names={"-categories", "-c"}, description="name of categories can be added to the template if the same for all modules", required=false)
	protected List<String> categories;
	
	@Parameter(names={"-maintainer", "-g"}, description="name of github users that maintain the module", required=false)
	protected List<String> maintainer;
	
	@Parameter(names={"-overwrite"}, description="overwrites existing documentation files without further confirmation", required=false)
	protected boolean overwrite = false;
	
	@Parameter(names={"-watchdogBase", "-w"}, description="uses a different watchdog installation directory; default: ${jarLocation}/..", required=false)
	protected String watchdogBase;
		
	@Parameter(names={"-tmpFolder", "-t"}, description="uses a different tmp folder; default: ${watchdogBase}/tmp", required=false)
	protected String tmpFolder;
	
	@Parameter(names={"-version"}, description="prints the version number of Watchdog", required=false, help=true)
	protected boolean version = false;
	
	@Parameter(names={"-help", "-h", "--help", "--man", "-man"}, description="print usage message and exit", help=true)
	protected boolean help = false;
}