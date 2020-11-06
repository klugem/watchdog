package de.lmu.ifi.bio.watchdog.resume;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.lmu.ifi.bio.watchdog.logger.Logger;
import de.lmu.ifi.bio.watchdog.task.Task;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLTask;

/**
 * Data store class for resume info
 * @author kluge
 *
 */
public class ResumeInfo {
	public static String ID = "id";
	public static String SUB = "sub";
	public static String RET = "ret";
	public static String HASH = "hash";
	public static String TMP_CONST_NAME = "${TMP}";
	public static String COMMAND = "finalCommand";
	public static String MODULE = "module";
	public static String SOFTWARE_VERSION_INFO = "softwareVersion";
	public static String MODULE_VERSION = "moduleVersion";
	private static final String VERSION_REP = XMLParser.VERSION_SEP + "[0-9]+$"; 
	private static final String MOD_VER_REPLACE = "\\[Module version\\]: [0-9]+" + System.lineSeparator();
	private static final String CALLED_COMMAND_REPLACE = "\\[Called command\\]: .+" + System.lineSeparator();
	
	private final int TASK_ID;
	private final String VERSION_SOFTWARE;
	private final String MOD;
	private final int MOD_VERSION;
	private final String GROUP_FILE_NAME;
	private final HashMap<String, String> RETURN_PARAMS = new HashMap<>();
	private final String ARG_HASH_CODE; 
	private boolean isDirty = false;
	
	public static final Gson GSON = new Gson();

	
	/**
	 * constructor
	 * @param taskID
	 * @param groupFileName
	 * @param hash
	 * @param returnParams
	 * @param moduleVersion
	 * @param returnParams
	 */
	private ResumeInfo(int taskID, String groupFileName, String hash, HashMap<String, String> returnParams, String module, int moduleVersion, String softwareVersion) {
		this.TASK_ID = taskID;
		this.GROUP_FILE_NAME = groupFileName;
		this.ARG_HASH_CODE = hash;
		this.MOD = module;
		this.MOD_VERSION = moduleVersion;
		this.VERSION_SOFTWARE = softwareVersion;
		if(returnParams != null)
			this.RETURN_PARAMS.putAll(returnParams);
	}

	public int getTaskID() {
		return TASK_ID;
	}
	
	public String getModuleName() {
		return MOD;
	}
	
	public int getModuleVersion() {
		return MOD_VERSION;
	}

	public String getGroupFileName() {
		return GROUP_FILE_NAME;
	}

	public HashMap<String, String> getReturnParams() {
		return RETURN_PARAMS;
	}
	
	public boolean hasSoftwareVersion() {
		return this.VERSION_SOFTWARE != null && this.VERSION_SOFTWARE.length() > 0;
	}
	
	public String getSoftwareVersion() {
		return this.VERSION_SOFTWARE;
	}
	
	public static String getArgHash(XMLTask x, String command, LinkedHashMap<String, Pair<Pair<String, String>, String>> detailArgs) {
		// 1) remove return param as it will change for every run (TMP dir)
		// 2) replace TMP working dir back to it's variable as it will change every run!
		String tmpDir = x.getExecutor().getWorkingDir();
		StringBuilder b = new StringBuilder(command);
		b.append(" ");
		detailArgs.forEach((key, p) -> {
			String value = p.getValue();
			if(!key.equals(x.getReturnParamName())) {
				if(value != null && value.contains(tmpDir))
					value = value.replace(tmpDir, TMP_CONST_NAME);
				
				Pair<String, String> format = p.getKey();
				b.append(format.getLeft());
				b.append(key);
				b.append(" ");
				if(value != null) {
					b.append(format.getRight());
					b.append(value);
					b.append(format.getRight());
					b.append(" ");
				}
			}
		});
		b.append(XMLParser.VERSION_SEP);
		b.append(" v.");
		b.append(x.getVersion());
		return Integer.toString(b.toString().hashCode());
	}
	
	/**
	 * checks, if a task is unchanged regarding it's parameters
	 * @return
	 */
	public boolean isResumeInfoValid(XMLTask x, Task t, boolean ignoreParamHash) {
		return !this.isDirty() && (ignoreParamHash || this.ARG_HASH_CODE.equals(getArgHash(x, t.getBinaryCall(), t.getDetailedArguments())));
	}
	
	public static ResumeInfo parseResumeInfo(String jsonString) {
		HashMap<String, String> retMap = GSON.fromJson(jsonString, new TypeToken<HashMap<String, String>>() {}.getType());
		
		// parse return values
		String retJsonString = retMap.get(RET);
		HashMap<String, String> reVal = null;
		if(retJsonString != null)
			reVal = GSON.fromJson(retJsonString, new TypeToken<HashMap<String, String>>() {}.getType());
		
		int tid = Integer.parseInt(retMap.get(ID));
		int modV = Integer.parseInt(retMap.get(MODULE_VERSION));
		String groupFileName = retMap.get(SUB);
		String module = retMap.get(MODULE);
		String softwareVersion = retMap.get(SOFTWARE_VERSION_INFO);
		return new ResumeInfo(tid, groupFileName == null ? "" : groupFileName, retMap.get(HASH), reVal, module, modV, softwareVersion);
	}
	
	public static String getResumeInfo(XMLTask x, Task task) {		
		// get values from task
		int taskID = task.getTaskID();
		String groupFileName = task.getGroupFileName();
		HashMap<String, String> ret = task.getReturnParams();
	
		// build string
		HashMap<String, String> values = new HashMap<>();
		values.put(ID, Integer.toString(taskID));
		values.put(MODULE_VERSION, Integer.toString(x.getVersion()));
		values.put(MODULE, x.getTaskType().replaceFirst(VERSION_REP, ""));
		values.put(HASH, getArgHash(x, task.getBinaryCall(), task.getDetailedArguments()));
		
		if(groupFileName != null && groupFileName.length() > 0)
			values.put(SUB, groupFileName);
		if(ret != null && ret.size() > 0) {
			String retJson = GSON.toJson(ret); 
			values.put(RET, retJson);
		}
		
		// add software version and detailed parameter args
		String sep = " ";
		String command = new StringBuffer(task.getBinaryCall()) + sep + StringUtils.join(task.getArguments(), sep);
		values.put(COMMAND, command);
		
		if(task.hasVersionQueryInfoFile()) {
			try {
				File v = task.getVersionQueryInfoFile();
				String vc = StringUtils.join(Files.readAllLines(Paths.get(v.getAbsolutePath())), System.lineSeparator());
				vc = vc.replaceFirst(MOD_VER_REPLACE, ""); // ignore module version here
				vc = vc.replaceFirst(CALLED_COMMAND_REPLACE, ""); // ignore called command here
				values.put(SOFTWARE_VERSION_INFO, vc);
			}
			catch(IOException ex) {
				Logger l = task.getLogger();
				if(l != null) {
					l.error("Failed to read version query file '"+ task.getVersionQueryInfoFile().getAbsolutePath() +"'.");
				}
				ex.printStackTrace();
			}
		}
		
		return GSON.toJson(values);
	}

	public boolean hasReturnParams() {
		return this.RETURN_PARAMS != null && this.RETURN_PARAMS.size() > 0;
	}
	
	public boolean isDirty() {
		return this.isDirty;
	}
	
	public void setDirty() {
		this.isDirty = true;
	}
}
