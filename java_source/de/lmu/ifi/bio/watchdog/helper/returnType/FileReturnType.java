package de.lmu.ifi.bio.watchdog.helper.returnType;

import java.io.File;
import java.util.regex.Pattern;

import de.lmu.ifi.bio.watchdog.helper.ReplaceSpecialConstructs;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

public class FileReturnType extends ReturnType {

	private static final long serialVersionUID = -3140215882256919724L;
	private static final String FILE = "file";
	
	protected static String ABSOLUTE_FOLDER = "paramAbsoluteFolderPath";
	protected static String ABSOLUTE_FILE = "paramAbsoluteFilePath";
	protected static String RELATIVE_FOLDER = "paramRelativeFolderPath";
	protected static String RELATIVE_FILE = "paramRelativeFilePath";
	protected static String FILENAME = "paramFilename";
	
	protected static Pattern REGEX_ABSOLUTE_FOLDER = Pattern.compile("(^/([^/]+/+)*$)|(^[A-Z]:\\\\([^\\\\]+\\\\+)*$)");
	protected static Pattern REGEX_ABSOLUTE_FILE = Pattern.compile("(^/([^/]+/+)*[^/]+$)|(^[A-Z]:\\\\([^\\\\]+\\\\+)*[^\\\\]+$)");
	protected static Pattern REGEX_RELATIVE_FOLDER = Pattern.compile("(^([^/]+/+)*$)|(^([^\\\\]+\\\\+)*$)");
	protected static Pattern REGEX_RELATIVE_FILE = Pattern.compile("(^([^/]+/+)*[^/]+$)|(^([^\\\\]+\\\\+)*[^\\\\]+$)");
	
	public static FileReturnType AB_FILE = new FileReturnType(ABSOLUTE_FILE);
	public static FileReturnType AB_FOLDER = new FileReturnType(ABSOLUTE_FOLDER);
	public static FileReturnType RE_FILE = new FileReturnType(RELATIVE_FILE);
	public static FileReturnType RE_FOLDER = new FileReturnType(RELATIVE_FOLDER);
	
	private final String FILE_TYPE;
	
	public FileReturnType(String type) {
		super(FILE);
		this.FILE_TYPE = type;
	}

	@Override
	public boolean checkType(String check) {
		if(check == null || check.length() == 0)
			return false;

		// check for constants --> ok
		if(XMLParser.PATTERN_CONST.matcher(check).matches())
			return true;
		// check for return parameters
		else if(ReplaceSpecialConstructs.containsReplaceRegex(check))
			return true;
		// plain checks from now
		else if(this.FILE_TYPE.equals(FILENAME))
			return check.length() > 0 && !check.contains(File.separator);
		else if(this.FILE_TYPE.equals(ABSOLUTE_FOLDER))
			return REGEX_ABSOLUTE_FOLDER.matcher(check).matches();
		else if(this.FILE_TYPE.equals(ABSOLUTE_FILE))
			return REGEX_ABSOLUTE_FILE.matcher(check).matches();
		else if(this.FILE_TYPE.equals(RELATIVE_FOLDER))
			return REGEX_RELATIVE_FOLDER.matcher(check).matches();
		else if(this.FILE_TYPE.equals(RELATIVE_FILE))
			return REGEX_RELATIVE_FILE.matcher(check).matches();

		return false;
	}
	
	@Override 
	public String toString() {
		return this.FILE_TYPE.replace("param", "").replaceAll("([A-Z])", " $1").toLowerCase();
	}
}
