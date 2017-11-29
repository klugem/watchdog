package de.lmu.ifi.bio.watchdog.GUI.helper;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;

import org.ini4j.Ini;

import de.lmu.ifi.bio.watchdog.helper.Mailer;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Is used to store some settings
 * @author kluge
 *
 */
public class PreferencesStore {
	
	private static final String SECTION_GENERAL = "GENERAL";
	private static final String SECTION_PATHS = "PATHS";
	private static final String SECTION_APPEARANCE = "APPEARANCE";
	private static final String BASE_DIR = "watchdogBase";
	private static final String MODULE_INCLUDE_FOLDER = "moduleInclude";
	private static final String MODULE_INCLUDE_FOLDER_NAMES = "moduleIncludeNames";
	private static final String FULL_SCREEN_MODE = "fullScreenMode";
	private static final String LAST_SAVE_FILES = "lastSaveFiles";
	private static final String DEFAULT_WDITH_NAME = "width";
	private static final String DISPLAY_GRID = "displayGridByDefault";
	private static final String DEFAULT_HEIGHT_NAME = "height";
	private static final String PORT = "port";
	private static final String MAIL = "notificationMail";
	private static final String SMTP_CONFIG = "smtpConfigPath";
	private static final int MAX_NUMBER_OF_LAST_SAVE_FILES = 5;
	private static final String INI_FILE_NAME = ".watchdogData" + File.separator + "settings.watchdog.ini";
	public static final int DEFAULT_HEIGHT = 900;
	public static final int DEFAULT_WIDTH = 1050;
	public static final String DEFAULT_NAME = "provided by bio.ifi.lmu.de";
	public static final int DEFAULT_PORT = 8080;
	
	// values that are stored in this class
	private static final LinkedList<File> LAST_SAVE_FILES_STORE = new LinkedList<>();
	private static final HashMap<String, String> MODULE_SEARCH_FOLDERS = new HashMap<>();
	private static String watchdogBaseDir = null;
	private static boolean isFullScreenMode = false;
	private static boolean displayGrid = false;
	public static File defaultIniFile;
	public static int width = -1;
	public static int height = -1;
	public static String mail = null;
	private static String smtpConfig = null;
	public static int port = DEFAULT_PORT;
	
	// detect application data folder
	static {
		String appDataDir = null;
		String os = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
		if (os.contains("win"))
			appDataDir = System.getenv("AppData");
		else // linux or mac
		{
			appDataDir = System.getProperty("user.home");
			if (os.contains("mac") || os.contains("darwin"))
				appDataDir += File.separator + "Library" + File.separator + "Application Support";
		}
		defaultIniFile = new File(appDataDir + File.separator + INI_FILE_NAME);
		defaultIniFile.getParentFile().mkdirs();
	}
	
	public static boolean saveSettingsToFile(File file) {
		try {
			Ini ini = new Ini();
			ini.add(SECTION_GENERAL);
			ini.add(SECTION_PATHS);
			ini.add(SECTION_APPEARANCE);
			
			// General section
			Ini.Section general = ini.get(SECTION_GENERAL);
			general.add(PORT, port);
			general.add(MAIL, mail);
			general.add(SMTP_CONFIG, smtpConfig);
			
			// Path section
			Ini.Section paths = ini.get(SECTION_PATHS);
			paths.add(BASE_DIR, watchdogBaseDir);
			for(String key : MODULE_SEARCH_FOLDERS.keySet()) {
				paths.add(MODULE_INCLUDE_FOLDER_NAMES, key);
				paths.add(MODULE_INCLUDE_FOLDER, MODULE_SEARCH_FOLDERS.get(key));
			}
			for(File lastSave : LAST_SAVE_FILES_STORE)
				paths.add(LAST_SAVE_FILES, lastSave.getAbsolutePath());
			
			// Appearance section
			Ini.Section appearance = ini.get(SECTION_APPEARANCE);
			appearance.add(FULL_SCREEN_MODE, isFullScreenMode);
			appearance.add(DEFAULT_WDITH_NAME, getWidth());
			appearance.add(DEFAULT_HEIGHT_NAME, getHeight());
			appearance.add(DISPLAY_GRID, displayGrid);
			
			// store the settings in the file
			ini.store(file);
			return true;
		}
		catch(Exception e) { e.printStackTrace(); }
		return false;
	}

