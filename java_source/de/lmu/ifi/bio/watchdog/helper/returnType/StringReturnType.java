package de.lmu.ifi.bio.watchdog.helper.returnType;

public class StringReturnType extends ReturnType {

	private static final long serialVersionUID = -6800315951893562775L;
	private static final String STRING = "string";
	public static final StringReturnType TYPE = new StringReturnType();
	
	public StringReturnType() {
		super(STRING);
	}

	@Override
	public boolean checkType(String check) {
		return check != null;
	}
}
