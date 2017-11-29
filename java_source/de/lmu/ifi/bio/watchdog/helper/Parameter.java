package de.lmu.ifi.bio.watchdog.helper;

import java.io.Serializable;

import de.lmu.ifi.bio.watchdog.helper.returnType.BooleanReturnType;
import de.lmu.ifi.bio.watchdog.helper.returnType.ReturnType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;

public class Parameter implements Serializable {

	private static final long serialVersionUID = -5111660871955667788L;
	private final ReturnType TYPE;
	private final String NAME;
	private final Integer MIN_O;
	private final Integer MAX_O;
	
	public Parameter(String name, Integer minO, Integer maxO, ReturnType r) {
		this.NAME = name;
		this.TYPE = r;
		this.MIN_O = minO;
		this.MAX_O = maxO;
	}
	
	public String getName() {
		return this.NAME;
	}
	public ReturnType getType() {
		return this.TYPE;
	}
	public Integer getMin() {
		return this.MIN_O;
	}
	public Integer getMax() {
		return this.MAX_O;
	}
	
	public boolean isOptional() {
		return this.MIN_O == 0;
	}
	public boolean isUnbounded() {
		return this.MAX_O == null;
	}
	public boolean isNumberCountValid(int count) {
		boolean minOK = this.MIN_O <= count;
		boolean maxOK = this.isUnbounded() || count <= this.MAX_O;
		return minOK && maxOK;
	}

	public Control getControlElement() {
		if(this.TYPE instanceof BooleanReturnType) 
			return new CheckBox();
		else
			return new TextField();
	}

	public boolean isOnlySingleInstanceAllowed() {
		return (this.getMax() != null && this.getMax() == 1);
	}
}
