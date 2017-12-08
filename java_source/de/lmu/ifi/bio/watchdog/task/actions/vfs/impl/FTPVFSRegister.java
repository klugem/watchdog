package de.lmu.ifi.bio.watchdog.task.actions.vfs.impl;

public class FTPVFSRegister extends SimpleVFSRegister {

	private static final String CLASS_NAME = "org.apache.commons.vfs2.provider.ftp.FtpFileProvider";
	private static final String[] SCHEME = new String[]{"ftp"};
	
	public FTPVFSRegister() throws Exception {
		super(CLASS_NAME, SCHEME);
	}
}
