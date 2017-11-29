package de.lmu.ifi.bio.watchdog.GUI.helper;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;

public class PasswordRequest extends Dialog<String> {
	 private PasswordField pass = new PasswordField();
	 
	 public PasswordRequest(String name) {
		 this.setTitle("Password");
		 this.setHeaderText("Please enter your password for "+name+".");
		 this.pass.setPadding(new Insets(15));
		 
		 this.getDialogPane().setContent(this.pass);
		 getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		 
		 this.setResultConverter(button -> {
		      if(button.equals(ButtonType.OK)) {
		        return this.pass.getText();
		      }
		      return ""; // avoid any null pointer exceptions followed later on
		}); 
		 
		Platform.runLater(() -> this.pass.requestFocus());
	 }
	 
	 public String getPassword() {
		 String pass = this.pass.getText();
		 this.pass.setText("");
		 this.pass = null;
		 System.gc();
		 return pass;
	 }
}
