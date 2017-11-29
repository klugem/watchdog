package de.lmu.ifi.bio.watchdog.GUI.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import de.lmu.ifi.bio.watchdog.GUI.css.GUIFormat;
import de.lmu.ifi.bio.watchdog.GUI.interfaces.Validator;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;

public class ValidationGroupType<T extends Control> {
	
	private final ArrayList<T> VALIDATE_CONTROL = new ArrayList<>();
	private final ArrayList<Validator<T>> VALIDATE_HANDLER = new ArrayList<>();
	private final HashMap<String, String> VALIDATED_VALUES = new HashMap<>();
	private ValidationGroupType() {}; // hide constructor
	
	@SuppressWarnings("unchecked")
	public void addValidateToControl(T c, Validator<T>... validators) {
		for(Validator<T> v : validators) {
			this.VALIDATE_CONTROL.add(c);
			this.VALIDATE_HANDLER.add(v);
		}
	}
	
	/**
	 * validates if all input is correct
	 * @return
	 */
	public boolean validate() {
		// get rid of old values
		this.VALIDATED_VALUES.clear();
		
		boolean okAll = true;
		HashSet<T> blackList = new HashSet<>();
		
		// validate all controls
		for(int i = 0; i < this.VALIDATE_CONTROL.size(); i++) {
			T c = this.VALIDATE_CONTROL.get(i);
			if(!blackList.contains(c)) {
				Validator<T> v = this.VALIDATE_HANDLER.get(i);
				// might be indented to get values for stuff that must not be validated really...
				boolean ok = true;
				if(v != null) {
					ok = v.validate(c);
					// color the stuff
					GUIFormat.colorControl(c, ok);
					okAll = okAll && ok;
				}
				
				// collect data from the ones with names
				if(c != null && c.getUserData() != null && c.getUserData() instanceof String) {
					String name = c.getUserData().toString();
					String value = null;
					if(c instanceof TextField) 
						value = ((TextField) c).getText();
					else if(c instanceof ChoiceBox && ((ChoiceBox<?>) c).getSelectionModel().getSelectedItem() != null)
						value = ((ChoiceBox<?>) c).getSelectionModel().getSelectedItem().toString();
					else if(c instanceof CheckBox) 
						value = Boolean.toString(((CheckBox) c).isSelected());
					
					// save it 
					this.VALIDATED_VALUES.put(name, value);
				}
				
				// add a negative candidate to the blacklist
				if(!ok)
					blackList.add(c);
			}
		}
		return okAll;
	}
	
	public HashMap<String, String> getValidatedValues() {
		return this.VALIDATED_VALUES;
	}
	
	@SuppressWarnings("rawtypes")
	public static ValidationGroupType getValidationGroupType(Control c) {
		if(c == null)
			return new ValidationGroupType<Control>();
		else if(c instanceof TextField)
			return new ValidationGroupType<TextField>();
		else if(c instanceof ChoiceBox)
			return new ValidationGroupType<ChoiceBox>();
		else if(c instanceof CheckBox)
			return new ValidationGroupType<CheckBox>();
		else {
			System.out.println("ValidationGroupType factory is not implemented for type '"+c.getClass().getName()+"'");
			System.exit(1);
		}
		return null;
	}	
}
