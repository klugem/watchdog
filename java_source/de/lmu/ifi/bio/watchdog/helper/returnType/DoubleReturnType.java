package de.lmu.ifi.bio.watchdog.helper.returnType;

public class DoubleReturnType extends ReturnType {

	private static final long serialVersionUID = -2101616732901729354L;
	private static final String DOUBLE = "double";
	public static final DoubleReturnType TYPE = new DoubleReturnType();
	
	public DoubleReturnType() {
		super(DOUBLE);
	}

	@Override
	public boolean checkType(String check) {
		try { 
			Double.parseDouble(check); 
			return true;
		}
		catch(Exception e) {
			return false;
		}
	}
}
