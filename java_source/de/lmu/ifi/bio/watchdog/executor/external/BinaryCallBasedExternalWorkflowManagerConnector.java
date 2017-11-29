package de.lmu.ifi.bio.watchdog.executor.external;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import de.lmu.ifi.bio.watchdog.logger.Logger;

public abstract class BinaryCallBasedExternalWorkflowManagerConnector<A extends ExternalScheduledExecutor<?>> extends ExternalWorkloadManagerConnector<A> {

	private static final int BUFFER_SIZE = 1024;
	private final byte[] BUFFER = new byte[BUFFER_SIZE];
	
	public BinaryCallBasedExternalWorkflowManagerConnector(Logger l) {
		super(l);
	}

	private synchronized void readStream(InputStream stream, OutputStream out, boolean flush) throws IOException {
		int read = 0;
		while(stream.available() > 0) {
			 if((read = stream.read(BUFFER)) != -1) {
				out.write(BUFFER, 0, read);
				if(flush) out.flush();
			 }
		}
	}
		
	protected BinaryCallInfo executeCommand(String command, HashMap<String, String> env, String workingdir) {
		return this.executeCommand(command, new ArrayList<String>(), env, workingdir);
	}
	
	protected synchronized BinaryCallInfo executeCommand(String command, ArrayList<String> arguments, HashMap<String, String> env, String workingdir) {
		BinaryCallInfo info = new BinaryCallInfo();
		info.command = command;
		info.args = arguments.toArray(new String[0]);
		
		arguments.add(0, command);
		// create buffer streams
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();

		try {
			// create process
			ProcessBuilder pb = new ProcessBuilder();
			pb.command(arguments.toArray(new String[0]));
			if(env != null) pb.environment().putAll(env);
			if(workingdir != null) pb.directory(new File(workingdir));
			Process p = pb.start();
			// block until we get a return
			while(p.isAlive()) {
				Thread.sleep(1);
				// read streams
				this.readStream(p.getInputStream(), out, false);
				this.readStream(p.getErrorStream(), err, false);
			}
			// empty streams
			this.readStream(p.getErrorStream(), out, true);
			this.readStream(p.getErrorStream(), err, true);
			info.err = err.toString();
			info.out = out.toString();
			info.exit = p.exitValue();
			return info;
		}
		catch(Exception e) { e.printStackTrace();}
		
		info.printInfo(this.LOGGER, true);
		System.exit(1);
		return null;
	}
}
