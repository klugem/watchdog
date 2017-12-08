package de.lmu.ifi.bio.watchdog.task.actions.vfs.impl;

public class SFTPVFSRegister extends SimpleVFSRegister {

	private static final String CLASS_NAME = "org.apache.commons.vfs2.provider.sftp.SftpFileProvider";
	private static final String[] SCHEME = new String[]{"sftp"};
	
	public SFTPVFSRegister() throws Exception {
		super(CLASS_NAME, SCHEME);
	}
}
