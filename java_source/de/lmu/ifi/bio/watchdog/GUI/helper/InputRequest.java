package de.lmu.ifi.bio.watchdog.GUI.helper;

import de.lmu.ifi.bio.watchdog.GUI.css.CSSRessourceLoader;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;

public class InputRequest<T extends TextField> extends Dialog<String> {
	 protected T input;
	 
	 public InputRequest(String title, String header, T t) {
		 try { this.getDialogPane().getScene().getStylesheets().add(CSSRessourceLoader.getCSS("control.css")); }
		 catch(Exception e) {}
		 
		 this.input = t;
		 this.setTitle(title);
		 this.setHeaderText(header);
		 this.input.setPadding(new Insets(15));
		 
		 this.getDialogPane().setContent(this.input);
		 this.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		
		 this.setResultConverter(button -> {
		      if(button.equals(ButtonType.OK)) {
		        return this.input.getText();
		      }
		      this.input.setText("");
		      return ""; // avoid any null pointer exceptions followed later on
		}); 
		 
		Platform.runLater(() -> this.input.requestFocus());
	 }
	 
	 public void setDisable(boolean disable) {
		 this.getDialogPane().lookupButton(ButtonType.OK).setDisable(disable);
	 }
	 
	 public String getInput() {
		 String i = this.input.getText();
		 this.input.setText("");
		 this.input = null;
		 return i;
	 }
}
