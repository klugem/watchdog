package de.lmu.ifi.bio.watchdog.task.actions.vfs.impl;

public class HTTPVFSRegister extends SimpleVFSRegister {

	private static final String CLASS_NAME = "org.apache.commons.vfs2.provider.http.HttpFileProvider";
	private static final String[] SCHEME = new String[]{"http"};
	
	public HTTPVFSRegister() throws Exception {
		super(CLASS_NAME, SCHEME);
	}
}