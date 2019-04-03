package de.lmu.ifi.bio.multithreading;

public class ConvertedMonitorRunnable extends MonitorRunnable {
	private final Runnable RUNNABLE;
	private static int counter = 0;
	private boolean canBeStoppedForDetach = false;

	public ConvertedMonitorRunnable(Runnable r, boolean canBeStoppedForDetach) {
		super(r.getClass().getSimpleName() + "_" + counter);
		this.RUNNABLE = r;
		this.canBeStoppedForDetach = canBeStoppedForDetach;
		counter++;
	}
	
	@Override
	public void run() {
		super.run();
		this.RUNNABLE.run();
	}

	@Override
	public boolean canBeStoppedForDetach() {
		return this.canBeStoppedForDetach;
	}
}
