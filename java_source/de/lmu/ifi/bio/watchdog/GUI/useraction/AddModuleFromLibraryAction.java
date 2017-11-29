package de.lmu.ifi.bio.watchdog.GUI.useraction;

import java.io.Serializable;

import de.lmu.ifi.bio.watchdog.GUI.datastructure.ExtendedClipboardContent;
import de.lmu.ifi.bio.watchdog.GUI.datastructure.Module;
import javafx.scene.input.DragEvent;

/**
 * Transfers data when a module is copied from the toolbar
 * @author kluge
 *
 */
public class AddModuleFromLibraryAction extends Useraction implements Serializable {

	private static final long serialVersionUID = 6092544237966805146L;
	private final Module MODULE;
	
	public AddModuleFromLibraryAction(Module m) {
		this.MODULE = m;
	}
	
	public Module getModule() {
		return this.MODULE;
	}
	
	public static final AddModuleFromLibraryAction getAddModuleFromLibraryAction(DragEvent event) {
		Useraction a = ExtendedClipboardContent.getUseraction(event);
		if(a != null && a instanceof AddModuleFromLibraryAction)
			return (AddModuleFromLibraryAction) a;
		return null;
	}
}
