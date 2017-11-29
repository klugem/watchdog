package de.lmu.ifi.bio.watchdog.task;

import java.io.Serializable;


/**
 * modes for when the TaskActions should be performed
 * @author kluge
 *
 */
public enum TaskActionTime implements Serializable {
	
	BEFORE("beforeTask"), AFTER("afterTask"), SUCCESS("onSuccess"), FAIL("onFailure"), TERMINATE("beforeTerminate");
	private final String TYPE;
	private static final long serialVersionUID = 1557518524537639261L;
	
	private TaskActionTime(String xsdType) {
		TYPE = xsdType;
	}
	
	public String getType() {
		return this.TYPE;
	}
	
	@Override
	public String toString() {
		return this.TYPE.replaceAll("([A-Z])", " $1").toLowerCase();
	}
	
	/**
	 * getter for task action times
	 * @param time
	 * @return
	 */
	public static TaskActionTime getTaskActionTime(String time) {
		if(BEFORE.TYPE.equals(time))
			return BEFORE;
		else if(AFTER.TYPE.equals(time))
			return AFTER;
		else if(SUCCESS.TYPE.equals(time))
			return SUCCESS;
		else if(FAIL.TYPE.equals(time))
			return FAIL;
		else if(TERMINATE.TYPE.equals(time))
			return TERMINATE;
		
		try { throw new IllegalArgumentException("String '"+time+"' is no valid TaskActionTime!"); }
		catch(IllegalArgumentException e) { e.printStackTrace(); }
		System.exit(1);
		return null;
	}
	
	/* checker */
	public boolean isBefore() {
		return BEFORE.TYPE.equals(this.TYPE);
	}
	public boolean isAfter() {
		return AFTER.TYPE.equals(this.TYPE);
	}
	public boolean isOnSuccess() {
		return SUCCESS.TYPE.equals(this.TYPE);
	}
	public boolean isFailure() {
		return FAIL.TYPE.equals(this.TYPE);
	}
	public boolean isBeforeTerminate() {
		return TERMINATE.TYPE.equals(this.TYPE);
	}
}
