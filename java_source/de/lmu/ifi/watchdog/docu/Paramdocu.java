
package de.lmu.ifi.watchdog.docu;

import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;

/**
 * Class used for parameter documentation of Watchdog modules
 * @author Michael Kluge
 *
 */
public class Paramdocu extends Docu {
	private static String SEP = "-";
	
	private String defaultV = null;
	private String restrictions = null;
	private int minOccurs = 1;
	private int maxOccurs = 1;
	
	/**
	 * Constructor
	 * @param name name of the parameter
	 * @param type base type of the parameter
	 * @param description  description
	 * @param defaultValue default value if applicable
	 * @param valueRestrictions value restrictions on the parameter if some exist
	 */
	public Paramdocu(String name, String type, String description, String defaultValue, String valueRestrictions) {
		super(name, type, description);
		this.defaultV = defaultValue;
		this.restrictions = valueRestrictions;
	}
	
	public Paramdocu(String name, String type) {
		super(name, type, null);
	}

	public String getRestriction() {
		return this.restrictions;
	}
	public String getDefault() {
		return this.defaultV;
	}
	public void setRestriction(String valueRestrictions) {
		this.restrictions = valueRestrictions;
	}
	public void setDefault(String defaultValue) {
		this.defaultV = defaultValue;
	}
	public int getMinOccurs() {
		return this.minOccurs;
	}
	public int getMaxOccurs() {
		return this.maxOccurs;
	}	
	public boolean isOptional() {
		return this.getMinOccurs() == 0;
	}
	public void setOccurs(Integer min, Integer max) {
		if(min != null) this.minOccurs = min;
		if(max != null) this.maxOccurs = max;
	}
	
	@Override
	public String toXML() {
		XMLBuilder b = this.getBaseXMLAttributes(DocuXMLParser.PARAM);
		b.noNewlines(true);
		if(this.hasRestrictions()) b.addQuotedAttribute(DocuXMLParser.RESTRICTIONS, this.getRestriction());
		if(this.hasDefault()) b.addQuotedAttribute(DocuXMLParser.DEFAULT, this.getDefault());
		b.addQuotedAttribute(DocuXMLParser.MIN_OCCURS, this.getMinOccurs());
		b.addQuotedAttribute(DocuXMLParser.MAX_OCCURS, this.getMaxOccurs());
		b.startTag(DocuXMLParser.DESCRIPTION, true);
		b.endOpeningTag();
		b.addContent(this.getDescription(), false);
		b.endCurrentTag();
		return b.toString();
	}

	private boolean hasRestrictions() {
		return this.getRestriction() != null && this.getRestriction().length() > 0;
	}

	private boolean hasDefault() {
		return this.getDefault() != null && this.getDefault().length() > 0;
	}

	public String getOccurenceInfo() {
		if(this.getMinOccurs() == this.getMaxOccurs())
			return Integer.toString(this.getMinOccurs());
		if(this.isOptional())
			return "*";
		else
			return this.getMinOccurs() + SEP + this.getMaxOccurs();
	}
}
