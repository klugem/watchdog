package de.lmu.ifi.bio.watchdog.helper;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;
import java.io.Reader;

import de.lmu.ifi.bio.watchdog.logger.Logger;

/**
 * get confirmation from CMD
 * @author kluge
 *
 */
public class CMDConfirmation implements UserConfirmationInterface {

	@Override
	public boolean confirm(String question, Logger log) {
		// print the question
		log.info(question);
		
		// get the reader to read from
		Reader reader = null;
		Console c = System.console();
		if(c == null) {
			if(System.in != null) {
				// might be a eclipse run --> try to read from stdin
			    reader = new BufferedReader(new InputStreamReader(System.in));
			}
			else {
				log.error("No console is associated with that java vm. Hence no input can not be entered!");
			}
		}
		else {
			reader = c.reader();
		}
		
		// try to get confirmation
		if(reader != null) {
			log.info("confirm by typing 'Y': ");
			
			// read input
			try {
				int b;
				boolean first = true;
				while((b = reader.read()) != -1) {
					if(first) { 
						if(b == ((int) 'Y') && ((b = reader.read()) == 10 || b == 13)) {
							return true; 
						}
						first = false;
					}
				}
			}
			catch(Exception ex) { ex.printStackTrace(); }
			return false;
		}
		else {
			log.error("Obtained reader is null. Hence no input can not be entered!");
		}
		return false;
	}
}
