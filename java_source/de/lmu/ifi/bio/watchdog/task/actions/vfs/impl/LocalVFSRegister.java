package de.lmu.ifi.bio.watchdog.task.actions.vfs.impl;

public class LocalVFSRegister extends SimpleVFSRegister {

	private static final String CLASS_NAME = "org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider";
	private static final String[] SCHEME = new String[]{"file"};
	
	public LocalVFSRegister() throws Exception {
		super(CLASS_NAME, SCHEME);
	}
}