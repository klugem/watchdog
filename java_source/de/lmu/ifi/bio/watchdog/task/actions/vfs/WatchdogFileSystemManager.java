package de.lmu.ifi.bio.watchdog.task.actions.vfs;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.impl.DefaultFileReplicator;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.impl.PrivilegedFileReplicator;

import de.lmu.ifi.bio.watchdog.logger.Logger;

/**
 * 
 * @author kluge
 *
 */
public class WatchdogFileSystemManager extends DefaultFileSystemManager {
	
    private static WatchdogFileSystemManager instance;

    private WatchdogFileSystemManager() {}
	
	public static WatchdogFileSystemManager getManager(boolean noExit) {
		if(instance == null) {
			WatchdogFileSystemManager wfsm = new WatchdogFileSystemManager();
			try { wfsm.init(); }
			catch(Exception e) {
				e.printStackTrace();
				if(!noExit)
					System.exit(1);
			}
			instance = wfsm;
		}
		return instance;
	}
	
    @Override
    public void init() throws FileSystemException {
        // taken from StandardFileSystemManager
        final DefaultFileReplicator replicator = new DefaultFileReplicator();
        setReplicator(new PrivilegedFileReplicator(replicator));
        setTemporaryFileStore(replicator);
        
        // add providers
        VFSLoader.configureVFS(this, new Logger());
        
        super.init();
    }
}
