package de.lmu.ifi.bio.watchdog.runner;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

import de.lmu.ifi.bio.watchdog.executor.WatchdogThread;

public class XMLBasedWatchdogParameters {

	@Parameter(names={"-xml", "-x"}, description="path to the XML workflow file", required=true)
	protected String xml;
	
	@Parameter(names={"-port", "-p"}, description="port for the HTTP server", required=false)
	protected int port = WatchdogThread.DEFAULT_HTTP_PORT;
	
	@Parameter(names={"-log", "-l"}, description="path to the log file", required=false)
	protected String log;
	
	@Parameter(names={"-start"}, description="start with that ID (included)", required=false)
	protected int start = Integer.MIN_VALUE;
	
	@Parameter(names={"-stop"}, description="stop with that ID (included)", required=false)
	protected int stop = Integer.MAX_VALUE;	
	
	@Parameter(names={"-simulate"}, description="simulate the jobs as far as possible", required=false)
	protected boolean simulate = false;
	
	@Parameter(names={"-disableCheckpoint"}, description="checkpoints are ignored during execution", required=false)
	protected boolean disableCheckpoint = false;
	
	@Parameter(names={"-disableMails"}, description="no mails are sent even if a mail adress is given in the workflow", required=false)
	protected boolean disableMails = false;
	
	@Parameter(names={"-ignoreExecutor"}, description="ignores the executor info and executes all tasks on the local host running n tasks at once", required=false, help=false)
	protected int ignoreExecutor = 0;
	
	@Parameter(names={"-validate"}, description="validate a XML file or a folder containing *.xml files", required=false)
	protected boolean validate = false;
	
	@Parameter(names={"-forceLoading"}, description="ignores the XSD schema definition file of watchdog and all modules during parsing (might crash)", required=false)
	protected boolean forceLoading = false;
	
	@Parameter(names={"-mailConfig"}, description="config file for the mail server; if none is given SMTP on port 25 on localhost without authentification is used", required=false)
	protected String mailConfig = null;
	
	@Parameter(names={"-mailWaitTime"}, description="wait time in seconds before task processing is started if no mail adress is given", required=false)
	protected int mailWaitTime = 5;
	
	@Parameter(names={"-include", "-i"}, description="xml task id that should be executed; can be used several times; can be used in combination with -start and -stop", required=false)
	protected List<String> include = new ArrayList<>();
	
	@Parameter(names={"-exclude", "-e"}, description="xml task id that should be ignored during execution; can be used several times; can be used in combination with -start and -stop", required=false)
	protected List<String> exclude = new ArrayList<>();
	
	@Parameter(names={"-resume"}, description="resumes workflow execution whereby tasks that were executed successfully (and parameters are unchanged) are ignored; expects a watchdog status log file from a previous watchdog run; can be used in combination with -start, -stop, -include and -exclude", required=false)
	protected String resume;
	
	@Parameter(names={"-ignoreParamHashInResume"}, description="do not validate parameter hash in resume mode", required=false)
	protected boolean ignoreParamHashInResume;
	
	@Parameter(names={"-autoDetach"}, description="stops the execution of Watchdog whenever possible (running tasks on an external executor will not be terminated on detach); status of previously running tasks is checked when Watchdog is started with the -restart and -attachInfo option;", required=false)
	protected boolean autoDetach = false;
	
	@Parameter(names={"-attachInfo"}, description="path to a file that is used to restore the info on previously running tasks when Wathdog should be re-attached to running tasks; (can not be used in combination with -resume as resume file is automatically loaded)", required=false)
	protected String attachInfo = null;
	
	@Parameter(names={"-useEnvBase"}, description="ignores the watchdogBase attribute of the XML workflow and overrides it with the content of the "+ XMLBasedWatchdogRunner.ENV_WATCHDOG_HOME_NAME +" environment variable", required=false)
	protected boolean useEnvBase = false;
	
	@Parameter(names={"-tmpFolder", "-t"}, description="uses a different tmp folder; should be accessible by external executors; default: ${watchdogBase}/tmp", required=false)
	protected String tmpFolder;
	
	@Parameter(names={"-version"}, description="prints the version number of Watchdog", required=false, help=true)
	protected boolean version = false;
	
	@Parameter(names={"-help", "-h", "--help", "--man", "-man"}, description="print usage message and exit", help=true)
	protected boolean help = false;
}