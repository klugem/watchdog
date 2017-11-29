package de.lmu.ifi.bio.multithreading;

public class ConvertedMonitorRunnable extends MonitorRunnable {
	private final Runnable RUNNABLE;
	private static int counter = 0;

	public ConvertedMonitorRunnable(Runnable r) {
		super(ConvertedMonitorRunnable.class.getSimpleName() + "_" + counter);
		this.RUNNABLE = r;
		counter++;
	}
	
	@Override
	public void run() {
		super.run();
		this.RUNNABLE.run();
	}
}