	public static boolean loadSettingsFromFile(File file) {
		if(!(file.exists() && file.canRead()))
			return false;
		try {
			Ini ini = new Ini(new FileReader(file));
			// General section
			Ini.Section general = ini.get(SECTION_GENERAL);
			if(general == null)
				return false;
			port = Integer.parseInt(general.get(PORT));
			mail = general.get(MAIL);
			smtpConfig = general.get(SMTP_CONFIG);
						
			// Path section
			Ini.Section paths = ini.get(SECTION_PATHS);
			if(paths == null)
				return false;
			
			watchdogBaseDir = paths.get(BASE_DIR);
			watchdogBaseDir = watchdogBaseDir.replaceFirst(File.separator + "$", "").replaceAll(File.separator + "{2,}", File.separator);
			
			String m[] = paths.getAll(MODULE_INCLUDE_FOLDER, String[].class);
			String[] keys = paths.getAll(MODULE_INCLUDE_FOLDER_NAMES, String[].class);
			// add the folders
			for(int i = 0; i < keys.length; i++) {
				if(m[i].endsWith(File.separator))
					m[i] = m[i].replaceFirst(File.separator + "$", "");
				m[i] = m[i].replaceAll(File.separator + "{2,}", File.separator);
				MODULE_SEARCH_FOLDERS.put(keys[i], m[i]);
			}
			
			for(String lastSave : paths.getAll(LAST_SAVE_FILES, String[].class)) {
				File f = new File(lastSave);
				// ensure that files are there
				if(f.exists() && !LAST_SAVE_FILES_STORE.contains(f))
					LAST_SAVE_FILES_STORE.add(f);
			}
			
			// Appearance section
			Ini.Section appearance = ini.get(SECTION_APPEARANCE);
			try { isFullScreenMode = Boolean.parseBoolean(appearance.get(FULL_SCREEN_MODE)); } catch(Exception e) {}
			try { displayGrid = Boolean.parseBoolean(appearance.get(DISPLAY_GRID)); } catch(Exception e) {}
			try { width = Integer.parseInt(appearance.get(DEFAULT_WDITH_NAME)); } catch(Exception e) {}
			try { height = Integer.parseInt(appearance.get(DEFAULT_HEIGHT_NAME)); } catch(Exception e) {}
		}
		catch(Exception e) { e.printStackTrace(); }
		return false;
	}
	
	/************** methods to set values *************************/
	public static void setBaseDir(String base) {
		// do not allow base dir to end with a slash as this will case problems.
		if(base != null && base.endsWith(File.separator))
			base = base.replaceFirst(File.separator + "$", "");
		base = base.replaceAll(File.separator + "{2,}", File.separator);
		watchdogBaseDir = base;
		MODULE_SEARCH_FOLDERS.put(DEFAULT_NAME, watchdogBaseDir + File.separator + XMLParser.MODULES + File.separator);
	}
	public static void setModuleDirectories(HashMap<String, String> dirs) {
		MODULE_SEARCH_FOLDERS.clear();
		for(String key : dirs.keySet())
			MODULE_SEARCH_FOLDERS.put(key, dirs.get(key));
	}
	public static void setFullScreenMode(boolean enableFullScreen) {
		isFullScreenMode = enableFullScreen;
	}
	public static void setDisplayGridByDefault(boolean enableByDefault) {
		displayGrid = enableByDefault;
	}
	public static void setWidth(int w) {
		width = w;
	}
	public static void setHeight(int h) {
		height = h;
	}
	public static void setPort(int p) {
		port = p;
	}
	public static void setMail(String m) {
		if(m.length() > 0 && !Mailer.validateMail(m))
			return;
		if(m.length() == 0)
			mail = null;
		else
			mail = m;
	}
	public static void setSMTPConfigPath(String smtp) {
		smtpConfig = smtp;
	}
	public static void addLastSaveFile(File f) {
		if(f == null)
			return;
		f = f.getAbsoluteFile();
		
		// do not store it twice
		while(LAST_SAVE_FILES_STORE.contains(f))
			LAST_SAVE_FILES_STORE.remove(f);
		
		// move it to top
		LAST_SAVE_FILES_STORE.add(0, f);
		
		// delete too many entries
		while(LAST_SAVE_FILES_STORE.size() > MAX_NUMBER_OF_LAST_SAVE_FILES) 
			LAST_SAVE_FILES_STORE.removeLast();
	}

	
	/************** methods to query the set values *************************/
	public static boolean hasWatchdogBaseDir() {
		return getWatchdogBaseDir() != null;
	}
	public static boolean isFullScreenMode() {
		return isFullScreenMode;
	}
	public static boolean isGridDisplayedByDefault() {
		return displayGrid;
	}
	public static String getWatchdogBaseDir() {
		return watchdogBaseDir;
	}
	public static HashMap<String, String> getMouleFolders() {
		return MODULE_SEARCH_FOLDERS;
	}
	public static LinkedList<File> getLastSaveFiles() {
		return LAST_SAVE_FILES_STORE;
	}
	public static int getWidth() {
		return width != -1 ? width : DEFAULT_WIDTH;
	}
	public static int getHeight() {
		return height != -1 ? height : DEFAULT_HEIGHT;
	}
	public static int getPort() {
		return port;
	}
	public static String getMail() {
		return mail;
	}
	public static boolean isMailNotificationEnabled() {
		return getMail() != null;
	}
	public static String getSMTPConfigPath() {
		return smtpConfig;
	}
}	
