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
			logger.error("Slurm command failed: " + this.command);
			logger.error("arguments: " + StringUtils.join(this.args, " "));
			logger.error("exit code: " + this.exit);
			logger.error("stdout: " + this.out);
			logger.error("stderr: " + this.err);
		}
		else {
			logger.info("Slurm command info: " + this.command);
			logger.info("arguments: " + StringUtils.join(this.args, " "));
			logger.error("exit code: " + this.exit);
			logger.info("stdout: " + this.out);
			logger.info("stderr: " + this.err);	
		}
	}
}
