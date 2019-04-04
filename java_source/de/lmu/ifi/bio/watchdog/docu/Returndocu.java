package de.lmu.ifi.bio.watchdog.docu;

import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;

/**
 * Class used for return value documentation of Watchdog modules
 * @author Michael Kluge
 *
 */
public class Returndocu extends Docu {
	
	/**
	 * Constructor
	 * @param name name of the parameter
	 * @param type base type of the parameter
	 * @param description  description
	 */
	public Returndocu(String name, String type, String description) {
		super(name, type, description);
	}

	@Override
	public String toXML() {
		XMLBuilder b = this.getBaseXMLAttributes(DocuXMLParser.VAR);
		b.noNewlines(true);
		b.startTag(DocuXMLParser.DESCRIPTION, true);
		b.endOpeningTag();
		b.addContent(this.getDescription(), false);
		b.endCurrentTag();
		return b.toString();
	}
}
