package de.lmu.ifi.bio.watchdog.slave;

import java.io.File;

import com.beust.jcommander.Parameter;

public class SlaveParameters {

	@Parameter(names={"-host", "-h"}, description="hostname or IP of the master", required=true)
	protected String host;
	
	@Parameter(names={"-port", "-p"}, description="port used for the connection to the master", required=true)
	protected int port = 7115;
	
	@Parameter(names={"-base", "-b"}, description="base folder of watchdog", required=true)
	protected File base;
	
	@Parameter(names={"-id", "-i"}, description="id that can be used to identify the slave", required=true)
	protected String id;
	
	@Parameter(names={"-maxRunning", "-m"}, description="number of tasks that are allowed to run at the same time; default: 1", required=false)
	protected Integer maxRunning = 1;
}