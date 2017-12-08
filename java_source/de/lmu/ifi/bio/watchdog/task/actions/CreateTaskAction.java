package de.lmu.ifi.bio.watchdog.task.actions;

import java.io.Serializable;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;

import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.task.TaskAction;
import de.lmu.ifi.bio.watchdog.task.TaskActionTime;
import de.lmu.ifi.bio.watchdog.task.actions.vfs.WatchdogFileSystemManager;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Creates files or folders
 * @author kluge 
 *
 */
public class CreateTaskAction extends TaskAction implements Serializable {
	
	private static final long serialVersionUID = -95383908598948825L;
	private final String PATH;
	private final boolean OVERRIDE; 
	private final boolean CREATE_PARENT;
	private final boolean IS_FILE_TYPE;
	
	public CreateTaskAction(String path, boolean override, boolean createParent, boolean isFileType, TaskActionTime time, boolean uncoupleFromExecutor) {
		super(time, uncoupleFromExecutor);
		
		this.PATH = path;
		this.OVERRIDE = override;
		this.CREATE_PARENT = createParent;
		this.IS_FILE_TYPE = isFileType;
	}
	
	/**
	 * Constructor for variable replacement
	 * @param path
	 * @param c
	 */
	public CreateTaskAction(String path, CreateTaskAction c) {
		super(c.getActionTime(), c.isUncoupledFromExecutor());
		
		this.PATH = c.PATH;
		this.OVERRIDE = c.OVERRIDE;
		this.CREATE_PARENT = c.CREATE_PARENT;
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
			FileObject f = fsManager.resolveFile(this.PATH);
			FileObject p = f.getParent();
			
			// check, if parent folder exists
			if(!p.exists() && !this.CREATE_PARENT) { 
				this.addError("Parent folder '"+p.getPublicURIString()+"' does not exist and should not be created.");
				return false;
			}
			// check, if file is already there
			if(f.exists()) {
				if(!this.OVERRIDE) {
					this.addError("File or folder '"+f.getPublicURIString()+"' does already exist.");
					return false;
				}
				else 
					f.delete();
			}
			
			// create the parent folder
			if(!p.exists() && this.CREATE_PARENT) {
				try { p.createFolder(); }
				catch(Exception e) {
					this.addError("Failed to create parent folder '"+p.getPublicURIString()+"'!");
					return false;
				}
			}
			// create the file or folder
			if(this.IS_FILE_TYPE) {
				try { f.createFile(); }
				catch(Exception e) {
					this.addError("Failed to create file '"+f.getPublicURIString()+"'!");
					return false;
				}
			}
			else {
				if(f.isFile() && f.exists()) {
					this.addError("Failed to create folder '"+f.getPublicURIString()+"' because a file with that name already exists.");
					return false;
				}
				else if(!f.exists()) {
					try { f.createFolder(); }
					catch(Exception e) {
						this.addError("Failed to create folder '"+p.getPublicURIString()+"'!");
						return false;
					}
				}
			}
			return true;
		}
		catch(FileSystemException e) {
			e.printStackTrace();
			this.addError(this.getName() + " failed caused by a FileSystemException of org.apache.commons.vfs2. See log file for stackTrace.");
		}
		return false;
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

	public String getPath() {
		return this.PATH;
	}
	
	@Override
	public String toXML() {
		XMLBuilder x = new XMLBuilder();
		x.startTag(this.IS_FILE_TYPE ? XMLParser.CREATE_FILE : XMLParser.CREATE_FOLDER, false);
		x.addQuotedAttribute(this.IS_FILE_TYPE ? XMLParser.FILE : XMLParser.FOLDER, this.getPath());
		
		// set optional values if they are not the defaults
		if(this.OVERRIDE) x.addQuotedAttribute(XMLParser.OVERRIDE, this.OVERRIDE);
		if(!this.CREATE_PARENT) x.addQuotedAttribute(XMLParser.CREATE_FILE, this.CREATE_PARENT);
		
		// close and return the XML tag
		x.endCurrentTag(true);
		return x.toString();
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
		return this.getPath();
	}
	
	@Override
	public Object[] getDataToLoadOnGUI() { return null; }
}
