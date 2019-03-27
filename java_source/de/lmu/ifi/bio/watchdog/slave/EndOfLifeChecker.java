package de.lmu.ifi.bio.watchdog.slave;

import java.util.HashMap;
import java.util.HashSet;

import de.lmu.ifi.bio.multithreading.StopableLoopRunnable;

/**
 * Checks, if any slave can be killed
 * @author kluge
 *
 */
public class EndOfLifeChecker extends StopableLoopRunnable {

	private static int WAIT_SECONDS = 5;
	public HashMap<String, Integer> CHECK_IDS = new HashMap<>();
	public HashMap<String, Boolean> FORCED_SLAVE_IDS = new HashMap<>();
	
	public EndOfLifeChecker() {
		super("Server_EndOfLifeChecker");
	}
	
	/**
	 * adds the ID of a slave that should be checked
	 * @param sID
	 */
	public synchronized void addSlaveID2Check(String sID, boolean forced) {
		if(sID != null) {
			this.CHECK_IDS.put(sID, -2); // give it some time before actual checks start
			this.FORCED_SLAVE_IDS.put(sID, forced);
		}
	}
	
	/**
	 * test, if any of the registered slaves should be killed
	 */
	public synchronized void checkKill() {
		HashSet<String> ids = new HashSet<>(this.CHECK_IDS.keySet());
		for(String sID : ids) {
			// kill it instantly
			if(this.FORCED_SLAVE_IDS.containsKey(sID) && this.FORCED_SLAVE_IDS.get(sID)) {
				this.killSlave(sID);
			}
			// normal slave --> counter
			else {
				int count = this.CHECK_IDS.get(sID);
				if(count < 0) { 
					this.CHECK_IDS.put(sID, count+1);
				} 
				else if(!Master.isSlaveNeededAnyMore(sID)) {
					// check, if check was x times negative
					if(count > WAIT_SECONDS)
						this.killSlave(sID);
					else
						this.CHECK_IDS.put(sID, count+1);
				}
				// slave is needed --> do not check again until a new request is spawned.
				else {
					this.CHECK_IDS.remove(sID);
				}
			}
		}
	}
	
	/**
	 * kills a slave
	 * @param sID
	 */
	private synchronized void killSlave(String sID) {
		Master.killSlave(sID);
		this.CHECK_IDS.remove(sID);
		this.FORCED_SLAVE_IDS.remove(sID);
	}

	@Override
	public int executeLoop() throws InterruptedException {
		this.checkKill();
		return 1;
	}

	@Override
	public void afterLoop() {
		this.CHECK_IDS.clear();
		this.FORCED_SLAVE_IDS.clear();
	}

	@Override
	public long getDefaultWaitTime() {
		return 200;
	}

	@Override
	public void beforeLoop() {
	
	}

	@Override
	public boolean canBeStoppedForDetach() {
		return this.CHECK_IDS.size() == 0;
	}
}
