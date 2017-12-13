package de.lmu.ifi.bio.watchdog.task.actions;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;

import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.task.TaskActionTime;
import de.lmu.ifi.bio.watchdog.task.actions.vfs.WatchdogFileSystemManager;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Deletes files or folders
 * @author kluge
 *
 */
public class DeleteTaskAction extends PatternFolderTaskAction implements Serializable {
	
	private static final long serialVersionUID = -15453908592948855L;
	private final String PATH;
	private final boolean IS_FILE_TYPE;
	
	public DeleteTaskAction(String path, boolean isFileType, TaskActionTime time, boolean uncoupleFromExecutor, String pattern) {
		super(time, uncoupleFromExecutor, pattern);
		
		this.PATH = path;
		this.IS_FILE_TYPE = isFileType;
	}
	
	/**
	 * Constructor for variable replacement
	 * @param path
	 * @param d
	 */
	public DeleteTaskAction(String path, String pattern, DeleteTaskAction d) {
		super(d.getActionTime(), d.isUncoupledFromExecutor(), pattern);
		
		this.PATH = path;
		this.IS_FILE_TYPE = d.IS_FILE_TYPE;
	}
	
	@Override
	protected boolean performAction() {
		// ensure that is only executed once
		if(this.wasExecuted())
			return this.wasSuccessfull();
		
		super.performAction();
		try {
			FileSystemManager fsManager = WatchdogFileSystemManager.getManager(false);
			FileObject f = fsManager.resolveFile(this.PATH);

			// nothing to do, if file or folder is not there
			if(!f.exists()) 
				return true;
	
			// delete file or folder
			if(this.IS_FILE_TYPE && f.isFile()) {
				if(!f.delete()) {
					this.addError("File or folder '"+f.getPublicURIString()+"' could not be deleted.");
					return false;
				} 
				return true;
			}
			else if(!this.IS_FILE_TYPE && f.isFolder()) {
				try {
					if(this.hasPattern())
						f.delete(this.getPatternSelector(f));
					else
						f.deleteAll();
					return true;
				}
				catch(Exception e) {
					this.addError("Failed to delete folder '"+f.getPublicURIString()+"':" + NEWLINE + StringUtils.join(e.getStackTrace(), NEWLINE));
					return false;					
				}
			}
			else {
				if(f.isFile())
					this.addError("Expected folder but got file '"+f.getPublicURIString()+"'!");
				else
					this.addError("Expected file but got folder '"+f.getPublicURIString()+"'!");
				
				return false;
			}
		}
		catch(FileSystemException e) {
			e.printStackTrace();
			this.addError(this.getName() + " failed caused by a FileSystemException of org.apache.commons.vfs2. See log file for stackTrace.");
		}
		return false;
	}

	public String getPath() {
		return this.PATH;
	}
	
	@Override
	public String toXML() {
		XMLBuilder x = new XMLBuilder();
		x.startTag(this.IS_FILE_TYPE ? XMLParser.DELETE_FILE : XMLParser.DELETE_FOLDER, false);
		x.addQuotedAttribute(this.IS_FILE_TYPE ? XMLParser.FILE : XMLParser.FOLDER, this.PATH);
		if(!this.IS_FILE_TYPE && this.hasPattern()) x.addQuotedAttribute(XMLParser.PATTERN, this.getPattern());
		
		// close and return the XML tag
		x.endCurrentTag(true);
		return x.toString();
	}

	@Override
	public String getName() {
		return super.getName() + (this.IS_FILE_TYPE ? " file" : " folder");
	}
	
	public boolean isFileType() {
		return this.IS_FILE_TYPE;
	}

	@Override
	public void setColor(String c) {}

	@Override
	public String getColor() { return null; }

	@Override
	public void onDeleteProperty() {}

	@Override
	public String getTarget() {
		return this.getPath();
	}
	
	@Override
	public Object[] getDataToLoadOnGUI() { return null; }
}
