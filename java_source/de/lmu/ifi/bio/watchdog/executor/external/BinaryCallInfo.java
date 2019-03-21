package de.lmu.ifi.bio.watchdog.executor.external;

import org.apache.commons.lang3.StringUtils;

import de.lmu.ifi.bio.watchdog.logger.Logger;

public class BinaryCallInfo {

	public String command;
	public String[] args;
	public String out;
	public String err;
	public int exit;
	
	
	public void printInfo(Logger logger, boolean error) {
		if(error) {
			logger.error("Command failed: " + this.command);
			logger.error("arguments: " + StringUtils.join(this.args, " "));
			logger.error("exit code: " + this.exit);
			logger.error("stdout: " + this.out);
			logger.error("stderr: " + this.err);
		}
		else {
			logger.debug("Command info: " + this.command);
			logger.debug("arguments: " + StringUtils.join(this.args, " "));
			logger.debug("exit code: " + this.exit);
			logger.debug("stdout: " + this.out);
			logger.debug("stderr: " + this.err);	
		}
	}
}
