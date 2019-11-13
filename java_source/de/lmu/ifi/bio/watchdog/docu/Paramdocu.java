
package de.lmu.ifi.bio.watchdog.docu;

import org.apache.commons.lang3.StringEscapeUtils;

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
	private Integer minOccurs = 1;
	private Integer maxOccurs = 1;
	
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
		this.defaultV = StringEscapeUtils.escapeHtml4(defaultValue);
		this.restrictions = StringEscapeUtils.escapeHtml4(valueRestrictions);
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
		this.restrictions = StringEscapeUtils.escapeHtml4(valueRestrictions);
	}
	public void setDefault(String defaultValue) {
		this.defaultV = StringEscapeUtils.escapeHtml4(defaultValue);
	}
	public Integer getMinOccurs() {
		return this.minOccurs;
	}
	public Integer getMaxOccurs() {
		return this.maxOccurs;
	}	
	public boolean isOptional() {
		return this.getMinOccurs() == 0;
	}
	public void setOccurs(Integer min, Integer max) {
		if(min != null) this.minOccurs = min;
		this.maxOccurs = max;
	}
	
	@Override
	public String toXML() {
		XMLBuilder b = this.getBaseXMLAttributes(DocuXMLParser.PARAM);
		b.noNewlines(true);
		if(this.hasRestrictions()) b.addQuotedAttribute(DocuXMLParser.RESTRICTIONS, this.getRestriction());
		if(this.hasDefault()) b.addQuotedAttribute(DocuXMLParser.DEFAULT, this.getDefault());
		b.addQuotedAttribute(DocuXMLParser.MIN_OCCURS, this.getMinOccurs());
		// unbounded if null
		if(this.getMaxOccurs() != null)
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
		if(this.getMaxOccurs() != null) {
			if(this.getMinOccurs() == this.getMaxOccurs())
				return Integer.toString(this.getMinOccurs());
			else 
				return "*";	
		}
		else {
			if(this.isOptional())		
				return this.getMinOccurs() + SEP + this.getMaxOccurs();
			else
				return this.getMinOccurs() + SEP;
		}
	}
}
