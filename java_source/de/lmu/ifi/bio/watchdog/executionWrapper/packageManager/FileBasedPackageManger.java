package de.lmu.ifi.bio.watchdog.executionWrapper.packageManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

import de.lmu.ifi.bio.watchdog.executionWrapper.ExecutionWrapper;

/**
 * Abstract class that loads the before and after commands from files and adapts them to the used operating system
 * @author kluge
 *
 */
public abstract class FileBasedPackageManger extends ExecutionWrapper {
	
	private static final long serialVersionUID = 1765948465959477484L;
	public static final String VAR_PREFIX="WATCHDOG_";
	public static final String EQ="=";

	public FileBasedPackageManger(String name, String watchdogBaseDir) {
		super(name, watchdogBaseDir);
	}
	
	@Override
	public boolean isPackageManager() { return true; }

	/**
	 * adds variables at the beginning of the the list
	 * all variables are prefixed with VAR_PREFIX
	 * @return
	 */
	protected ArrayList<String> addVariables(HashMap<String, String> vars) {
		return this.addVariables(vars, null);
	}
	
	/**
	 * adds variables at the beginning of the the list
	 * all variables are prefixed with VAR_PREFIX
	 * @return
	 */
	protected ArrayList<String> addVariables(HashMap<String, String> vars, ArrayList<String> l) {
		if(vars == null) return l;
		
		ArrayList<String> r = new ArrayList<>();
		for(String k : vars.keySet()) {
			String v = vars.get(k);
			r.add(VAR_PREFIX + k + EQ + v);
		}
		if(l == null) return r;
		else {
			r.addAll(l);
			return r;
		}
	}

	/**
	 * tries to read all lines from a file
	 * @param path
	 * @return
	 * @throws IOException
	 */
	protected ArrayList<String> readScript(String path) {
		File f = new File(this.getPluginScriptFolder() + File.separator + path);
		if(f.exists() && f.canRead()) {
			ArrayList<String> l = new ArrayList<>();
			try { 
				l.addAll(Files.readAllLines(f.toPath()));
				return l;
			} catch(Exception e) { e.printStackTrace(); }
		}
		return null;
	}
	
	/**
	 * ready to add list of the before script commands
	 * @return
	 */
	public ArrayList<String> getBeforeScriptCommands() {
		ArrayList<String> l = this.addVariables(this.getAdditionalVariables(), this.readScript(this.getBeforeScriptPath()));
		return l;
	}

	/**
	 * ready to add list of the after script commands
	 * @return
	 */
	public ArrayList<String> getAfterScriptCommands() {
		return this.readScript(this.getAfterScriptPath());
	}
	
	/**
	 * path to the before script template or null if not required
	 * @return
	 */
	public abstract String getBeforeScriptPath();
	
	/**
	 * path to the after script template or null if not required
	 * @return
	 */
	public abstract String getAfterScriptPath();
	
	/**
	 * returns environment variables that are required in the before / after scripts
	 * @return
	 */
	protected abstract HashMap<String, String> getAdditionalVariables();
}
