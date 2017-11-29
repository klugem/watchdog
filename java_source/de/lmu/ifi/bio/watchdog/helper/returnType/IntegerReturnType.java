package de.lmu.ifi.bio.watchdog.helper.returnType;

public class IntegerReturnType extends ReturnType {

	private static final long serialVersionUID = -238600777743446008L;
	private static final String INTEGER = "integer";
	public static final IntegerReturnType TYPE = new IntegerReturnType();
	
	public IntegerReturnType() {
		super(INTEGER);
	}

	@Override
	public boolean checkType(String check) {
		try { 
			Integer.parseInt(check); 
			return true;
		}
		catch(Exception e) {
			return false;
		}
	}
}
