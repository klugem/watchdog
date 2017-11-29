package de.lmu.ifi.bio.watchdog.GUI.helper;

import java.util.Optional;

import de.lmu.ifi.bio.watchdog.helper.GetPassword;
import de.lmu.ifi.bio.watchdog.helper.SSHPassphraseAuth;
import de.lmu.ifi.bio.watchdog.logger.Logger;

public class GUIPasswortGetter implements GetPassword {

	@Override
	public byte[] requestPasswortInputFromUser(String name, Logger log) throws Exception {
		boolean ok = false;
		while(!ok) {
			PasswordRequest request = new PasswordRequest("the remote executer '"+ name +"'");
			System.gc();
		    Optional<String> result = request.showAndWait();
		    if(result.isPresent()) {
		    	return SSHPassphraseAuth.char2byte(result.get().toCharArray());
		    }
		    request = null;
		    System.gc();
		}
		return null;
	}
}
