package de.lmu.ifi.bio.watchdog.helper.returnType;

public class BooleanReturnType extends ReturnType {

	private static final long serialVersionUID = -2688612627417351596L;
	private static final String BOOLEAN = "boolean";
	private static final String TRUE = "true";
	private static final String FALSE = "false";
	public static final BooleanReturnType TYPE = new BooleanReturnType();

	public BooleanReturnType() {
		super(BOOLEAN);
	}

	@Override
	public boolean checkType(String check) {
		return check.toLowerCase().equals(TRUE) || check.toLowerCase().equals(FALSE);
	}
}