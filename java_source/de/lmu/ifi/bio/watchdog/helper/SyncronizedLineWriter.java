package de.lmu.ifi.bio.watchdog.helper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SyncronizedLineWriter {
	private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy - hh:mm:ss");
	private final PrintWriter WRITER;
	
	private final boolean NO_STORE;
	
	/** 
	 * do not store the information we input anywhere
	 */
	public SyncronizedLineWriter() {
		this.NO_STORE = true;
		this.WRITER = null;
	}
	
	/**
	 * Constructor
	 * @param file
	 * @throws FileNotFoundException
	 */
	public SyncronizedLineWriter(File file) throws FileNotFoundException {
		this.WRITER = new PrintWriter(file);
		this.NO_STORE = false;
	}
	
	/**
	 * Constructor
	 * @param buffer
	 * @throws FileNotFoundException
	 */
	public SyncronizedLineWriter(BufferedWriter buffer) throws FileNotFoundException {
		this.WRITER = new PrintWriter(buffer);
		this.NO_STORE = false;
	}
	
	/**
	 * Writes a new entry in the log file
	 * @param line
	 * @param execName
	 */
	public synchronized void writeLog(String line, String execName, String id, String action) {
		if(!this.NO_STORE) {
			Date date = new Date();
			this.WRITER.write("[" + DATE_FORMAT.format(date) + "," + execName + "," + id + "] " + action + ": " + line);
			this.WRITER.flush();
		}
	}
	
	public synchronized void close() {
		if(!this.NO_STORE) {
			this.WRITER.flush();
			this.WRITER.close();
		}
	}
}
