package de.lmu.ifi.bio.watchdog.validator.github.checker;

import java.io.File;

/**
 * Tests if watchdog base and git clone directory are set (by travis) and exist 
 * @author kluge
 *
 */
public abstract class GithubCheckerIO extends GithubCheckerBase  {

	public GithubCheckerIO(String name) {
		super(name);
	}

	@Override
	public boolean test() {
		super.test();
		boolean ret = true;
		if(this.watchdogBase == null) {
			this.error("Watchdog's base is not set correctly via an environment variable.");
			ret = false;
		}
		boolean checkCloneDir = !this.isLocalTestMode();
		if(checkCloneDir) {
			if(this.GIT_CLONE_DIR == null) {
				this.error("Git clone directory is not set correctly via an environment variable.");
				ret = false;
			}
		}
		
		// test if folders really exists and are readable
		if(ret) {
			File watchdogBase = new File(this.watchdogBase);
			if(!watchdogBase.exists() || !watchdogBase.canRead()) {
				this.error("Watchdog's base does not exist or is not readable: '"+watchdogBase.getAbsolutePath()+"'");
				ret=false; 
			}
			if(checkCloneDir) {
				File cloneDir = new File(this.GIT_CLONE_DIR);
				if(!cloneDir.exists() || !cloneDir.canRead()) {
					this.error("Git clone directory does not exist or is not readable: '"+cloneDir.getAbsolutePath()+"'");
					ret=false;
				}
			}
		}
		return ret;
	}	
}
