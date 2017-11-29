package de.lmu.ifi.bio.watchdog.GUI.useraction;

import java.io.Serializable;

import de.lmu.ifi.bio.watchdog.GUI.layout.RasteredGridPane;
import javafx.util.Pair;

public class OriginBasedUseraction extends Useraction implements Serializable {

	private static final long serialVersionUID = -7527357613703651995L;
	protected final Pair<Integer, Integer> ORIGIN;
	
	public OriginBasedUseraction(String key) {
		this.ORIGIN = RasteredGridPane.getPostion(key);
	}
	
	public OriginBasedUseraction(int x, int y) {
		this.ORIGIN = new Pair<>(x, y);
	}

	public Pair<Integer, Integer> getCoordinates() {
		return this.ORIGIN;
	}
	
	public String getKey() {
		return RasteredGridPane.getKey(this.ORIGIN.getKey(), this.ORIGIN.getValue());
	}
}
