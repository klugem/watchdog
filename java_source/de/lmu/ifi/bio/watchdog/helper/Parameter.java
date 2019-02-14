package de.lmu.ifi.bio.watchdog.helper;

import java.io.Serializable;

import de.lmu.ifi.bio.watchdog.helper.returnType.ReturnType;

public class Parameter implements Serializable {

	private static final long serialVersionUID = -5111660871955667788L;
	private final ReturnType TYPE;
	private final String NAME;
	private final Integer MIN_O;
	private final Integer MAX_O;
	public int minVer = 0;
	public int maxVer = 0;
	
	public Parameter(String name, Integer minO, Integer maxO, ReturnType r) {
		this.NAME = name;
		this.TYPE = r;
		this.MIN_O = minO;
		this.MAX_O = maxO;
	}
	
	public String getName() {
		return this.NAME;
	}
	public ReturnType getType() {
		return this.TYPE;
	}
	public Integer getMin() {
		return this.MIN_O;
	}
	public Integer getMax() {
		return this.MAX_O;
	}
	
	public boolean isOptional() {
		return this.MIN_O == 0;
	}
	public boolean isUnbounded() {
		return this.MAX_O == null;
	}
	public boolean isNumberCountValid(int count) {
		boolean minOK = this.MIN_O <= count;
		boolean maxOK = this.isUnbounded() || count <= this.MAX_O;
		return minOK && maxOK;
	}
	
	public boolean isOnlySingleInstanceAllowed() {
		return (this.getMax() != null && this.getMax() == 1);
	}

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
