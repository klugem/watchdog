package de.lmu.ifi.bio.watchdog.GUI;

import com.beust.jcommander.Parameter;

public class WorkflowDesignParameters {

	@Parameter(names={"-tmpFolder", "-t"}, description="uses a different tmp folder; default: ${watchdogBase}/tmp", required=false)
	protected String tmpFolder = null;
	
	@Parameter(names={"-disableLoadScreen"}, description="disables the load screen of Wachdog", required=false)
	protected boolean disableLoadScreen = false;
	
	@Parameter(names={"-help", "-h", "--help", "--man", "-man"}, description="print usage message and exit", help=true)
	protected boolean help = false;
}