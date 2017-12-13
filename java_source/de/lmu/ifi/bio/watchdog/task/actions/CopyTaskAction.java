package de.lmu.ifi.bio.watchdog.task.actions;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.Selectors;

import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.task.TaskActionTime;
import de.lmu.ifi.bio.watchdog.task.actions.vfs.WatchdogFileSystemManager;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Copies files or folders
 * @author kluge
 *
 */
public class CopyTaskAction extends PatternFolderTaskAction implements Serializable {

	private static final long serialVersionUID = 4663622213901702886L;
	private final String SRC;
	private final String DEST;
	private final boolean OVERRIDE;
	private final boolean CREATE_PARENT;
	private final boolean DELETE_SOURCE;
	private final boolean IS_FILE_TYPE;
	
	public CopyTaskAction(String src, String dest, boolean override, boolean createParent, boolean deleteSource, boolean isFileType, TaskActionTime time, boolean uncoupleFromExecutor, String pattern) {
		super(time, uncoupleFromExecutor, pattern);
		
		this.SRC = src;
		this.DEST = dest;
		this.OVERRIDE = override;
		this.CREATE_PARENT = createParent;
		this.DELETE_SOURCE = deleteSource;
		this.IS_FILE_TYPE = isFileType;
	}

	/**
	 * Constructor for variable replacement
	 * @param src
	 * @param dest
	 * @param c
	 */
	public CopyTaskAction(String src, String dest, String pattern, CopyTaskAction c) {
		super(c.getActionTime(), c.isUncoupledFromExecutor(), pattern);
		
		this.SRC = src;
		this.DEST = dest;
		this.OVERRIDE = c.OVERRIDE;
		this.CREATE_PARENT = c.CREATE_PARENT;
		this.DELETE_SOURCE = c.DELETE_SOURCE;
		this.IS_FILE_TYPE = c.IS_FILE_TYPE;
	}

	@Override
	protected boolean performAction() {
		// ensure that is only executed once
		if(this.wasExecuted())
			return this.wasSuccessfull();
		
		super.performAction();
		try {
			FileSystemManager fsManager = WatchdogFileSystemManager.getManager(false);
			FileObject s = fsManager.resolveFile(this.SRC);
			FileObject d = fsManager.resolveFile(this.DEST);
			FileObject p = d.getParent();
			
			// check, if file is already there --> and remove it
			if(d.exists()) {
				if(!this.OVERRIDE) {
					this.addError("Destination file or folder '"+d.getPublicURIString()+"' does already exist.");
					return false;
				}
				else {
					d.delete(Selectors.SELECT_SELF_AND_CHILDREN);
				}
			}
			
			// check, if file or folder exists
			if(!s.exists()) {
				this.addError("Source file or folder '"+s.getPublicURIString()+"' does not exist.");
				return false;
			}
			
			// check, if parent folder exists
			if(!p.exists() && !this.CREATE_PARENT) { 
				this.addError("Parent folder '"+p.getPublicURIString()+"' of destination does not exist and should not be created.");
				return false;
			}
			// create the parent folder
			else if(!p.exists() && this.CREATE_PARENT) {
				try{ p.createFolder(); }
				catch(Exception e) {
					this.addError("Failed to create parent folder of destination '"+p.getPublicURIString()+"'!");
					return false;
				}
			}
			
			// copy files or folders
			boolean retCopy = false;
			if(this.IS_FILE_TYPE && s.isFile()) {
				try {
					d.copyFrom(s, Selectors.SELECT_SELF);
					retCopy = true;
				}
				catch(Exception e) { this.addError("Failed to copy file '"+s.getPublicURIString()+"' to '"+d.getPublicURIString()+"':" + NEWLINE + StringUtils.join(e.getStackTrace(), NEWLINE)); }				
			}
			else if(!this.IS_FILE_TYPE && s.isFolder()) {
				try {
					System.out.println("copy to " + d.getPublicURIString());
					d.copyFrom(s, this.getPatternSelector(s));
					retCopy = true;
				}
				catch(Exception e) { this.addError("Failed to copy folder '"+s.getPublicURIString()+"' to '"+d.getPublicURIString()+"':" + NEWLINE + StringUtils.join(e.getStackTrace(), NEWLINE)); }	
			}
			else {
				if(s.isFile())
					this.addError("Expected folder but got file '"+s.getPublicURIString()+"'!");
				else
					this.addError("Expected file but got folder '"+s.getPublicURIString()+"'!");
			}
			
			// delete the source files
			if(this.DELETE_SOURCE && retCopy) {
				DeleteTaskAction da = new DeleteTaskAction(this.SRC, this.IS_FILE_TYPE, this.getActionTime(), this.isUncoupledFromExecutor(), null);
				boolean retDel = da.performAction();
				if(!retDel) {
					// copy error messages
					for(String e : da.getErrors()) {
						this.addError(e.split(NEWLINE)[2]);
					}
				}
				return retDel;
			}
			else {
				return retCopy;
			}
		}
		catch(FileSystemException e) {
			e.printStackTrace();
			this.addError(this.getName() + " failed caused by a FileSystemException of org.apache.commons.vfs2. See log file for stackTrace.");
		}
		return false;
	}

	public String getSrc() {
		return this.SRC;
	}
	
	public String getDest() {
		return this.DEST;
	}

	@Override
	public String toXML() {
		XMLBuilder x = new XMLBuilder();
		x.startTag(this.IS_FILE_TYPE ? XMLParser.COPY_FILE : XMLParser.COPY_FOLDER, false);
		x.addQuotedAttribute(this.IS_FILE_TYPE ? XMLParser.FILE : XMLParser.FOLDER, this.SRC);
		x.addQuotedAttribute(XMLParser.DESTINATION, this.DEST);
		
		// set optional values if they are not the defaults
		if(this.DELETE_SOURCE) x.addQuotedAttribute(XMLParser.DELETE_SOURCE, this.DELETE_SOURCE);
		if(this.OVERRIDE) x.addQuotedAttribute(XMLParser.OVERRIDE, this.OVERRIDE);
		if(!this.CREATE_PARENT) x.addQuotedAttribute(XMLParser.CREATE_FILE, this.CREATE_PARENT);
		if(!this.IS_FILE_TYPE && this.hasPattern()) x.addQuotedAttribute(XMLParser.PATTERN, this.getPattern());
		
		// close and return the XML tag
		x.endCurrentTag(true);
		return x.toString();
	}
	
	public boolean isFileType() {
		return this.IS_FILE_TYPE;
	}
	public boolean isOverride() {
		return this.OVERRIDE;
	}
	public boolean isCreateParent() {
		return this.CREATE_PARENT;
	}
	public boolean isDeleteSource() {
		return this.DELETE_SOURCE;
	}

	@Override
	public String getName() {
		return super.getName() + (this.IS_FILE_TYPE ? " file" : " folder");
	}

	@Override
	public void setColor(String c) {}

	@Override
	public String getColor() { return null; }

	@Override
	public void onDeleteProperty() {}
	
	@Override
	public String getTarget() {
		return this.getSrc();
	}

	@Override
	public Object[] getDataToLoadOnGUI() { return null; }
}
