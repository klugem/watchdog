package de.lmu.ifi.bio.watchdog.docu.extractor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringEscapeUtils;

import de.lmu.ifi.bio.watchdog.docu.Moduledocu;
import de.lmu.ifi.bio.watchdog.docu.Paramdocu;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * extracts parameter for python files that use the argparse library for parameter definition
 * @author kluge
 *
 */
public class PythonArgparseParameterExtractor implements Extractor<Paramdocu> {
	
	public static final String EQUAL = "\\s*=\\s*";
	public static final String SPACE_START = "^\\s+";
	public static final String SPACE_END = "\\s+$";
	public static final String QUOTE_REMOVE = "^([\"'])(.*)\\1$";
	public static final String PARAM_REMOVE = "^-+";
	public static final String PARAM_REMOVE_MATCH = "^-+[^-]+";
	public static final String HELP = "help";
	public static final String NONE = "None";
	public static final String DEFAULT = "default";
	public static final String FILE_PATTERN = ".+\\.py$";
	public static final String QUOTE = "(^[\"'])|([\"']$)";
	public static final String END_PATTERN = ".*\\.parse_args\\(.*\\).*";
	public static final String VERSION_TAG_PATTERN = ".*#VER_TAG:\\s?([0-9]+)\\-([0-9]+).*";
	public static final String ARG_PATTERN = ".*\\.add_argument\\((.+)\\){0,1}\\s*";
	public static final String ARG_END_PATTERN = ".*\\)\\s*$";
	public static final String ARGSPARSE_DETECT = "^\\s*import argparse.*";
	public static final String SEP = "\\s*,\\s*";
	
	@Override
	public File findTarget(DocumentBuilderFactory dbfXSD, File moduleDir, File xsdFile) {
		try { 
			String command = XMLParser.getTaskCommand(dbfXSD, xsdFile);
			File local = new File(moduleDir.getAbsolutePath() + File.separator + command);
			if(local.isFile() && canBeApplied(local))
				return local;
		}
		catch(Exception e) {
			// should not fail
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	
	@Override
	public boolean canBeApplied(File f) {
		try {
			return f.getName().matches(FILE_PATTERN) && (Files.readAllLines(Paths.get(f.getAbsolutePath())).stream().filter(l -> l.matches(ARGSPARSE_DETECT)).toArray().length == 1);
		}
		catch(Exception ex) {}
		return false;
	}
	
	@Override
	public Moduledocu updateDocu(Moduledocu prev, LinkedHashMap<String, ArrayList<Paramdocu>> extracted) {
		for(String key : extracted.keySet()) {
			ArrayList<Paramdocu> pd = extracted.get(key);
			if(pd.size() > 1) {
				System.out.println("Support of multiple parameters with the same name ("+key+") not implemented yet (will be implemented if eve required).");
				pd.stream().map(x -> x.toXML()).forEach(System.out::println);
				System.exit(1);
			}
			Paramdocu ex = pd.get(0);
			Paramdocu p = prev.getParameter(key);
			if(p != null) {
				if((p.getMinVersion() == ex.getMinVersion() || p.getMinVersion() == 0) && (p.getMaxVersion() == ex.getMaxVersion() || p.getMaxVersion() == 0))
				// extract description and default value
				if(ex.getDescription() != null) 
					p.setDescription(StringEscapeUtils.escapeHtml4(ex.getDescription()));
				if(ex.getDefault() != null) 
					p.setDefault(ex.getDefault());
			}
		}
		return prev;
	}
	
	public static void addvalue(String value, ArrayList<String> names, HashMap<String, String> options) {
		if(value == null)
			return;
		value = value.replaceFirst(SPACE_START, "").replaceFirst(SPACE_END, "");
		String tmp[] = value.split(EQUAL, 2);
		// key=value
		if(tmp.length == 2) {
			// remove quotes if there are any
			tmp[1] = tmp[1].replaceFirst(QUOTE_REMOVE, "$2");
			options.put(tmp[0], tmp[1]);
		}
		// parameter name
		else {
			value = value.replaceFirst(QUOTE_REMOVE, "$2");
			if(value.matches(PARAM_REMOVE_MATCH)) {
				value = value.replaceAll(PARAM_REMOVE, "");
				names.add(value);
			}
		}
	}
	
	@Override
	public LinkedHashMap<String, ArrayList<Paramdocu>> getDocu(File f) throws Exception {
		Pattern ver = Pattern.compile(VERSION_TAG_PATTERN);
		Pattern argPattern = Pattern.compile(ARG_PATTERN);
		LinkedHashMap<String, ArrayList<Paramdocu>> ret = new LinkedHashMap<>();
		List<String> lines = Files.readAllLines(Paths.get(f.getAbsolutePath()));
		int minVersion = 0;
		int maxVersion = 0;
		
		// get parameter
		boolean inSingle = false;
		boolean inDouble = false;
		ArrayList<String> names = new ArrayList<>();
		HashMap<String, String> options = new HashMap<>();
		boolean isEndOfMatch = false;
		for(String l : lines) {
			Matcher m = argPattern.matcher(l);
			// check, if arguments are parsed now
			if(l.matches(END_PATTERN)) {
				break;
			}
			else if(!isEndOfMatch || m.matches()) {
				// test if line ends with ')'
				if(l.matches(ARG_END_PATTERN))
					isEndOfMatch = true;
				if(m.matches())
					l = m.group(1);
				l = l.replaceFirst(SPACE_START, "").replaceFirst(SPACE_END, "").replaceFirst("\\)$", "");

				char[] ch = l.toCharArray();
				char c;
				int start = 0;
				for(int i =0; i < ch.length; i++) {
					c = ch[i];
					// ignore next char
					if(c == '\\') {
						i++;
						continue;
					}
					if(c == '\'' && !inDouble)
						inSingle = !inSingle;
					else if(c == '"' && !inSingle)
						inDouble = !inDouble;
					else if(c == ',') {
						// found end of segment
						if(!inSingle && !inDouble) {
							addvalue(l.substring(start, i), names, options);
							start = i+1;
						}
					}
				}
				// add last element
				if(!inSingle && !inDouble && start != ch.length)
					addvalue(l.substring(start), names, options);

				// process data and clear elements content
				if(isEndOfMatch) {
					// get as much info as possible
					for(String name : names) {
						String defaultValue = options.get(DEFAULT);
						String description = options.get(HELP);

						if(!(defaultValue != null && defaultValue.length() > 0 && !defaultValue.equals(NONE))) 
							defaultValue = "";
							
						// save it
						Paramdocu p = new Paramdocu(name, "n.a.", description, defaultValue, "[VALUE_RESTRICTION]");
						if(minVersion != 0 && maxVersion != 0)
							p.setVersions(minVersion, maxVersion);
						
						// save in hash
						if(!ret.containsKey(name))
							ret.put(name, new ArrayList<>());
						ArrayList<Paramdocu> a = ret.get(name);
						a.add(p);
					}
					
					// do some clean up
					isEndOfMatch = false;
					inSingle = false;
					inDouble = false;
					names.clear();
					options.clear();
				}
			}
			// check, if we have some meta version anno
			else {
				Matcher mv = ver.matcher(l);
				if(mv.matches()) {
					minVersion = Integer.parseInt(mv.group(1));
					maxVersion = Integer.parseInt(mv.group(2));
				}
			}
		}
		return ret;
	}
}
