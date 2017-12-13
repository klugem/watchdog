package de.lmu.ifi.bio.watchdog.task.actions;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;

import de.lmu.ifi.bio.watchdog.helper.PatternFilenameFilter;

public class PatternFileSelector implements FileSelector {
	
	private final boolean ALL_LEVELS;
	private final String PATTERN;
	private final PatternFilenameFilter P;
	private final FileObject BASEDIR;
	
	public PatternFileSelector(FileObject basedir, String pattern, boolean allLevels) {
		this.PATTERN = pattern;
		this.ALL_LEVELS = allLevels;
		this.BASEDIR = basedir;
		this.P = new PatternFilenameFilter(this.PATTERN, false);
	}
	
	private boolean isBaseDir(FileSelectInfo fileInfo) {
		return fileInfo != null && this.BASEDIR.equals(fileInfo.getFile());
	}

	@Override
	public boolean includeFile(FileSelectInfo fileInfo) throws Exception {
		boolean ret = this.isBaseDir(fileInfo) || this.P.matchesFilename(fileInfo.getFile().getName().getBaseName());
		//System.out.println("testing '"+fileInfo.getFile().getName().getBaseName()+"' for '"+this.PATTERN+"'..." + ret);
		return ret;
	}

	@Override
	public boolean traverseDescendents(FileSelectInfo fileInfo) throws Exception {
		return this.ALL_LEVELS || this.isBaseDir(fileInfo);
	}
}
