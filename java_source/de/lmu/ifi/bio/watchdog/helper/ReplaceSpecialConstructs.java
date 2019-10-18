package de.lmu.ifi.bio.watchdog.helper;

import java.io.File;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import de.lmu.ifi.bio.watchdog.logger.LogLevel;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessBlock;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * helper class which replaces (), [], {} and $()
 * @author Michael Kluge
 *
 */
public class ReplaceSpecialConstructs {
	
	private static final String REGEX_PATH = "((/([^/]+/+)*[^/]+/{0,})|([A-Z]:\\\\([^\\\\]+\\\\+)*[^\\\\]+/{0,}))";
	public static final String TAB = "\t";
	private static final String MATCH_RANDOM = ".*?";
	private static final String SUBSTITUTE_FILENAME_PATTERN = "\\[([0-9]*)(,([^]\\)\\}]*)){0,1}\\]";
	private static final String SUBSTITUTE_PARENT_PATTERN = "\\(([0-9]*)(,([^]\\)\\}]*)){0,1}\\)";
	private static final String SUBSTITUTE_PATH_PATTERN = "\\{([0-9]*)(,([^]\\)\\}]*)){0,1}\\}";
	private static final String REGEX_NO_NAME =  MATCH_RANDOM + "[\\{\\[\\(][0-9]*(,([^]\\)\\}]*)){0,1}[\\}\\]\\)]" + MATCH_RANDOM;
	private static final String SUBSTITUTE_COUNTER_PATTERN = "\\$\\(((([0-9]+(\\.[0-9]+)?)|[xi]|[+\\-\\*/^²³]|[\\[\\]\\(\\)])+)\\)";
	public static final String MATCH_TABLE_NAME_PATTERN = MATCH_RANDOM + "([\\[\\(\\{](\\$([A-Za-z_0-9]+)(,\\s*){0,1})([0-9]+){0,1}(,\\s*){0,1}{0,1}(.+){0,1}[\\]\\)\\}]){1}" + MATCH_RANDOM;
	private static final String MATCH_PATH_PATTERN = MATCH_RANDOM + SUBSTITUTE_PATH_PATTERN + MATCH_RANDOM;
	private static final String MATCH_PARENT_PATTERN = MATCH_RANDOM + SUBSTITUTE_PARENT_PATTERN + MATCH_RANDOM;
	private static final String MATCH_COUNTER_PATTERN = "(" + MATCH_RANDOM + SUBSTITUTE_COUNTER_PATTERN + MATCH_RANDOM + ")+";
	private static final String MATCH_FILENAME_PATTERN = MATCH_RANDOM + SUBSTITUTE_FILENAME_PATTERN + MATCH_RANDOM;
	public static final Pattern PATTERN_PARENT = Pattern.compile(MATCH_PARENT_PATTERN);
	public static final Pattern PATTERN_PATH = Pattern.compile(MATCH_PATH_PATTERN);
	public static final Pattern PATTERN_COUNT = Pattern.compile(MATCH_COUNTER_PATTERN);
	public static final Pattern PATTERN_FILENAME = Pattern.compile(MATCH_FILENAME_PATTERN);
	public static final Pattern PATTERN_TABLE_COL_NAME = Pattern.compile(MATCH_TABLE_NAME_PATTERN);
	public static final int RESTRICT_NUMBER_SIZE = 25;

	private static final String SLASH = File.separator;
	private static final String POINT = "\\.";
	public static final DecimalFormat DF;
	private static final String X = "x";
	private static final String I = "i";
	private static final String START_EVAL = "$(";
	private static final String END_EVAL = ")";
	private static final String CLOSE_END = "\\)$";
	
	private static final Logger LOGGER = new Logger(LogLevel.INFO);
	
	static {
		DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
		dfs.setDecimalSeparator('.');
		DF = new DecimalFormat("#.00000", dfs);
		DF.setRoundingMode(RoundingMode.HALF_UP);
	}
	
	public static boolean containsReplaceRegex(String check) {
		return check.matches(REGEX_NO_NAME) || check.matches(MATCH_TABLE_NAME_PATTERN);
	}
	
