package de.lmu.ifi.bio.watchdog.optionFormat;

/**
 * Allows different quote formats for parameter
 * @author Michael Kluge
 *
 */
public enum QuoteFormat {
	unquoted, singleQuoted, doubleQuoted;
	
	public static final String ESCAPE = "\\\\";
	public static final String SINGLE_QUOTE = "'";
	public static final String DOUBLE_QUOTE = "\"";
	
	/**
	 * quotes an argument of a parameter based on the type of the QuoteFormat 
	 * but does not quite numbers
	 * @param value
	 * @return
	 */
	public String quoteValue(String value) {
		if(value == null) {
			try {
			throw new IllegalArgumentException("");
			}
			catch(Exception e) { e.printStackTrace(); }
		}
		// do not quote numbers
		try { 
			Double.parseDouble(value); 
			return value;
		}
		catch(Exception e) {
			// unquoted
			if(this.equals(QuoteFormat.unquoted)) {
				return value;
			}
			// single quoted with '
			else if(this.equals(QuoteFormat.singleQuoted)) {
				// mask single quote ' --> '"'"'
				value = value.replaceAll(SINGLE_QUOTE, SINGLE_QUOTE + DOUBLE_QUOTE + SINGLE_QUOTE + DOUBLE_QUOTE + SINGLE_QUOTE);
				return SINGLE_QUOTE + value + SINGLE_QUOTE;
			}
			// double quoted with " --> \"
			else {
				// mask double quote
				value = value.replaceAll(DOUBLE_QUOTE, ESCAPE + DOUBLE_QUOTE);
				return DOUBLE_QUOTE + value + DOUBLE_QUOTE;
			}
		}	
	}

	/**
	 * returns a formater based on a string
	 * @param paramFormatString
	 * @return
	 */
	public static QuoteFormat getFormater(String quoteFormatString) {
		if(QuoteFormat.unquoted.toString().equals(quoteFormatString))
			return QuoteFormat.unquoted;
		else if(QuoteFormat.singleQuoted.toString().equals(quoteFormatString))
			return QuoteFormat.singleQuoted;
		// return default value
		return QuoteFormat.doubleQuoted;
	}
}
