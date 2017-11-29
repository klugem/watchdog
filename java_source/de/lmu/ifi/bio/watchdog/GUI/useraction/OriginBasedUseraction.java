package de.lmu.ifi.bio.watchdog.GUI.useraction;

import java.io.Serializable;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.GUI.layout.RasteredGridPane;

public class OriginBasedUseraction extends Useraction implements Serializable {

	private static final long serialVersionUID = -7527357613703651995L;
	protected final Pair<Integer, Integer> ORIGIN;
	
	public OriginBasedUseraction(String key) {
		this.ORIGIN = RasteredGridPane.getPostion(key);
	}
	
	public OriginBasedUseraction(int x, int y) {
		this.ORIGIN = Pair.of(x, y);
	}

	public Pair<Integer, Integer> getCoordinates() {
		return this.ORIGIN;
	}
	
	public String getKey() {
		return RasteredGridPane.getKey(this.ORIGIN.getKey(), this.ORIGIN.getValue());
	}
}
