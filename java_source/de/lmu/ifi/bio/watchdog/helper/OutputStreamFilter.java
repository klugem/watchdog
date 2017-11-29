package de.lmu.ifi.bio.watchdog.helper;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashSet;

/**
 * Allows to filter specific messages which should not be printed and can not be caught otherwise.
 * @author Michael Kluge
 *
 */
public class OutputStreamFilter extends PrintStream {
	private final HashSet<String> FILTER = new HashSet<>();
	
	/**
	 * 
	 * Constructor
	 * @param fd FileDescriptor to write to
	 */
	public OutputStreamFilter(FileDescriptor fd) {
			super(new FileOutputStream(fd));
	}
	
	/**
	 * Get filter for standard output stream
	 * @return
	 */
	public static OutputStreamFilter getStdOutStreamFilter() {
		return new OutputStreamFilter(FileDescriptor.out);
	}

	/**
	 * Get filter for standard error stream
	 * @return
	 */
	public static OutputStreamFilter getStdErrStreamFilter() {
		return new OutputStreamFilter(FileDescriptor.err);
	}
	
	/**
	 * adds a string which should not be printed
	 * @param notPrint
	 */
	public void addStringToFilter(String notPrint) {
		this.FILTER.add(notPrint);
	}

	@Override
	public void println(String s) {
		if(!this.FILTER.contains(s))
			super.println(s);
	}
}
