package de.lmu.ifi.bio.watchdog.task.actions.vfs.impl;

public class FTPSVFSRegister extends SimpleVFSRegister {

	private static final String CLASS_NAME = "org.apache.commons.vfs2.provider.ftps.FtpsFileProvider";
	private static final String[] SCHEME = new String[]{"ftps"};
	
	public FTPSVFSRegister() throws Exception {
		super(CLASS_NAME, SCHEME);
	}
}
