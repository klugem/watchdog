package de.lmu.ifi.bio.watchdog.GUI.layout;
import de.lmu.ifi.bio.watchdog.GUI.interfaces.ListLibraryView;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;

public class DragableTreeTableCell<A, B> extends TreeTableCell<A, B> {
	
	public DragableTreeTableCell(EventHandler<MouseEvent> onDragDetected, EventHandler<DragEvent> onDragDone) {
		if(onDragDetected != null)
			this.setOnDragDetected(onDragDetected);
		if(onDragDone != null)
			this.setOnDragDone(onDragDone);
	}
	
    @Override
    protected void updateItem(B item, boolean empty) {
        super.updateItem(item, empty);
        String text = null;

        if(item != null && item instanceof ListLibraryView) {
            text = ((ListLibraryView) item).getNameForDisplay();
        }

        // set the content of the cell
        if (item == null) {
            super.setText(null);
            super.setGraphic(null);
        } else if (item instanceof Node) {
            super.setText(null);
            super.setGraphic((Node) item);
        } else {
            super.setText(text);
            super.setGraphic(null);
        }
    }
}