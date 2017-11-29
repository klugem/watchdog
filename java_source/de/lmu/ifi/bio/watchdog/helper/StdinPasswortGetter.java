package de.lmu.ifi.bio.watchdog.helper;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;

import de.lmu.ifi.bio.watchdog.logger.Logger;

public class StdinPasswortGetter implements GetPassword {

	@Override
	public byte[] requestPasswortInputFromUser(String name, Logger log) throws Exception {
		Console c = System.console();
		if(c == null) {
			if(System.in != null) {
				// might be a eclipse run --> try to read from stdin
			    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			    System.out.println("######################################");
				System.out.println("Your private key for the remote executer '"+ name +"' is secured by a passphrase (well done!). Please enter the passphrase to use the key:");
				System.gc();
				System.out.println("[WARNING] YOUR password will be displayed as plain text as no real console is associated with this java vm!");
				System.out.println("######################################");
				char[] p = new String(reader.readLine()).toCharArray();
				return SSHPassphraseAuth.char2byte(p);
			}
			else {
				log.error("No console is associated with that java vm. Because of that the pass-phrase for the remote executor can not be entered!");
				System.exit(1);
			}
		}
		else {
			log.info("Your private key for the remote executer '"+ name +"' is secured by a passphrase (well done!). Please enter the passphrase to use the key:");
			char[] p = c.readPassword();
			return SSHPassphraseAuth.char2byte(p);
		}
		return null;
	}

}
