package de.lmu.ifi.bio.watchdog.GUI.layout;

import javafx.event.EventHandler;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

/**
 * Factory that can be used to make some cells of a TreeViewTable drag-able
 * @author kluge
 *
 * @param <S>
 * @param <T>
 */
public class DragableTableCellFactory<S, T> implements Callback<TreeTableColumn<S, T>, TreeTableCell<S, T>> {
	
	private final EventHandler<MouseEvent> ON_DRAG_DETECTED;
	private final EventHandler<DragEvent> ON_DRAG_DONE;

	public DragableTableCellFactory(EventHandler<MouseEvent> onDragDetected, EventHandler<DragEvent> onDragDone) {
		this.ON_DRAG_DETECTED = onDragDetected;
		this.ON_DRAG_DONE = onDragDone; 
	}

	@Override
	public TreeTableCell<S, T> call(TreeTableColumn<S, T> param) {
		return new DragableTreeTableCell<S, T>(this.ON_DRAG_DETECTED, this.ON_DRAG_DONE);
    }
}
