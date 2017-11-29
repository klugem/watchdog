package de.lmu.ifi.bio.watchdog.optionFormat;

/**
 * Allows different formats of parameters.
 * @author Michael Kluge
 *
 */
public enum ParamFormat {
	shortOnly, longShort, plain;
	
	public static final String LONG_PREFIX = "--";
	public static final String SHORT_PREFIX = "-";
	
	/**
	 * formats a parameter name based on the type of the ParamFormat
	 * @param parameterName
	 * @return
	 */
	public String formatParameter(String parameterName, boolean isFlag) {
		// plain format
		if(!isFlag && this.equals(ParamFormat.plain)) {
			return "";
		}
		// all short prefix
		else if(this.equals(ParamFormat.shortOnly)) {
			return SHORT_PREFIX + parameterName;
		}
		// long prefix for all parameter names with a length greater than 1
		else {
			if(parameterName.length() == 1) {
				return SHORT_PREFIX + parameterName;
			}
			else {
				return LONG_PREFIX + parameterName;
			}
		}
	}
	
	/**
	 * returns a formater based on a string
	 * @param paramFormatString
	 * @return
	 */
	public static ParamFormat getFormater(String paramFormatString) {
		if(ParamFormat.plain.toString().equals(paramFormatString))
			return ParamFormat.plain;
		else if(ParamFormat.shortOnly.toString().equals(paramFormatString))
			return ParamFormat.shortOnly;
		// return default value
		return ParamFormat.longShort;
	}
}
