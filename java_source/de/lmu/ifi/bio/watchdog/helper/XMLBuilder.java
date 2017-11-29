package de.lmu.ifi.bio.watchdog.helper;

import java.io.File;
import java.util.ArrayList;

import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * can be used to easily create XML documents
 * @author kluge
 *
 */
public class XMLBuilder {
	private final StringBuilder B = new StringBuilder();
	private final ArrayList<String> OPEN_TAGS = new ArrayList<>();
	private final ArrayList<Boolean> HAS_TAG_CHILDREN = new ArrayList<>();
	private int newlineCounter = 0;
	/**
	 * some static XML stuff that is used internally
	 */
	private static final String START_COMMENT = "<!-- ";
	private static final String END_COMMENT = " -->";
	private static final String WATCHDOG = "watchdog";
	private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	private static final String BASE_REP = "%%%BASE%%%";
	private static final String START_WATCHDOG = "<watchdog xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"watchdog.xsd\" watchdogBase=\""+BASE_REP+"\"";
	private static boolean unsafeMode = false;
	
	/**
	 * can be used to set unsafe mode in which some null pointers are captured --> might result errors later on.
	 * @param unsave
	 */
	public static void setMode(boolean unsave) {
		XMLBuilder.unsafeMode = unsave;
	}
	
	/**
	 * starts the document with the XML header
	 */
	public void startDocument() {
		this.B.append(XML_HEADER);
		this.newline();
	}
	
	public void startWachdog(File base, boolean isValidToXSD) {
		this.B.append(START_WATCHDOG.replace(BASE_REP, base.getAbsolutePath() + File.separator));
		if(!isValidToXSD) 
			this.addQuotedAttribute(XMLParser.IS_NOT_VALID_XSD_STRING, true);
		this.B.append(XMLParser.CLOSE_TAG);
		this.newline();
		
		// add the element to the open tags
		this.OPEN_TAGS.add(WATCHDOG);
		this.HAS_TAG_CHILDREN.add(false);
	}
	
	public void startTag(String name, boolean asChild) {		
		this.startTag(name, asChild, false, true);
	}
	
	public void startTag(String name, boolean asChild, boolean oldTagAlreadClosed) {		
		this.startTag(name, asChild, oldTagAlreadClosed, true);
	}
	
	
	/**
	 * starts a new tag
	 * @param name
	 * @param asChild
	 */
	public void startTag(String name, boolean asChild, boolean oldTagAlreadClosed, boolean addNewLine) {		
		// close the current tag
		if(this.OPEN_TAGS.size() > 0 && !oldTagAlreadClosed) {
			if(asChild)
				this.B.append(XMLParser.CLOSE_TAG);
			else
				this.endCurrentTag();
		}
		
		// add the element to the open tags
		this.OPEN_TAGS.add(name);
		this.HAS_TAG_CHILDREN.add(false);
		
		// update last one
		if(asChild)
			this.HAS_TAG_CHILDREN.set(this.OPEN_TAGS.size()-2, true);
		
		// add it to the string
		if(addNewLine && B.length() > 0)
			this.newline();
		this.B.append(XMLParser.OPEN_TAG);
		this.B.append(name);
	}
		
	/**
	 * add a quoted attribute to the currently open tag
	 * @param name
	 * @param value
	 */
	public void addQuotedAttribute(String name, Object value) {
		if(XMLBuilder.unsafeMode && value == null) // we want to get an error if not in unsafe mode!
			return;
		
		this.B.append(XMLParser.SPACER);
		this.B.append(name);
		this.B.append(XMLParser.EQUAL);
		this.B.append(XMLParser.QUOTE);
		this.B.append(value.toString().replace("\"", "\\\""));
		this.B.append(XMLParser.QUOTE);
	}
	
	/**
	 * ends the current tag and adds a newline before
	 */
	public void endCurrentTag() {
		this.endCurrentTag(false);
	}
	
	/**
	 * ends the current tag
	 * @param noNewline
	 */
	public void endCurrentTag(boolean noNewline) {
		// remove the element from the open tags
		int lastIndex = this.OPEN_TAGS.size() - 1;
		String currentTag = this.OPEN_TAGS.remove(lastIndex);
		boolean hasCurrentTagChildren = this.HAS_TAG_CHILDREN.remove(lastIndex);
		
		// add it to the string
		if(hasCurrentTagChildren) {
			if(!noNewline)
				this.newline();
			this.B.append(XMLParser.OPEN_CLOSE_TAG);
			this.B.append(currentTag);
			this.B.append(XMLParser.CLOSE_TAG);
		}
		else {
			this.B.append(XMLParser.SPACER);
			this.B.append(XMLParser.CLOSE_NO_CHILD_TAG);
		}
	}
	
	/**
	 * adds some value to the current open tag and closes it afterwards
	 * @param value
	 */
	public void addContentAndCloseTag(String value) {
		this.B.append(XMLParser.CLOSE_TAG);
		this.B.append(value);
		this.HAS_TAG_CHILDREN.set(this.OPEN_TAGS.size()-1, true);
		this.endCurrentTag(true);
	}
	
	public void endOpeningTag(boolean addNewline) {
		this.HAS_TAG_CHILDREN.set(this.OPEN_TAGS.size()-1, true); // ensure that </XXX> is applied instead of />
		this.B.append(XMLParser.CLOSE_TAG);
		if(addNewline)
			this.newline();
	}
	
	public void endOpeningTag() {
		this.endOpeningTag(true);
	}
	
	public void addContent(String value, boolean addNewline) {
		this.HAS_TAG_CHILDREN.set(this.OPEN_TAGS.size()-1, true);
		this.B.append(value);
		if(addNewline)
			this.newline();
	}
	
	public void addContent(ArrayList<String> values, boolean addNewline) {
		this.HAS_TAG_CHILDREN.set(this.OPEN_TAGS.size()-1, true);
		
		for(String value : values) {
			this.B.append(value);
			if(addNewline)
				this.newline();
		}
	}
	
	public void addXMLDataStoreContent(ArrayList<XMLDataStore> data, boolean addNewline) {
		this.HAS_TAG_CHILDREN.set(this.OPEN_TAGS.size()-1, true);
		
		for(XMLDataStore d : data) {
			this.B.append(d.toXML());
			if(addNewline)
				this.newline();
		}
	}
	
	@Override
	public String toString() {
		// close all remaining open tags
		while(this.OPEN_TAGS.size() > 0)
			this.endCurrentTag(false);
		
		return this.B.toString();
	}

	public void addComment(String comment) {
		this.newline();
		this.B.append(START_COMMENT);
		this.B.append(comment);
		this.B.append(END_COMMENT);
		this.newline();
	}

	public void newline() {
		this.B.append(XMLParser.NEWLINE);
		this.newlineCounter++;
	}
	
	public int getNewlines() {
		return this.newlineCounter;
	}


}
