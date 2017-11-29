package de.lmu.ifi.bio.watchdog.task.actions;

import java.io.File;
import java.io.Serializable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.task.TaskAction;
import de.lmu.ifi.bio.watchdog.task.TaskActionTime;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Copies files or folders
 * @author kluge
 *
 */
public class CopyTaskAction extends TaskAction implements Serializable {

	private static final long serialVersionUID = 4663622213901702886L;
	private final String SRC;
	private final String DEST;
	private final boolean OVERRIDE;
	private final boolean CREATE_PARENT;
	private final boolean DELETE_SOURCE;
	private final boolean IS_FILE_TYPE;
	
	public CopyTaskAction(String src, String dest, boolean override, boolean createParent, boolean deleteSource, boolean isFileType, TaskActionTime time, boolean uncoupleFromExecutor) {
		super(time, uncoupleFromExecutor);
		
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
	public CopyTaskAction(String src, String dest, CopyTaskAction c) {
		super(c.getActionTime(), c.isUncoupledFromExecutor());
		
		this.SRC = src;
		this.DEST = dest;
		this.OVERRIDE = c.OVERRIDE;
		this.CREATE_PARENT = c.CREATE_PARENT;
		this.DELETE_SOURCE = c.DELETE_SOURCE;
		this.IS_FILE_TYPE = c.IS_FILE_TYPE;
	}

	@Override
	protected boolean performAction() {
		File s = new File(this.SRC);
		File d = new File(this.DEST);
		File p = d.getParentFile();
		
		// check, if file or folder exists
		if(!s.exists()) {
			this.addError("Source file or folder '"+s.getAbsolutePath()+"' does not exist.");
			return false;
		}
		
		// check, if parent folder exists
		if(!p.exists() && !this.CREATE_PARENT) { 
			this.addError("Parent folder '"+p.getAbsolutePath()+"' of destination does not exist and should not be created.");
			return false;
		}
		// create the parent folder
		else if(!p.exists() && this.CREATE_PARENT) {
			if(!p.mkdirs()) {
				this.addError("Failed to create parent folder of destination '"+p.getAbsolutePath()+"'!");
				return false;
			}
		}
		
		// check, if file is already there
		if(d.exists()) {
			if(!this.OVERRIDE) {
				this.addError("Destination file or folder '"+d.getAbsolutePath()+"' does already exist.");
				return false;
			}
		}
		// create a new empty file for file copy
		else if(this.IS_FILE_TYPE) {
			try { d.createNewFile(); }
			catch(Exception e) { this.addError("Failed to copy file '"+s.getAbsolutePath()+"' to '"+d.getAbsolutePath()+"':" + NEWLINE + StringUtils.join(e.getStackTrace(), NEWLINE)); }
		}
		
		// copy files or folders
		boolean retCopy = false;
		if(this.IS_FILE_TYPE && s.isFile()) {
			try {
				FileUtils.copyFile(s, d);
				retCopy = true;
			}
			catch(Exception e) { this.addError("Failed to copy file '"+s.getAbsolutePath()+"' to '"+d.getAbsolutePath()+"':" + NEWLINE + StringUtils.join(e.getStackTrace(), NEWLINE)); }				
		}
		else if(!this.IS_FILE_TYPE && s.isDirectory()) {
			try {
				FileUtils.copyDirectory(s, d);
				retCopy = true;
			}
			catch(Exception e) { this.addError("Failed to copy folder '"+s.getAbsolutePath()+"' to '"+d.getAbsolutePath()+"':" + NEWLINE + StringUtils.join(e.getStackTrace(), NEWLINE)); }	
		}
		else {
			if(s.isFile())
				this.addError("Expected folder but got file '"+s.getAbsolutePath()+"'!");
			else
				this.addError("Expected file but got folder '"+s.getAbsolutePath()+"'!");
		}
		
		// delete the source files
		if(this.DELETE_SOURCE && retCopy) {
			DeleteTaskAction da = new DeleteTaskAction(this.SRC, this.IS_FILE_TYPE, this.getActionTime(), this.isUncoupledFromExecutor());
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
}
