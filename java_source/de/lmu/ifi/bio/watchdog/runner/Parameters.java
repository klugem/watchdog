package de.lmu.ifi.bio.watchdog.runner;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

import de.lmu.ifi.bio.watchdog.GUI.helper.PreferencesStore;

public class Parameters {

	@Parameter(names={"-xml", "-x"}, description="path to the XML workflow file", required=true)
	protected String xml;
	
	@Parameter(names={"-port", "-p"}, description="port for the HTTP server", required=false)
	protected int port = PreferencesStore.DEFAULT_PORT;
	
	@Parameter(names={"-log", "-l"}, description="path to the log file", required=false)
	protected String log;
	
	@Parameter(names={"-start"}, description="start with that ID (included)", required=false)
	protected int start = Integer.MIN_VALUE;
	
	@Parameter(names={"-stop"}, description="stop with that ID (included)", required=false)
	protected int stop = Integer.MAX_VALUE;	
	
	@Parameter(names={"-simulate"}, description="only simulate the jobs as far as possible", required=false)
	protected boolean simulate = false;
	
	@Parameter(names={"-version"}, description="prints the version number of Watchdog", required=false, help=true)
	protected boolean version = false;
	
	@Parameter(names={"-ignoreExecutor"}, description="ignores the executor info and executes all tasks on the local host running n tasks at once", required=false, help=false)
	protected int ignoreExecutor = 0;
	
	@Parameter(names={"-validate"}, description="validate a XML file or a folder containing *.xml files", required=false)
	protected boolean validate = false;
	
	@Parameter(names={"-mailConfig"}, description="config file for the mail server; if none is given SMTP on port 25 on localhost without authentification is used", required=false)
	protected String mailConfig = null;
	
	@Parameter(names={"-mailWaitTime"}, description="wait time in seconds before task processing is started if no mail adress is given", required=false)
	protected int mailWaitTime = 15;
	
	@Parameter(names={"-include", "-i"}, description="xml task id that should be executed; can be used several times; can be used in combination with -start and -stop", required=false)
	protected List<String> include = new ArrayList<>();
	
	@Parameter(names={"-exclude", "-e"}, description="xml task id that should be ignored during execution; can be used several times; can be used in combination with -start and -stop", required=false)
	protected List<String> exclude = new ArrayList<>();
	
	@Parameter(names={"--help"}, description="print usage message and exit", help=true)
	protected boolean help = false;
}