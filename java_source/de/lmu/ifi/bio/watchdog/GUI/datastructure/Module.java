package de.lmu.ifi.bio.watchdog.GUI.datastructure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;

import de.lmu.ifi.bio.watchdog.GUI.interfaces.ListLibraryView;
import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import de.lmu.ifi.bio.watchdog.helper.Parameter;
import de.lmu.ifi.bio.watchdog.helper.returnType.ReturnType;

/**
 * Class that holds data about modules
 * @author kluge
 *
 */
public class Module implements ListLibraryView, Serializable {

	private static final long serialVersionUID = 2491749477334229919L;
	protected final String NAME;
	protected final String INSTALL_DIR;
	protected final String IMAGE;
	protected final String CAT;
	protected final LinkedHashMap<String, Parameter> PARAMETER;
	protected final HashMap<String, ReturnType> RETURN_PARAMS = new HashMap<>();
	
	public Module(String name, String installDir, String image, HashMap<String, Parameter> parameter, HashMap<String, ReturnType> retParams, String cat) {
		this.NAME = name;
		this.INSTALL_DIR = installDir;
		this.IMAGE = ImageLoader.getURL(ImageLoader.RED).toString(); 
		this.CAT = cat;
		
		LinkedHashMap<String, Parameter> sortedList = new LinkedHashMap<>();
		// sort parameters
		if(parameter != null) {
			ArrayList<String> paramNames = new ArrayList<>(parameter.keySet());
			Collections.sort(paramNames);
			
			for(String n : paramNames)
				sortedList.put(n, parameter.get(n));
		}
		this.PARAMETER = sortedList;
		if(retParams != null)
			this.RETURN_PARAMS.putAll(retParams);
	}

	public String getName() {
		return this.NAME;
	}

	public String getInstallDir() {
		return this.INSTALL_DIR;
	}

	public String getImage() {
		return this.IMAGE;
	}
	
	public String getCategory() {
		return this.CAT;
	}
	
	public boolean hasImage() {
		return this.IMAGE != null;
	}
	
	public LinkedHashMap<String, Parameter> getParameter() {
		return this.PARAMETER;
	}
	
	public HashMap<String, ReturnType> getReturnParams() {
		return this.RETURN_PARAMS;
	}

	public static Module getRootModule() {
		return new RootModule();
	}
	
	private static class RootModule extends Module {
		private static final long serialVersionUID = -2312650814682405950L;

		public RootModule() {
			super("", "", "", null, null, "");
		}
	}
}
