package de.lmu.ifi.bio.watchdog.helper;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.helper.returnType.ReturnType;
import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessBlock;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask;

/**
 * Container class for error or success checker
 * @author Michael Kluge
 *
 */
public class CheckerContainer {
	private static final Logger LOGGER = new Logger(LogLevel.INFO);
	
	@SuppressWarnings("rawtypes")
	private final Constructor C;
	@SuppressWarnings("rawtypes")
	private final ArrayList<Pair<Class, String>> ARGS = new ArrayList<>();
	private final File FILE;
	private boolean IS_ERROR_CHECKER;
	
	/**
	 * Constructor
	 * @param c
	 * @param args
	 */
	@SuppressWarnings("rawtypes")
	public CheckerContainer(Constructor c, ArrayList<Pair<Class, String>> args, boolean errorChecker, File classFile) {
		this.C = c;
		this.ARGS.addAll(args);
		this.FILE = classFile;
		this.IS_ERROR_CHECKER = errorChecker;
	}
	
	public ErrorCheckerStore convert2ErrorCheckerStore() {
		@SuppressWarnings("rawtypes")
		Class c = this.C.getDeclaringClass();
		LinkedHashSet<Pair<ReturnType, String>> args = new LinkedHashSet<>();
		for(@SuppressWarnings("rawtypes") Pair<Class, String> p : this.ARGS) {
			args.add(Pair.of(ReturnType.getRetunType(p.getKey()) , p.getValue()));
		}
		return new ErrorCheckerStore(c.getCanonicalName(), this.FILE.getAbsolutePath(), this.IS_ERROR_CHECKER ? ErrorCheckerType.ERROR : ErrorCheckerType.SUCCESS, null);
	}
	
	/**
	 * tries to create a checker
	 * @param inputReplacement
	 * @param nameMapping
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public Object getChecker(Task t, String inputReplacement, HashMap<String, Integer> nameMapping, ProcessBlock pb, int spawnedTasks) {
		String workingDir = XMLTask.hasXMLTask(t.getTaskID()) ? XMLTask.getXMLTask(t.getTaskID()).getExecutor().getWorkingDir() : "";
		ArrayList<Object> args = new ArrayList<>();
		args.add(t);
		for(Pair<Class, String> p : this.ARGS) {
			Class c = p.getKey();
			String value = p.getValue();

			// only try to replace parameters
			if(inputReplacement != null) {
				value = ReplaceSpecialConstructs.replaceValues(value, inputReplacement, pb != null ? pb.getClass() : null, spawnedTasks + 1, nameMapping, workingDir, false);
			}

			// try to parse the parameter
			try {
				if(c.equals(Integer.class))
					args.add(Integer.parseInt(value));
				else if(c.equals(Double.class))
					args.add(Double.parseDouble(value));
				else if(c.equals(Boolean.class))
					args.add(Boolean.parseBoolean(value));
				else
					args.add(value);
			}
			catch(Exception e) {
				LOGGER.error("Failed to parse value '"+value+"' in order to create a instance of a checker.");
				e.printStackTrace();
			}
		}
		
		try {
			Object checker = this.C.newInstance(args.toArray(new Object[0]));
			return checker;
		}
		catch(Exception e) {
			LOGGER.error("Failed to create a instance of a error checker!");
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	public boolean isErrorChecker() {
		return this.IS_ERROR_CHECKER;
	}
}
