package de.lmu.ifi.bio.watchdog.helper.returnType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

public abstract class ReturnType implements Serializable {

	private static final long serialVersionUID = -2625661511287719315L;
	private final String TYPE;
	public int minVer = 0;
	public int maxVer = 0;
	
	private static final HashSet<String> FILE_BASE_TYPE = new HashSet<>();
	private static final HashMap<Class<?>, ReturnType> CLASS_2_RETURN = new HashMap<>();
	
	static {
		FILE_BASE_TYPE.add(FileReturnType.ABSOLUTE_FOLDER);
		FILE_BASE_TYPE.add(FileReturnType.ABSOLUTE_FILE);
		FILE_BASE_TYPE.add(FileReturnType.RELATIVE_FOLDER);
		FILE_BASE_TYPE.add(FileReturnType.RELATIVE_FILE);
		FILE_BASE_TYPE.add(FileReturnType.FILENAME);
		
		CLASS_2_RETURN.put(String.class, StringReturnType.TYPE);
		CLASS_2_RETURN.put(Integer.class, IntegerReturnType.TYPE);
		CLASS_2_RETURN.put(Double.class, DoubleReturnType.TYPE);
		CLASS_2_RETURN.put(Boolean.class, BooleanReturnType.TYPE);
	}
	
	public static boolean isFileBaseType(String type) { return FILE_BASE_TYPE.contains(type); };
	
	public static ReturnType getRetunType(Class<?> c) {
		return CLASS_2_RETURN.get(c);
	}
	
	/**
	 * Constructor
	 * @param type
	 */
	public ReturnType(String type) {
		this.TYPE = type;
	}
	
	/**
	 * returns the type of the return 
	 * @return
	 */
	public String getType() {
		return this.TYPE;
	}
	
	@Override
	public String toString() {
		return this.getType();
	}
	
	/**
	 * 
	 * @param check
	 * @return
	 */
	public abstract boolean checkType(String check);
	
	/**
	 * used in template docu extractor
	 * @param minVersion
	 * @param maxVersion
	 */
	public void setVersion(int minVersion, int maxVersion) {
		this.minVer = minVersion;
		this.maxVer = maxVersion;
	}
}
