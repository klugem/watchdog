package de.lmu.ifi.bio.watchdog.GUI.datastructure;

import de.lmu.ifi.bio.watchdog.GUI.useraction.Useraction;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;

/**
 * Class that can be used to transfer custom objects in clipboard
 * @author kluge
 *
 */
public class ExtendedClipboardContent extends ClipboardContent {

	private static final long serialVersionUID = -4094869178300420223L;
	public static final DataFormat USERACTION_FORMAT = ExtendedDataFormat.USERACTION;

	public final boolean putUserAction(Useraction a) {
        if (a== null) {
            this.remove(USERACTION_FORMAT);
        } else {
            this.put(USERACTION_FORMAT, a);
        }
        return true;
    }
	
	/**
	 * Returns the useraction, if one is set
	 * @param event
	 * @return
	 */
	public static final Useraction getUseraction(DragEvent event) {
		if(event.getDragboard().hasContent(USERACTION_FORMAT)) {
			return (Useraction) event.getDragboard().getContent(ExtendedClipboardContent.USERACTION_FORMAT);
		}
		return null;
	}

	private static class ExtendedDataFormat extends DataFormat {
		private static final String USERACTION_TXT = Useraction.class.getName();
		private static final DataFormat USERACTION = getUseractionDataFormat();
		
		public static DataFormat getUseractionDataFormat() {
			return new DataFormat(USERACTION_TXT);
		}
	}
}
