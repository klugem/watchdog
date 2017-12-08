package de.lmu.ifi.bio.watchdog.task.actions.vfs.impl;

public class UrlFileVFSRegister extends SimpleVFSRegister {

	private static final String CLASS_NAME = "org.apache.commons.vfs2.provider.url.UrlFileProvider";
	
	public UrlFileVFSRegister() throws Exception {
		super(CLASS_NAME, null); // use it as default
	}
}