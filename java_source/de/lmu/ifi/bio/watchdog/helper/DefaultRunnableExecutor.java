package de.lmu.ifi.bio.watchdog.helper;

import de.lmu.ifi.bio.watchdog.interfaces.RunnableExecutor;

/**
 * Simply executes the Runnable in the thread that is calling
 * @author kluge
 *
 */
public class DefaultRunnableExecutor implements RunnableExecutor {

	@Override
	public void run(Runnable runnable) {
		runnable.run();
	}
}
