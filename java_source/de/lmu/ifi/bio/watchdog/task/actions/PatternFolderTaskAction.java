package de.lmu.ifi.bio.watchdog.task.actions;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.Selectors;

import de.lmu.ifi.bio.watchdog.task.TaskAction;
import de.lmu.ifi.bio.watchdog.task.TaskActionTime;

/**
 * can be used for a task action that is applied on a folder that allows to select files of the folder
 * based on a pattern
 * @author kluge
 *
 */
public abstract class PatternFolderTaskAction extends TaskAction {

	private static final long serialVersionUID = 1231482532040177580L;
	private static final String DEFAULT_PATTERN = "*";
	protected final String PATTERN;

	public PatternFolderTaskAction(TaskActionTime time, boolean uncoupleFromExecutor, String pattern) {
		super(time, uncoupleFromExecutor);
		this.PATTERN = (pattern != null && (pattern.length() == 0 || pattern.equals(DEFAULT_PATTERN))) ? null : pattern;
	}
	
	public boolean hasPattern() {
		return this.getPattern() != null;
	}

	public String getPattern() {
		return this.PATTERN;
	}
	
	/**
	 * 
	 * @return
	 */
	protected FileSelector getPatternSelector(FileObject basedir) {
		if(!this.hasPattern())
			return Selectors.SELECT_CHILDREN;
		else {
			return new PatternFileSelector(basedir, this.getPattern(), false);
		}
	}
}
