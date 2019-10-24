package de.lmu.ifi.bio.watchdog.GUI.helper;

import java.util.Optional;

import de.lmu.ifi.bio.watchdog.helper.UserConfirmationInterface;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import javafx.scene.control.ButtonType;

/**
 * get confirmation from GUI
 * @author kluge
 *
 */
public class GUIConfirmation implements UserConfirmationInterface {

	@Override
	public boolean confirm(String question, Logger log) {
		log.info(question);
		Optional<ButtonType> result = Inform.confirm(question);
		if (result.get() == ButtonType.OK) {
			return true;
		}
		return false;
	}
}