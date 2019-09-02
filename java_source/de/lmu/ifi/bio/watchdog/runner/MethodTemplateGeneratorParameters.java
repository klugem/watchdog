package de.lmu.ifi.bio.watchdog.runner;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

public class MethodTemplateGeneratorParameters extends DescriptionParameters {

	@Parameter(names={"-xml", "-x"}, description="path to the XML workflow file; required for loading of correct module foders", required=true)
	protected String xml;
		
	@Parameter(names={"-start"}, description="start with that ID (included)", required=false)
	protected int start = Integer.MIN_VALUE;
	
	@Parameter(names={"-stop"}, description="stop with that ID (included)", required=false)
	protected int stop = Integer.MAX_VALUE;	
	
	@Parameter(names={"-outputFile"}, description="writes citation information of used modules into that file; must be used in combination with the -xml and -resume option", required=true)
	protected String outputFile = null;
		
	@Parameter(names={"-include", "-i"}, description="xml task id that should be executed; can be used several times; can be used in combination with -start and -stop", required=false)
	protected List<Integer> include = new ArrayList<>();
	
	@Parameter(names={"-exclude", "-e"}, description="xml task id that should be ignored during execution; can be used several times; can be used in combination with -start and -stop", required=false)
	protected List<Integer> exclude = new ArrayList<>();
	
	@Parameter(names={"-resume", "-logFile", "-l", "-r"}, description="watchdog status log file from a previous watchdog run", required=true)
	protected String resume;
	
	@Parameter(names={"-enumerate"}, description="enumerate the different citation information; format: N)", required=false)
	protected boolean enumerate = false;
	
	@Parameter(names={"-newline"}, description="separate citation information with newlines", required=false)
	protected boolean newline = false;

	@Parameter(names={"-name"}, description="add the name of the module before its citation information", required=false)
	protected boolean name = false;
	
	@Parameter(names={"-ignore"}, description="ignore modules wihout citation information", required=false)
	protected boolean ignore = false;
	
	@Parameter(names={"-pmid"}, description="add PMIDs if documented in module as meta-info; format: [PMIDS: x, y, z]", required=false)
	protected boolean pmid = false;
	
	@Parameter(names={"-used"}, description="add information on how many tasks and subtasks used that module as meta-info; format: [USED: tasks, subtasks]", required=false)
	protected boolean used = false;
	
	@Parameter(names={"-version"}, description="prints the version number of Watchdog", required=false)
	protected boolean version = false;
	
	@Parameter(names={"-help", "-h", "--help", "--man", "-man"}, description="print usage message and exit", help=true)
	protected boolean help = false;

	@Override
	public String getDescription() {
		return "Generates a short description for a workflow based on the status log file of a Watchdog run using "
				+ " the XML documentation files of modules.";
	}
}