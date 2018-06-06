package de.lmu.ifi.bio.utils.interfaces;

import javafx.event.Event;

public interface EventDistributor {

	/**
	 * distributes a event
	 * @param e
	 */
	public void distribute(Event e);
}
