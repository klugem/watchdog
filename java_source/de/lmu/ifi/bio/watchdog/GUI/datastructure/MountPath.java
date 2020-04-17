package de.lmu.ifi.bio.watchdog.GUI.datastructure;

import javafx.scene.control.Button;

public class MountPath extends DeleteButton {
	private final String HOST;
	private final String CONTAINER;
	
	public MountPath(String host, Button b) {
		super(b);
		this.HOST = host;
		this.CONTAINER = "";
	}
	
	public MountPath(String host, String container, Button b) {
		super(b);
		this.HOST = host;
		if(host.equals(container))
			container = null;
		this.CONTAINER = (container == null ? "" : container);
	}
	
	public String getHost() {
		return this.HOST;
	}
	
	public String getContainer() {
		return this.CONTAINER;
	}
	
	@Override
	public String toString() {
		return this.getHost();
	}
}
