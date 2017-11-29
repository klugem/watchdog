package de.lmu.ifi.bio.watchdog.GUI.useraction;

import java.io.Serializable;

import de.lmu.ifi.bio.watchdog.GUI.datastructure.ExtendedClipboardContent;
import javafx.scene.input.DragEvent;

/**
 * Data that is transfered if a new dependency is created
 * @author kluge
 *
 */
public class CreateDependencyAction extends OriginBasedUseraction implements Serializable {

	private static final long serialVersionUID = 7775667398679973431L;
	private final boolean IS_START_OUTPUT;

	public CreateDependencyAction(String key, boolean startIsOutput) {
		super(key);
		this.IS_START_OUTPUT = startIsOutput;
	}
	
	public CreateDependencyAction(int x, int y, boolean startIsOutput) {
		super(x, y);
		this.IS_START_OUTPUT = startIsOutput;
	}
	
	public boolean isStartOutput() {
		return this.IS_START_OUTPUT;
	}
	
	public static final CreateDependencyAction getCreateDependencyAction(DragEvent event) {
		Useraction a = ExtendedClipboardContent.getUseraction(event);
		if(a != null && a instanceof CreateDependencyAction)
			return (CreateDependencyAction) a;
		return null;
	}
}
