package de.lmu.ifi.bio.watchdog.errorChecker;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.bio.watchdog.helper.returnType.ReturnType;
import de.lmu.ifi.bio.watchdog.interfaces.ErrorChecker;
import de.lmu.ifi.bio.watchdog.task.Task;

public class ParameterReturnErrorChecker extends ErrorChecker implements Serializable {

	private static final long serialVersionUID = -1225957239576183024L;
	private final static String LAST_LINE = "?EOF!";
	private final static int WAIT = 25; // wait time in ms for files
	private final static int COUNTER = 2400; // counter
	private final static String TAB = "\t"; 
	
	private final HashMap<String, String> COLLECTED_RETURN_PARAMETER = new HashMap<>();
	private final HashMap<String, ReturnType> EXPECTED_PARAMETER;
	private final File PARAM_FILE;
	
	/**
	 * Constructor
	 * @param t
	 * @param expectedParams
	 * @param paramFile
	 */
	public ParameterReturnErrorChecker(Task t, HashMap<String, ReturnType> expectedParams, File paramFile) {
		super(t);
		this.EXPECTED_PARAMETER = new HashMap<String, ReturnType>(expectedParams);
		this.PARAM_FILE = paramFile;
	}
	
	/**
	 * checks, if errors could be found in the file and if so adds them
	 * @param f
	 */
	private void checkFile(File f) {
		if(this.T.getExitStatus() != null && this.T.getExitStatus() == 0) {
			int i = 0;
			// check, if file is there and give it a few seconds until file is there
			while(!f.exists() || !f.canRead()) {
				try { Thread.sleep(WAIT); } catch(Exception e) { break; }
				i++;

				if(i > COUNTER) {
					this.ERRORS.add("Return parameter file '"+f.getAbsolutePath()+"' could not be opened after 10 minutes of wait time!");
					LOGGER.error("Return parameter file '"+f.getAbsolutePath()+"' could not be opened after 10 minutes of wait time!");
					return;
				}
			}
			// file is there, ensure that last line is ?EOF!
			try {
				List<String> lines;
				while(true) {
					i++;
					lines = Files.readAllLines(Paths.get(f.getAbsolutePath()));
					if(LAST_LINE.equals(lines.get(lines.size()-1))) {
						lines.remove(lines.size()-1);
						break;
					} // newline after that line
					else if(LAST_LINE.equals(lines.get(lines.size()-2))) {
						lines.remove(lines.size()-1);
						lines.remove(lines.size()-1);
						break;
					}
	
					// give the file system time to write that file...
					try { Thread.sleep(WAIT); } catch(Exception e) { break; }
					if(i > COUNTER) {
						this.ERRORS.add("Return parameter file '"+f.getAbsolutePath()+"' does not end with '"+LAST_LINE+"' after 10 minutes of wait time!");
						LOGGER.error("Return parameter file '"+f.getAbsolutePath()+"' does not end with '"+LAST_LINE+"' after 10 minutes of wait time!");
						return;
					}
				}
			
				String[] tmp;
				String name, value;
				ReturnType r;
				for(String line : lines) {
					// try to split the line at the separator
					tmp = line.split(TAB);
	
					if(tmp.length == 1 || tmp.length == 2) {
						name = tmp[0];
						
						if(tmp.length == 1)
							value = "NULL";
						else
							value = tmp[1];
						
						// check, if param is expected
						if(this.EXPECTED_PARAMETER.containsKey(name)) {
							r = this.EXPECTED_PARAMETER.get(name);
							if(r.checkType(value)) {
								// delete, the parameter, because all is ok
								this.EXPECTED_PARAMETER.remove(name);
								
								// add it to the collected parameters
								this.COLLECTED_RETURN_PARAMETER.put(name,  value);
							}
							else {
								this.ERRORS.add("Parameter with name '"+name+"' has not the correct type '"+r.getType()+"' ('"+value+"').");
							}
						}
					}
				}
				HashMap<String, String> c = new HashMap<>();
				c.putAll(this.COLLECTED_RETURN_PARAMETER);
				c.put("task", this.T.getID());
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			// check if some of the parameters were not given
			for(String name : this.EXPECTED_PARAMETER.keySet()) {
				this.ERRORS.add("Parameter with name '"+name+"' was not found in return parameter file '"+this.PARAM_FILE.getAbsolutePath()+"'.");
			}
		}
	}

	@Override
	public boolean hasTaskFailed() {
		if(!this.wasCheckPerformed()) {
			// perform the checking
			this.checkFile(this.PARAM_FILE);
			// mark that check was performed
			this.checkWasPerformed();
			
			// set the return values, if no errors were found
			if(this.ERRORS.size() == 0)
				this.T.setReturnParams(this.getValidReturnParameters());
		}
		if(this.ERRORS.size() == 0)
			this.T.setReturnParams(this.getValidReturnParameters());

		return ERRORS.size() > 0;
	}
	
	/**
	 * returns the valid return parameters 
	 * @return
	 */
	private HashMap<String, String> getValidReturnParameters() {
		return new HashMap<String, String>(this.COLLECTED_RETURN_PARAMETER);
	}
}
