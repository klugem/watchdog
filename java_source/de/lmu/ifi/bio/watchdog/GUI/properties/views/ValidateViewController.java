package de.lmu.ifi.bio.watchdog.GUI.properties.views;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.MessageType;
import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.StatusConsole;
import de.lmu.ifi.bio.watchdog.GUI.css.GUIFormat;
import de.lmu.ifi.bio.watchdog.GUI.helper.ValidationGroupType;
import de.lmu.ifi.bio.watchdog.GUI.interfaces.Validator;
import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import de.lmu.ifi.bio.watchdog.helper.returnType.FileReturnType;
import de.lmu.ifi.bio.watchdog.helper.returnType.ReturnType;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public abstract class ValidateViewController implements Initializable {

		@FXML protected Button saveButton;
		
		protected final static String CONDITION_SEP = "-§-";
		protected final LinkedHashMap<String, ValidationGroupType<Control>> VAL_GROUPS = new LinkedHashMap<>();
		protected StatusConsole status;
		protected boolean isDataLoaded = false;
		private final HashMap<String, String> VALIDATED_VALUES = new HashMap<>();
		private final HashMap<String, String> LAST_SAVED_VALUES = new HashMap<>();
		protected SimpleBooleanProperty hasUnsavedData = new SimpleBooleanProperty(false);
		protected SimpleBooleanProperty validatedData = new SimpleBooleanProperty(false);
		
		private boolean dataChangedUsed = false; // indicates if data change function is used or not
		
		@Override
		public void initialize(URL location, ResourceBundle resources) {
			this.saveButton.setDisable(true);
			this.saveButton.onActionProperty().set(e -> this.onSave());
			this.saveButton.setGraphic(ImageLoader.getImage(ImageLoader.SAVE_SMALL));
		}
		
		public void setStatusConsole(StatusConsole s) {
			this.status = s;
			this.validate();
		}
		
		public StatusConsole getStatusConsole() {
			return this.status;
		}
		
		public SimpleBooleanProperty getUnsavedDataBoolean() {
			this.dataChangedUsed = true; // apparently someone is using it ;)
			return this.hasUnsavedData;
		}
		
		public SimpleBooleanProperty getValidatedDataBoolean() {
			return this.validatedData;
		}
		
		@SuppressWarnings("unchecked")
		public void addValidateToControl(Control c, String name, Validator<Control> v, String condition) {
			String type;
			if(c == null)
				type = Control.class.getName();
			else
				type = c.getClass().getName();
			
			if(condition != null)
				type = type + CONDITION_SEP + condition;
			else
				type = type + CONDITION_SEP;
			
			// add new validation group, if not already there
			if(!this.VAL_GROUPS.containsKey(type))
				this.VAL_GROUPS.put(type, ValidationGroupType.getValidationGroupType(c));
		
			// add the validator
			this.VAL_GROUPS.get(type).addValidateToControl(c, v);
			if(c != null)
				c.setUserData(name);
		}
		
		protected void addValidateToControl(Control c, String name, Validator<Control> v) {
			this.addValidateToControl(c, name, v, null);
		}
		
		/**
		 * validates all validation groups
		 * @return
		 */
		protected boolean validate(String condition) {
			this.VALIDATED_VALUES.clear();
			
			if(this.status != null)
				this.status.clear();
			boolean ok = true;
			
			if(condition == null)
				condition = CONDITION_SEP;
			else 
				condition = CONDITION_SEP + condition;
			
			for(String type : this.VAL_GROUPS.keySet()) {
				if(type.endsWith(CONDITION_SEP) || type.endsWith(condition)) {
					ValidationGroupType<? extends Control> vg = this.VAL_GROUPS.get(type);
					ok = vg.validate() && ok;
					this.VALIDATED_VALUES.putAll(vg.getValidatedValues());
				}
			}
			
			// check, if the data was changed
			boolean changedData = this.wasValidateDataChanged();
			// ensure that the change listeners are called...
			this.hasUnsavedData.set(!changedData);
			this.validatedData.set(!ok);
			// set the real data
			this.hasUnsavedData.set(changedData);
			this.validatedData.set(ok);
			
			// update status of save button
			this.saveButton.setDisable(!ok);
			return ok;
		}
		
		/**
		 * true, if the data was changed true the last save after the last validate call
		 * @return
		 */
		private boolean wasValidateDataChanged() {
			if(!this.dataChangedUsed)
				return false;
			
			HashMap<String, String> save = new HashMap<String, String>(this.LAST_SAVED_VALUES);
			HashMap<String, String> validated = new HashMap<String, String>(this.VALIDATED_VALUES);

			if(save.size() != validated.size()) 
				return true;
			
			for(String k : save.keySet()) {
				if(validated.containsKey(k) && validated.get(k).equals(save.get(k))) {
					validated.remove(k);
				}
				else
					return true;
			}

			if(validated.size() > 0)
				return true;
			return false;
		}

		protected boolean validate() {
			return this.validate(null);
		}
		
		protected void onSave() {
			this.onSave(null);
		}
		
		public void saveLastSavedData4Validate() {
			this.validate();
			this.LAST_SAVED_VALUES.clear();
			this.LAST_SAVED_VALUES.putAll(this.VALIDATED_VALUES);
			this.hasUnsavedData.set(false);
		}
		
		/**
		 * is called, when the user clicks on the save button
		 */
		protected void onSave(String condition) {
			if(this.validate(condition)) {
				this.saveData();
				this.close();
			}
		}
		
		public void close() {
			Stage stage = (Stage) this.saveButton.getScene().getWindow();
			stage.close();
		}
			
		public boolean isEmpty(TextField box) {
			return box.getText().isEmpty();
		}
		
		public boolean isEmpty(TextField box, String text) {
			if(box == null)
				return true;
			GUIFormat.colorTextField(box, box.getText() != null && !box.getText().isEmpty()); // color the stuff correctly
			if(box.getText() == null)
				return true;
			if(!this.isEmpty(box))
				return false;
			else 
				this.addMessageToPrivateLog(MessageType.WARNING, text);
			return true;
		}
		
		public boolean isInteger(TextField box, String text) {
			boolean isInt = true;
			try { Integer.parseInt(box.getText()); } catch (Exception e) { isInt = false; };

			GUIFormat.colorTextField(box, isInt); // color the stuff correctly
			if(!isInt)
				this.addMessageToPrivateLog(MessageType.WARNING, text);
			return isInt;
		}
		
		public boolean isDouble(TextField box, String text) {
			boolean isDouble = true;
			try { Double.parseDouble(box.getText()); } catch (Exception e) { isDouble = false; };

			GUIFormat.colorTextField(box, isDouble); // color the stuff correctly
			if(!isDouble)
				this.addMessageToPrivateLog(MessageType.WARNING, text);
			return isDouble;
		}
		
		public boolean isValidReturnType(TextField box, ReturnType type, String text) {
			boolean ret = type.checkType(box.getText());
			GUIFormat.colorTextField(box, ret); // color the stuff correctly
			if(!ret)
				this.addMessageToPrivateLog(MessageType.WARNING, text);
			return ret;
		}
		
		private boolean testFileFolder(TextField box, String text, boolean isFile, boolean isAbsolute, boolean updateColorAndAddText) {
			// determine the file type to test
			FileReturnType test = null;
			if(isFile) {
				if(isAbsolute) test = FileReturnType.AB_FILE;
				else test = FileReturnType.RE_FILE;
			} else {
				if(isAbsolute) test = FileReturnType.AB_FOLDER;
				else test = FileReturnType.RE_FOLDER;
			}
			
			// make the test
			boolean ret = test.checkType(box.getText());
			if(updateColorAndAddText) {
				GUIFormat.colorTextField(box, ret); // color the stuff correctly
				if(!ret)
					this.addMessageToPrivateLog(MessageType.WARNING, text);
			}
			return ret;
		}
		
		public boolean isAbsoluteFile(TextField box, String text) {
			return testFileFolder(box, text, true, true, true);
		}
		
		public boolean isAbsoluteFolder(TextField box, String text) {
			return testFileFolder(box, text, false, true, true);
		}
		
		public boolean isRelativeFile(TextField box, String text) {
			return testFileFolder(box, text, true, false, true);
		}
		
		public boolean isRelativeFolder(TextField box, String text) {
			return testFileFolder(box, text, false, false, true);
		}
		
		public boolean isAbsoluteOrRelativeFolder(TextField box, String text, boolean isOptional) {
			return isAbsoluteOrRelativeFolder(box, text, false, isOptional);
		}
		
		public boolean isAbsoluteOrRelativeFile(TextField box, String text, boolean isOptional) {
			return isAbsoluteOrRelativeFolder(box, text, true, isOptional);
		}
		
		private boolean isAbsoluteOrRelativeFolder(TextField box, String text, boolean isFile, boolean isOptional) {
			if(isOptional && box.getText().isEmpty())
				return true;
			
			boolean abs = testFileFolder(box, text, isFile, true, false);
			boolean rel = testFileFolder(box, text, isFile, false, false);
			
			boolean ret = abs || rel;
			if(!ret) {
				GUIFormat.colorTextField(box, ret); // color the stuff correctly
				if(!ret)
					this.addMessageToPrivateLog(MessageType.WARNING, text);
			}
			return ret;
		}

		public boolean isEmpty(TextField box, int row, String name) {
			return this.isEmpty(box, "Row " + row + " is missing a value for column named '"+name+"'.");
		}
		
		protected boolean addMessageToPrivateLog(MessageType type, String text) {
			if(this.status == null)
				return false;
			else {
				if(this.status.size() == 0) {
					this.status.addInfoMessage("Please take notice of the following messages in order to finish task configuration successfully: ");
				}
				this.status.addMessage(type, text);
			}
			return true;
		}
		
		/**
		 * returns the name for the save dialog
		 * @return
		 */
		public abstract String getSaveName();

		/**
		 * saves the data
		 */
		protected void saveData() {
			// Check, if message should be showed
			if(this.getSaveName() != null)
				StatusConsole.addGlobalMessage(MessageType.INFO, this.getSaveName() +" with name '"+this.getStoredData().getName()+"' was saved");
		}
		
		public abstract XMLDataStore getStoredData();
		
		/**
		 * true, if property is currently loaded; can be used to disable eventhandler
		 * @return
		 */
		protected boolean isDuringLoadProcess() {
			return this.isDataLoaded;
		}
	}
