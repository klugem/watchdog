package de.lmu.ifi.bio.multithreading;

/**
 * Runnable that has a name and can measure the real time that execution takes
 * overwriting method must call super.run() for time measurement
 * @author kluge
 *
 */
public abstract class MonitorRunnable implements Runnable {

	private final String NAME;
	private final long CREATION_TIME = System.currentTimeMillis();
	private long execution_start_time = - 1;
	
	public MonitorRunnable(String name) {
		this.NAME = name;
	}
	
	public String getName() {
		return this.NAME;
	}
	
	@Override
	public void run() {
		this.execution_start_time = System.currentTimeMillis();
	}
	
	public long getExecutionStart() {
		return this.execution_start_time;
	}
	
	public long getCreationTime() {
		return this.CREATION_TIME;
	}
	
	@Override
	public int hashCode() {
		return this.getName().hashCode();
	}
	
	/**
	 * should return true, if it can be stopped for restart / detach
	 * @return
	 */
	public abstract boolean canBeStoppedForDetach();
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof MonitorRunnable) {
			return this.NAME.equals(((MonitorRunnable) o).getName());
		}
		return false;
	}
}
