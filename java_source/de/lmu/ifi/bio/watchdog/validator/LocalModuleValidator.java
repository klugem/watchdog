package de.lmu.ifi.bio.watchdog.validator;

/**
 * All tests that implement that validator can be tested locally
 * @author kluge
 *
 */
public interface LocalModuleValidator {
		
	/**
	 * returns the module folder that should be validated
	 * @return
	 */
	public String getModuleFolderToValidate();
	
	/**
	 * sets the module folder that should be validated
	 * @param absDir
	 */
	public void setModuleFolderToValidate(String absDir);
	
	/**
	 * true, if a local test is run
	 * @return
	 */
	public default boolean isLocalTest() {
		return !this.canNOTBeUsedLocally() && this.getModuleFolderToValidate() != null;
	}
	
	/**
	 * can deny that test can be run locally
	 */
	public default boolean canNOTBeUsedLocally() { return false; }
}