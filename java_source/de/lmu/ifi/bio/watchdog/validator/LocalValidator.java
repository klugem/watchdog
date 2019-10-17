package de.lmu.ifi.bio.watchdog.validator;

/**
 * All tests that implement that validator can be tested locally
 * @author kluge
 *
 */
public interface LocalValidator {
		
	/**
	 * returns the folder that should be validated
	 * @return
	 */
	public String getFolderToValidate();
	
	/**
	 * sets the folder that should be validated
	 * @param absDir
	 */
	public void setFolderToValidate(String absDir);
	
	/**
	 * true, if a local test is run
	 * @return
	 */
	public default boolean isLocalTest() {
		return !this.canNOTBeUsedLocally() && this.getFolderToValidate() != null;
	}
	
	/**
	 * can deny that test can be run locally
	 */
	public default boolean canNOTBeUsedLocally() { return false; }
}