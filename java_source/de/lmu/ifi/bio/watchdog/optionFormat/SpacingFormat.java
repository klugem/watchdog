package de.lmu.ifi.bio.watchdog.optionFormat;

public enum SpacingFormat {
	blankSeperated, equalSeparated, notSeparated;
	
	public static final String EQUAL_SEP = "=";
	
	/**
	 * formats an option based on the type of this class
	 * @param value
	 * @return
	 */
	public String[] formatSpacing(String parameter, String value) {
		// do only return the value if the parameter value is empty.
		if(parameter.length() > 0) {
			// equal quoted
			if(this.equals(SpacingFormat.equalSeparated)) {
				return new String[] {parameter + EQUAL_SEP + value};
			}
			else if(this.equals(SpacingFormat.notSeparated)) {
				return new String[] {parameter + value};
			}
			// space quoted
			else {
				return new String[] {parameter, value};
			}
		}
		else {
			return new String[] {value};
		}
	}

	/**
	 * returns a formater based on a string
	 * @param spacingFormatString
	 * @return
	 */
	public static SpacingFormat getFormater(String spacingFormatString) {
		if(SpacingFormat.equalSeparated.toString().equals(spacingFormatString))
			return SpacingFormat.equalSeparated;
		if(SpacingFormat.notSeparated.toString().equals(spacingFormatString))
			return SpacingFormat.notSeparated;
		// return default value
		return SpacingFormat.blankSeperated;
	}
}
