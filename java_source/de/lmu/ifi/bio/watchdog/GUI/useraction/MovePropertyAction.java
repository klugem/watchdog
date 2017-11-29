package de.lmu.ifi.bio.watchdog.GUI.useraction;

import java.io.Serializable;

import de.lmu.ifi.bio.watchdog.GUI.datastructure.ExtendedClipboardContent;
import de.lmu.ifi.bio.watchdog.GUI.properties.PropertyData;
import de.lmu.ifi.bio.watchdog.GUI.properties.PropertyLine;
import javafx.scene.input.DragEvent;

/**
 * Transfers a property
 * @author kluge
 *
 */
public class MovePropertyAction extends Useraction implements Serializable {
	
	private static final long serialVersionUID = 3662074186672668501L;
	private final int PROPERTY_DATA_ID;
	private final String PARENT_PROPERTY_LINE_ID;
	private final boolean IS_LOCATED_IN_PROPERTY_TOOLBAR; 
	
	public MovePropertyAction(int propertyID, boolean isLocatedInPropertyToolbar, PropertyLine parent) {
		this.PROPERTY_DATA_ID = propertyID;
		this.PARENT_PROPERTY_LINE_ID = parent.getId();
		this.IS_LOCATED_IN_PROPERTY_TOOLBAR = isLocatedInPropertyToolbar;
	}
	
	public PropertyData getPropertyData() {
		return PropertyData.getPropertyData(this.PROPERTY_DATA_ID);
	}
	
	public static final MovePropertyAction getMovePropertyAction(DragEvent event) {
		Useraction a = ExtendedClipboardContent.getUseraction(event);
		if(a != null && a instanceof MovePropertyAction)
			return (MovePropertyAction) a;
		return null;
	}

	public boolean isLocatedInPropertyToolbar() {
		return this.IS_LOCATED_IN_PROPERTY_TOOLBAR;
	}

	public PropertyLine getParentLine() {
		return PropertyLine.getPropertyLine(this.PARENT_PROPERTY_LINE_ID);
	}
}
