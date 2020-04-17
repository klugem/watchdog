package de.lmu.ifi.bio.watchdog.GUI.datastructure;

import javafx.scene.control.Button;

public class DeleteButton {
	private Button b;
	
	public Button getButton() {
		return b;
	}
	DeleteButton(Button button) {
		this.b = button;
	}

	public void setButton(Button button) {
		this.b = button;
	}
}
