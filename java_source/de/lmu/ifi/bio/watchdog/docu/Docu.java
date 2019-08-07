package de.lmu.ifi.bio.watchdog.docu;

import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Base Class used for documentation of Watchdog modules
 * @author Michael Kluge
 *
 */
public abstract class Docu {
	protected final String NAME;
	protected final String TYPE;
	protected String desc;
	private int minVersion = 0;
	private int maxVersion = 0;
	
	/**
	 * Constructor
	 * @param name name of the parameter
	 * @param type base type of the parameter
	 * @param description  description
	 */
	public Docu(String name, String type, String description) {
		this.NAME = name;
		this.TYPE = type;
		this.desc = description;
	}
	
	public void setVersions(int min, int max) {
		this.minVersion = min;
		this.maxVersion = max;
	}

	public String getName() {
		return this.NAME;
	}
	public String getType() {
		return this.TYPE;
	}
	public void setDescription(String description) {
		this.desc = description;
	}
	public String getDescription() {
		return this.desc;
	}
	public int getMinVersion() {
		return this.minVersion;
	}
	public int getMaxVersion() {
		return this.maxVersion;
	}
	
	protected XMLBuilder getBaseXMLAttributes(String tagname) {
		XMLBuilder b = new XMLBuilder();
		b.startTag(tagname, false);
		b.addQuotedAttribute(DocuXMLParser.NAME, this.getName());
		b.addQuotedAttribute(DocuXMLParser.TYPE, this.getType());
		int mi = this.getMinVersion();
		int ma = this.getMaxVersion();
		if(mi != 0) 
			b.addQuotedAttribute(XMLParser.MIN_VERSION_ATTR, mi);
		if(ma != 0)
			b.addQuotedAttribute(XMLParser.MAX_VERSION_ATTR, ma);
		return b;
	}
	
	public abstract String toXML();
}