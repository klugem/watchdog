package de.lmu.ifi.bio.watchdog.docu.extractor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
 * extracts parameter for sh files that use the shflags library for parameter definition
 * @author kluge
 *
 */
public class SHFlagsParameterExtractor implements Extractor<Paramdocu> {
	
	public static String FILE_PATTERN = ".+\\.sh$";
	public static String QUOTE = "(^[\"'])|([\"']$)";
	public static String DEFINE = "DEFINE_";
	public static String VAR_DEF_START = "^\\s*" + DEFINE + "[(string)|(boolean)|(float)|(integer)].+";
	public static String VERSION_TAG_PATTERN = ".*#VER_TAG:\\s?([0-9]+)-([0-9]+).*";
	
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
		return f.getName().matches(FILE_PATTERN);
	}
	
	@Override
	public Moduledocu updateDocu(Moduledocu prev, LinkedHashMap<String, ArrayList<Paramdocu>> extracted) {
		for(String key : extracted.keySet()) {
			ArrayList<Paramdocu> pd = extracted.get(key);
			if(pd.size() > 1) {
				System.out.println("Support of multiple parameters with the same name not implemented yet (will be implemented if eve required).");
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
	
	@Override
	public LinkedHashMap<String, ArrayList<Paramdocu>> getDocu(File f) throws Exception {
		Pattern ver = Pattern.compile(VERSION_TAG_PATTERN);
		LinkedHashMap<String, ArrayList<Paramdocu>> ret = new LinkedHashMap<>();
		List<String> lines = Files.readAllLines(Paths.get(f.getAbsolutePath()));
		int minVersion = 0;
		int maxVersion = 0;
		for(String l : lines) {
			if(l.matches(VAR_DEF_START)) {
				l = l.replaceFirst("^\\s+", "");
				boolean inSingle = false;
				boolean inDouble = false;
				ArrayList<String> segments = new ArrayList<>();
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
					else if(c == ' ') {
						// found end of segment
						if(!inSingle && !inDouble) {
							segments.add(l.substring(start, i));
							start = i+1;
						}
					}
				}
				// add last element
				if(!inSingle && !inDouble && start != ch.length)
					segments.add(l.substring(start));

				// get as much info as possible
				if(segments.size() == 4 || segments.size() == 5) {
					String type = segments.get(0).replace(DEFINE, "").replaceAll(QUOTE, "");
					String name = segments.get(1).replaceAll(QUOTE, "");
					String defaultValue = segments.get(2).replaceAll(QUOTE, "");
					String description = segments.get(3).replaceAll(QUOTE, "");
					
					if(type.equals("boolean")) {
						if(defaultValue.equals("1"))
							defaultValue = "false";
						else if(defaultValue.equals("0"))
							defaultValue = "true";
					}
					// save it
					Paramdocu p = new Paramdocu(name, type, description, defaultValue, "[VALUE_RESTRICTION]");
					if(minVersion != 0 || maxVersion != 0)
						p.setVersions(minVersion, maxVersion);
					if(defaultValue.length() > 0) 
						p.setDefault(defaultValue);
					
					// save in hash
					if(!ret.containsKey(name))
						ret.put(name, new ArrayList<>());
					ArrayList<Paramdocu> a = ret.get(name);
					a.add(p);
				}
			}
			// check, if we have some meta version anno
			else {
				Matcher m = ver.matcher(l);
				if(m.matches()) {
					minVersion = Integer.parseInt(m.group(1));
					maxVersion = Integer.parseInt(m.group(2));
				}
			}
		}
		return ret;
	}
}
