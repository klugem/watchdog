package de.lmu.ifi.bio.watchdog.GUI.helper;

import java.util.ArrayList;

import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;

public class ParamValue {

	private final String NAME;
	private final Control CONTROL;
	private String value = null;
	private final Integer NUMBER;
	
	public ParamValue(String name, Control c, Integer number) {
		this.NAME = name;
		this.CONTROL = c;
		this.NUMBER = number;
	}
	
	/**
	 * creates a copy that can be used to add a additional element
	 * @return
	 */
	public ParamValue copy(Integer number) {
		if(this.isBoolean())
			return new ParamValue(this.NAME, new CheckBox(), number);
		else
			return new ParamValue(this.NAME, new TextField(), number);
	}
	
	public boolean isBoolean() {
		return this.CONTROL instanceof CheckBox;
	}
	
	public String getName() {
		return (this.NUMBER == null || this.NUMBER == 0) ? this.NAME : this.NAME + " (" + this.NUMBER + ")";
	}
	
	public String getCurrentValue() {
		if(this.isBoolean())
			return Boolean.toString(((CheckBox) this.CONTROL).isSelected());
		else
			return ((TextField) this.CONTROL).getText();
	}
	
	public String getSavedValue() {
		return this.value;
	}
	
	public void saveCurrentValue() {
		this.value = this.getCurrentValue();
	}
	
	public void deleteSavedValue() {
		this.value = null;
	}
	
	public void updateValue(String v) {
		if(this.isBoolean())
			((CheckBox) this.CONTROL).setSelected(Boolean.parseBoolean(v));
		else
			((TextField) this.CONTROL).setText(v);
	}
	
	@Override
	public String toString() {
		if(this.hasSavedValue()) {
			XMLBuilder b = new XMLBuilder();
			b.startTag(this.NAME, false);
			b.addContentAndCloseTag(this.getSavedValue());
			return b.toString();
		}
		return null;
	}

	public boolean hasSavedValue() {
		return this.getSavedValue() != null;
	}

	public Node getControl() {
		return this.CONTROL;
	}

	public String getPlainName() {
		return this.NAME;
	}

	/**
	 * split multiple parameters if required
	 * @return
	 */
	public ArrayList<ParamValue> split(boolean isOnlySingleInstanceAllowed) {
		ArrayList<ParamValue> r = new ArrayList<>();
		if(isOnlySingleInstanceAllowed)
			r.add(this);
		else {
			String[] singleParams = this.value.split(XMLTask.INTERNAL_PARAM_SEP);
			int i = 1;
			for(String v : singleParams) {
				ParamValue p = this.copy(i);
				p.value = v;
				r.add(p);
				i++;
			}
		}
		return r;
	}
}
