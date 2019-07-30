package de.lmu.ifi.bio.watchdog.validator.github;

/**
 * Enum for github PR status updates via the API
 * @author kluge
 *
 */
@Deprecated
public enum TestState {
	ERROR("error", "good that we did it!"), FAILURE("failure", "test was not executed correctly"), PENDING("pending", "waiting for test to finish on Travis CI..."), SUCCESS("success", "awesome!");
	
	private final String NAME;
	private final String DESC;
	
	private TestState(String name, String desc) {
		this.NAME = name;
		this.DESC = desc;
	}
	
	@Override
	public String toString() {
		return this.NAME;
	}

	/**
	 * Description of the test
	 * @return
	 */
	public String getDescription() {
		return this.DESC;
	}
}
