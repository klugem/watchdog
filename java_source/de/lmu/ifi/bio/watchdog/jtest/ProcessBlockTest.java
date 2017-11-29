package de.lmu.ifi.bio.watchdog.jtest;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

import de.lmu.ifi.bio.watchdog.executor.WatchdogThread;
import de.lmu.ifi.bio.watchdog.helper.Mailer;
import de.lmu.ifi.bio.watchdog.helper.PatternFilenameFilter;
import de.lmu.ifi.bio.watchdog.helper.returnType.ReturnType;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask2TaskThread;
import javafx.util.Pair;

@RunWith(Parameterized.class)
public class ProcessBlockTest {
	private static final String XSD_FILE = "watchdog/xsd/watchdog.xsd";
	private static final String INPUT_FILES = "watchdog/jUnit_test_data/processBlock";
	private static final String XML_ENDING = ".xml";
	private static final String VALIDATE_ENDING = ".validate";
	private static final String TAB = ";;";
	private static final String SEP = "_";
	private static final String ARG_SEP = "','";
	private static final String XML_PATTERN = "*" + XML_ENDING;
	private final File TEST_FILE;
	private final File VALIDATE_FILE;
	private final HashMap<String, HashSet<String>> VAL = new HashMap<>();
	private final HashMap<Integer, Integer> NUMBER_GROUPS = new HashMap<>();
	private static int ok = 0;
	private static int tested = 0;
	
	/**
	 * Constructor
	 * @param test
	 * @param validate
	 * @throws IOException 
	 */
	public ProcessBlockTest(File test, File validate) throws IOException {
	    this.TEST_FILE = test;
	    this.VALIDATE_FILE = validate;
	    this.VAL.putAll(this.getResultFromFile());
	}
	
	public static void failPrint(String message) {
		System.out.println("failed!");
		System.out.println(message);
		fail(message);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void test() throws SAXException, IOException, ParserConfigurationException {
		tested++;
		System.out.print("Testing '"+this.TEST_FILE.getAbsolutePath()+"'...");
		Object[] ret = XMLParser.parse(this.TEST_FILE.getAbsolutePath(), XSD_FILE, 0, false, false, false);
		ArrayList<XMLTask> xmlTasks = (ArrayList<XMLTask>) ret[0];
		String mail = (String) ret[1];
		HashMap<String, Pair<HashMap<String, ReturnType>, String>> retInfo = (HashMap<String, Pair<HashMap<String, ReturnType>, String>>) ret[3];
		WatchdogThread watchdog = new WatchdogThread(true, null, new File("watchdog/xsd/watchdog.xsd"), null); 		
		XMLTask2TaskThread xml2taskThread = new XMLTask2TaskThread(watchdog, xmlTasks, new Mailer(mail), retInfo, this.TEST_FILE, 0);
		
		for(int i = 0; i < xmlTasks.size(); i++) {
			XMLTask x = xmlTasks.get(i);
			LinkedHashMap<String, ArrayList<String>> list = xml2taskThread.getArgumentLists(x);
			
			if(list.size() != this.NUMBER_GROUPS.get(i+1))
				failPrint("Test and validate lists do not have the same number of input group names!");
			
			for(String key : list.keySet()) {
				if(!this.validateReturnValues(i+1, key, list.get(key))) {
					failPrint("Input group name'"+key+"' had not the expected return value!");
				}
			}
		}
		System.out.println("success!");
		ok++;
	}
	
	@After 
	public void stats() {
		if(data().size() == tested)
			System.out.println(ok + "/" + tested + " tests succeeded!");
	}
	
	@Parameters
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
	    // run through all file
	    PatternFilenameFilter patternMatcher = new PatternFilenameFilter(XML_PATTERN, false);
		for(File f : new File(INPUT_FILES).listFiles(patternMatcher)) {
			if(f.canRead() && f.isFile()) {
				data.add(new Object[] {f, new File(f.getAbsolutePath().replaceFirst(XML_ENDING, VALIDATE_ENDING))});
			}
		}
	    return data;
	}
	
	private boolean validateReturnValues(int id, String key, ArrayList<String> replacedValues) {
		if(!this.VAL.containsKey(id + SEP + key)) 
			failPrint("Validation file does not contain a input group name'"+key+"'!");
		
		HashSet<String> compare = new HashSet<String>(this.VAL.get(id + SEP + key));
		if(replacedValues.size() != compare.size())
			failPrint("Test and validate do not have the same number of arguments for group name '"+key+"'!");
		else {
			for(String a : replacedValues) {
				if(!compare.contains(a)) 
					failPrint("Validate arguments do not contain '"+a+"'!");
			}
		}
		return true;
	}
	
	/**
	 * reads the expected result from a file
	 * @return
	 * @throws IOException
	 */
	private HashMap<String, HashSet<String>> getResultFromFile() throws IOException {
		HashMap<String, HashSet<String>> ret = new HashMap<>();
		for(String line : Files.readAllLines(FileSystems.getDefault().getPath(this.VALIDATE_FILE.getAbsolutePath()))) {
			String[] tmp = line.split(TAB);
			HashSet<String> args = new HashSet<>();
			for(String arg : tmp[2].replaceFirst("^'", "").replaceFirst("'$", "").split(ARG_SEP))
				args.add(arg);
			
			ret.put(tmp[0] + SEP + tmp[1], args);
			int id = Integer.parseInt(tmp[0]);
			if(!this.NUMBER_GROUPS.containsKey(id))
				this.NUMBER_GROUPS.put(id, 0);
			
			this.NUMBER_GROUPS.put(id, this.NUMBER_GROUPS.get(id)+1);
		}
		return ret;
	}
}
