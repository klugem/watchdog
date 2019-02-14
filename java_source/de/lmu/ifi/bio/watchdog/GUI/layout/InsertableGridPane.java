package de.lmu.ifi.bio.watchdog.GUI.layout;

import java.util.LinkedHashMap;
import java.util.LinkedList;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 * Implements a grid pane in which rows can be inserted.
 * Other functions from base class may not work correctly and need to be implemented here if support is required!
 * @author kluge
 *
 */
public class InsertableGridPane extends GridPane {
	public static final String UNIQUE_ID = "UNIQUE_ID";
	protected final LinkedList<LinkedList<Node>> ITEMS = new LinkedList<>();
	protected final LinkedHashMap<String, Integer> LABELS = new LinkedHashMap<>();
	
	/**
	 * inserts a new row
	 * @param index
	 * @param children
	 */
	public void insertRow(int index, Node... children) {
		// store the data in the internal list as all stuff is private in GridPane...
		LinkedList<Node> l = new LinkedList<>();
		for(Node n : children)
			l.add(n);
		this.ITEMS.add(index, l);

		// item is added at the end
		if(index >= this.ITEMS.size()) {
			this.addRow(index, children);
		}
		// item is in the middle --> we have to re-add all rows
		else
			this.rebuildGrid();
	}
	
	/**
	 * deletes a row
	 * @param currentRowCount
	 */
	public void deleteRow(int currentRowCount) {
		if(this.ITEMS.size() > currentRowCount) {
			LinkedList<Node> l = this.ITEMS.remove(currentRowCount);
			if(l.get(0) instanceof Label) {
				this.LABELS.remove(((Label) l.get(0)).getText());
			}
			else if(l.get(0) instanceof TextField) {
				this.LABELS.remove(((TextField) l.get(0)).getText());
			}
			this.rebuildGrid();
		}
	}
	
	/**
	 * rebuilds the grid from scratch based on ITEMS
	 */
	protected void rebuildGrid() {
		// delete the complete content
		this.getChildren().clear();
		this.LABELS.clear();
		
		// add the new order
		int rowIndex = 0;
		for(LinkedList<Node> rows : this.ITEMS) {
			this.addRow(rowIndex++, rows.toArray(new Node[rows.size()]));
		}
	}
	
	@Override
	public void addRow(int rowIndex, Node... children) {
		// update hashmap
		if(children.length > 0 && children[0] instanceof Label) {
			LABELS.put(((Label) children[0]).getText(), rowIndex);
		}
		else if(children.length > 0 && children[0] instanceof TextField) {
			LABELS.put(((TextField) children[0]).getText(), rowIndex);
		}
		else if(children.length > 0 && children[0].getProperties().containsKey(UNIQUE_ID)) {
			LABELS.put(children[0].getProperties().get(UNIQUE_ID).toString(), rowIndex);
		}
		super.addRow(rowIndex, children);
	}
	
	@Override
	public void add(Node child, int columnIndex, int rowIndex) {
		if(this.ITEMS.size() <= rowIndex)
			this.ITEMS.add(new LinkedList<Node>());
		LinkedList<Node> l = this.ITEMS.get(rowIndex);
		while(l.size() <= columnIndex)
			l.add(null);
		l.remove(columnIndex);
		l.add(columnIndex, child);
		
		if(columnIndex == 0) {
			// update hashmap
			if(child instanceof Label) {
				LABELS.put(((Label) child).getText(), rowIndex);
			}
			else if(child instanceof TextField) {
				LABELS.put(((Label) child).getText(), rowIndex);
			}
			else if(child.getProperties().containsKey(UNIQUE_ID)) {
				LABELS.put(child.getProperties().get(UNIQUE_ID).toString(), rowIndex);
			}
		}
		super.add(child, columnIndex, rowIndex);
	}

	public Integer getRowNumber(String label) {
		return this.LABELS.get(label);
	}
	
	public Integer getRowNumber(Node n) {
		if(n.getProperties().containsKey(UNIQUE_ID)) 
			return this.LABELS.get(n.getProperties().get(UNIQUE_ID).toString());
		return null;
	}

	public int getLastBaseName(String plainName) {
		plainName = plainName + "\\([0-9]\\)$";
		Integer last = this.LABELS.size();
		for(String n : this.LABELS.keySet()) {
			if(n.matches(plainName))
				last = this.LABELS.get(n)+1;
		}
		return last;
	}

	public int getRowCountOwn() {
		return this.ITEMS.size();
	}
}
