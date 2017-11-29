package de.lmu.ifi.bio.watchdog.optionFormat;

/**
 * Formats options and flags
 * @author Michael Kluge
 *
 */
public class OptionFormat {

	private final ParamFormat PARAM_FORMAT;
	private final QuoteFormat QUOTE_FORMAT;
	private final SpacingFormat SPACING_FORMAT;
	
	/**
	 * Constructor
	 * @param paramFormat
	 * @param quoteFormat
	 * @param spacingFormat
	 */
	public OptionFormat(ParamFormat paramFormat, QuoteFormat quoteFormat, SpacingFormat spacingFormat) {
		this.PARAM_FORMAT = paramFormat;
		this.QUOTE_FORMAT = quoteFormat;
		
		// do not allow null values for the spacing format
		if(spacingFormat == null) {
			this.SPACING_FORMAT = SpacingFormat.blankSeperated;
		}
		else {
			this.SPACING_FORMAT = spacingFormat;
		}
	}
	
	/**
	 * Formats a flag
	 * @param flagName
	 * @return
	 */
	public String formatFlag(String flagName) {
		return this.PARAM_FORMAT.formatParameter(flagName, true);
	}
	
	/**
	 * Formats a parameter
	 * @param paramName
	 * @param value
	 * @return
	 */
	public String[] formatParameter(String paramName, String value) {
		if(this.PARAM_FORMAT != null)
			paramName = this.PARAM_FORMAT.formatParameter(paramName, false);
	
		if(this.QUOTE_FORMAT != null)
			value = this.QUOTE_FORMAT.quoteValue(value);
		
		return this.SPACING_FORMAT.formatSpacing(paramName, value);
	}
	
	/**
	 * prefix of the parameter formater
	 * @return
	 */
	public String getParamPrefix() {
		return (this.PARAM_FORMAT == null || this.PARAM_FORMAT.formatParameter("", false).length() == 0) ? null : this.PARAM_FORMAT.formatParameter("", false);
	}

	public String getFlagPrefix() {
		return (this.PARAM_FORMAT == null) ? "" : this.PARAM_FORMAT.formatParameter("", true);
	}

	public String getQuote() {
		return (this.QUOTE_FORMAT == null || (this.QUOTE_FORMAT.equals(QuoteFormat.unquoted)) ? null : ((Character) this.QUOTE_FORMAT.quoteValue("").charAt(0)).toString());
	}
}
