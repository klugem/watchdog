package de.lmu.ifi.bio.watchdog.task.actions.vfs.impl;

public class HTTPSVFSRegister extends SimpleVFSRegister {

	private static final String CLASS_NAME = "org.apache.commons.vfs2.provider.https.HttpsFileProvider";
	private static final String[] SCHEME = new String[]{"https"};
	
	public HTTPSVFSRegister() throws Exception {
		super(CLASS_NAME, SCHEME);
	}
}