	/**
	 * Replaces [], {}, (), $() and other variables in a string 
	 * @param value
	 * @param inputReplacement
	 * @param processBlockClass
	 * @param spawnedTaskCounter
	 * @param nameMapping name mapping for process table
	 * @param isEnv if true, more sensitive because it might be not intent replacement
	 * @return
	 */
	public static String replaceValues(String value, String inputReplacement, Class<? extends ProcessBlock> processBlockClass, int spawnedTaskCounter, HashMap<String, Integer> nameMapping, String tmpWorkingDir, boolean isEnv) {
		if(value == null)
			return null;
		
		// try to replace ${TMP} with tmp working directory 		
		value = value.replace("${"+XMLParser.TMP_BLOCKED_CONST+"}", tmpWorkingDir);
		value = value.replace("${"+XMLParser.WF_PARENT_BLOCKED_CONST+"}", XMLParser.getParentOfCurrentlyParsedFilePath());

		// test if value should be substituted by input of processGroup
		if(inputReplacement != null) {
			// check, if a process block type is set
			if(processBlockClass == null)
				return value;
			
			String replaceValue = null;
			String completePath = null;
			String parentPath = null;
			
			// get the correct field in case of a process table or multiple inputs
			if(nameMapping != null) {
				// check, if a name was found
				if(!isEnv && value.matches(REGEX_NO_NAME)) {
					// COMMENT: set to warn ?
					LOGGER.debug("Using {}/()/[] in the context of a process table or process input without a variable name (f.e. [$TEST] or [$TEST, 1]) is not allowed! Ensure that this value should not be replaced by a variable: '" + value + "'");
				}

				// test, if var replacement should be performed
				while(value.matches(MATCH_TABLE_NAME_PATTERN)) {
					// extract name and 
					Matcher m = PATTERN_TABLE_COL_NAME.matcher(value);
					if(m.matches()) {
						// get the matching stuff
						String name = m.group(3);
						
						// check, if variable with that name is there
						if(!nameMapping.containsKey(name)) {
							LOGGER.error("Mapping of process block does not contain a column named '" + name +"'");
							try {throw new Exception(""); } catch(Exception e) { e.printStackTrace(); }
							System.exit(1);
						}
						// replace the name
						value = value.replaceAll("\\$" + name+ "(\\s*,\\s*)?", ""); 
						int col = nameMapping.get(name);

						// recursive call
						String[] split = inputReplacement.split(TAB);
						if(split.length < col) {
							LOGGER.error("'"+inputReplacement+"' does not contain enough values.");
							LOGGER.error(StringUtils.join(nameMapping, " "));
							LOGGER.error(value);
							try {throw new Exception(""); } catch(Exception e) { e.printStackTrace(); }
							System.exit(1);
						}
						String retVal = replaceValues(value, split[col-1], processBlockClass, spawnedTaskCounter, null, tmpWorkingDir, isEnv);
						// security test
						if(retVal != null)
							value = retVal;
					}
				}
			}
			// normal replacement (not from process table)
			else {
				if(inputReplacement.matches(REGEX_PATH)) {
					inputReplacement = inputReplacement.replaceAll("/{2,}", "/");
					inputReplacement = inputReplacement.replaceAll("\\\\{2,}", "\\\\");
				}
				
				// separate file types and other values
				if(processBlockClass != null && inputReplacement.matches(REGEX_PATH)) {
					completePath = new File(inputReplacement).getParentFile().getAbsolutePath();
					replaceValue = new File(inputReplacement).getName();
					parentPath = new File(inputReplacement).getParentFile().getAbsolutePath();
				} 
				else {
					replaceValue = inputReplacement;
					parentPath = inputReplacement;
				}

				// substitute the stuff
				value = substitute(PATTERN_PATH, SUBSTITUTE_PATH_PATTERN, value, completePath, replaceValue, processBlockClass, POINT, isEnv);
				value = substitute(PATTERN_PARENT, SUBSTITUTE_PARENT_PATTERN, value, parentPath, processBlockClass, SLASH, isEnv);
				value = substitute(PATTERN_FILENAME, SUBSTITUTE_FILENAME_PATTERN, value, replaceValue, processBlockClass, POINT, isEnv);
			}
		}
		
		// check, if the value should be replaced
		if(value.contains(START_EVAL)) {
			Matcher matcher = PATTERN_COUNT.matcher(value);
			// try to match the pattern
			while(matcher.matches()) {
				String evalGroup =  matcher.group(2).replaceFirst(CLOSE_END, "");
				String replacedEvalGroup = evalGroup.replace(X, Integer.toString(spawnedTaskCounter));
				
				// test, if it is a numerical value
				boolean isNumeric = true;
				try { Double.parseDouble(inputReplacement); } catch(Exception e) { isNumeric = false; }

				// test, if replacement is valid
				if((inputReplacement == null || inputReplacement.equals("") || !isNumeric) && replacedEvalGroup.contains(I)) {
					LOGGER.error("The variable '"+I+"' is only valid in the context of a processSequence.");
					System.exit(1);
				}
				replacedEvalGroup = replacedEvalGroup.replace(I, inputReplacement);
		
				// eval the expression
				try {
					double d = de.lmu.ifi.bio.watchdog.helper.MathEval.eval(replacedEvalGroup);
					
					// let an integer be an real integer
					if(d == ((double) (int) d))
						replacedEvalGroup = Integer.toString((int) d);
					else
						replacedEvalGroup = DF.format(d);
				}
				catch(RuntimeException e) {
					LOGGER.error("Can not eval: '" + replacedEvalGroup + "'.");
					System.exit(1);
				}
				// replace the result and re-init matcher
				value = value.replace(START_EVAL + evalGroup + END_EVAL, replacedEvalGroup);
				matcher = PATTERN_COUNT.matcher(value);		
			}
		}
		return value;
	}
	
