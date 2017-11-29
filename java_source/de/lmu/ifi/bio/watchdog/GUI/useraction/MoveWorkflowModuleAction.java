package de.lmu.ifi.bio.watchdog.GUI.useraction;

import java.io.Serializable;

import de.lmu.ifi.bio.watchdog.GUI.datastructure.ExtendedClipboardContent;
import javafx.scene.input.DragEvent;

/**
 * Data that is transfered if a module is moved on the grid
 * @author kluge
 *
 */
public class MoveWorkflowModuleAction extends OriginBasedUseraction implements Serializable {
	
	private static final long serialVersionUID = 4426950035150865855L;

	public MoveWorkflowModuleAction(String key) {
		super(key);
	}
	
	public MoveWorkflowModuleAction(int x, int y) {
		super(x, y);
	}

	public static final MoveWorkflowModuleAction getMoveWorkflowModuleAction(DragEvent event) {
		Useraction a = ExtendedClipboardContent.getUseraction(event);
		if(a != null && a instanceof MoveWorkflowModuleAction)
			return (MoveWorkflowModuleAction) a;
		return null;
	}
}
