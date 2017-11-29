package de.lmu.ifi.bio.watchdog.helper;

import java.io.BufferedOutputStream;

/**
 * Writer, which will not save anything.
 * @author Michael Kluge
 *
 */
public class DevNullWriter extends BufferedOutputStream {

	public DevNullWriter() {
		super(null, 1);
	}
	
	@Override
	public void write(byte[] bbuf, int off, int len) { }
	
	@Override
	public void write(byte[] bbuf) { }
	
	@Override
	public void write(int c) { }

	@Override
	public void flush() { }
	
	@Override
	public void close() { }
}