	/**
	 * Substitutes a pattern and returns a relative path
	 * @param regexMatch
	 * @param regexReplace
	 * @param inputString
	 * @param replacementCrop
	 * @param separator
	 * @return
	 */
	private static String substitute(Pattern regexMatch, String regexReplace, String inputString, String replacementCrop, Class<? extends ProcessBlock> processBlockClass, String separator, boolean isEnv) {
		return substitute(regexMatch, regexReplace, inputString, null, replacementCrop, processBlockClass, separator, isEnv);
	}
	
	/**
	 * Searches for a pattern and replaced all occurrences of it and returns a relative or absolute filename
	 * If the first group of the pattern contains a integer the last n parts of replacementCrop are removed at split 'separator'
	 * @param regexMatch
	 * @param regexReplace
	 * @param inputString
	 * @param replacementBase might be zero, if an relative path should be returned
	 * @param replacementCrop
	 * @param processBlockClass
	 * @param separator
	 * @return
	 */
	private static String substitute(Pattern regexMatch, String regexReplace, String inputString, String replacementBase, String replacementCrop, Class<? extends ProcessBlock> processBlockClass, String separator, boolean isEnv) {
		boolean matching = true;
		while(matching) {
			matching = false;			
			Matcher matcher = regexMatch.matcher(inputString);

			// check, if not meaningful in that context
			if(matcher.matches()) {
				if(processBlockClass == null) {
					LOGGER.error("Parameter replacement with " + regexReplace + " is only meaningful in the context of a process block.");
					System.exit(1);
				}
				else {
					matching = true; // do another round because in this round a substitution was made
					
					// get the number which says how many of the endings of the filename should be deleted
					String number = matcher.group(1);
					
					// check, if a separator is also given
					if(matcher.groupCount() == 3 && matcher.group(3) != null)
						separator = matcher.group(3); // replace the separator

					int n = 0;
					try { n = Integer.parseInt(number); } catch(NumberFormatException e) {}
					// be more restrictive in case of environment variables because ([0-9]+) is a common pattern
					if(!isEnv || n <= RESTRICT_NUMBER_SIZE) {					
						//create the replacement
						String replacement = Functions.removeEndings(replacementCrop, n, separator);
						if(replacementBase != null) {
							replacement = new File(replacementBase + File.separator + replacement).getAbsolutePath();
						}
						// make the replacement
						inputString = inputString.replaceFirst(regexReplace, replacement);
					}
					else {
						break;
					}
				}
			}
		}
		return inputString;
	}
	
}
