package de.lmu.ifi.bio.watchdog.helper;

import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

public class Constants implements XMLDataStore {

	private static final long serialVersionUID = -6857131455109209861L;
	
	private final String NAME;
	private final String VALUE;
	
	public Constants(String name, String value) {
		this.NAME = name;
		this.VALUE = value;
	}

	@Override
	public String toXML() {
		XMLBuilder x = new XMLBuilder();
		x.startTag(XMLParser.CONST, false);
		x.addQuotedAttribute(XMLParser.NAME, this.getName());
		x.addContentAndCloseTag(this.getValue());
		return x.toString();
	}

	@Override
	public String getName() {
		return this.NAME;
	}

	public String getValue() {
		return this.VALUE;
	}
	
	@Override
	public void setColor(String c) {}
	@Override
	public String getColor() {return null;}

	@Override
	public void onDeleteProperty() {}
}
