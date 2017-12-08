package de.lmu.ifi.bio.watchdog.task.actions.vfs.impl;

public class RAMVFSRegister extends SimpleVFSRegister {

	private static final String CLASS_NAME = "org.apache.commons.vfs2.provider.ram.RamFileProvider";
	private static final String[] SCHEME = new String[]{"ram"};
	
	public RAMVFSRegister() throws Exception {
		super(CLASS_NAME, SCHEME);
	}
}
