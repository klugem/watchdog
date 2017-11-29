package de.lmu.ifi.bio.watchdog.helper;

import java.util.LinkedHashSet;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.helper.returnType.ReturnType;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

public class ErrorCheckerStore implements XMLDataStore {

	private static final long serialVersionUID = -2760027949489747649L;
	
	private final String FULL_CLASS_NAME;
	private final String PATH_TO_FILE;
	private final ErrorCheckerType TYPE;
	private final LinkedHashSet<Pair<ReturnType, String>> ARGS = new LinkedHashSet<>();
	
	public ErrorCheckerStore(String fullClassName, String path2file, ErrorCheckerType type, LinkedHashSet<Pair<ReturnType, String>> args) {
		this.FULL_CLASS_NAME = fullClassName;
		this.PATH_TO_FILE = path2file;
		this.TYPE = type;
		
		if(args != null) {
			this.ARGS.addAll(args);
		}
	}
	
	public String getFullClassName() {
		return this.FULL_CLASS_NAME;
	}
	
	public String getPathToClassFile() {
		return this.PATH_TO_FILE;
	}
	
	public ErrorCheckerType getType() {
		return this.TYPE;
	}
	
	@SuppressWarnings("unchecked")
	public LinkedHashSet<Pair<ReturnType, String>> getArguments() {
		return (LinkedHashSet<Pair<ReturnType, String>>) this.ARGS.clone();
	}
	
	@Override
	public String toXML() {
		XMLBuilder x = new XMLBuilder();
		// start with basic tag
		x.startTag(XMLParser.CHECKER, false);
		x.addQuotedAttribute(XMLParser.CLASS_PATH, this.PATH_TO_FILE);
		x.addQuotedAttribute(XMLParser.CLASS_NAME, this.FULL_CLASS_NAME);
		x.addQuotedAttribute(XMLParser.TYPE, this.TYPE);
		x.endOpeningTag();
		
		// add optional attributes
		for(Pair<ReturnType, String> data : this.ARGS) {
			x.startTag(XMLParser.C_ARG, true, true);
			x.addQuotedAttribute(XMLParser.TYPE, data.getKey().getType());
			x.addContentAndCloseTag(data.getValue());
		}
		
		// close environment tag
		x.endCurrentTag();
		return x.toString();
	}

	@Override
	public String getName() {
		return this.FULL_CLASS_NAME;
	}

	@Override
	public void setColor(String c) { }

	@Override
	public String getColor() { return null; }

	@Override
	public void onDeleteProperty() {}

	@Override
	public Object[] getDataToLoadOnGUI() { return null; }
}
