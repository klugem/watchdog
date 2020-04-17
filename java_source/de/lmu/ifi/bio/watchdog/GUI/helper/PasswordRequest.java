package de.lmu.ifi.bio.watchdog.GUI.helper;

import javafx.scene.control.PasswordField;

public class PasswordRequest extends InputRequest<PasswordField> {
	 
	 public PasswordRequest(String name) {
		 super("Password", "Please enter your password for "+name+".", new PasswordField());
	 }
	 
	 public String getInput() {
		 throw new IllegalArgumentException("Do not use getInput() method in PasswordRequest class!");
	 }
	 
	 public String getPassword() {
		 String pass = super.getInput();
		 System.gc();
		 return pass;
	 }
}
