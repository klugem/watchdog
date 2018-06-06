package de.lmu.ifi.bio.watchdog.GUI.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.GUI.WorkflowDesignController;
import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.MessageType;
import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.StatusConsole;
import de.lmu.ifi.bio.watchdog.GUI.datastructure.Module;
import de.lmu.ifi.bio.watchdog.GUI.helper.Inform;
import de.lmu.ifi.bio.watchdog.GUI.helper.PreferencesStore;
import de.lmu.ifi.bio.watchdog.GUI.module.WorkflowModule;
import de.lmu.ifi.bio.watchdog.GUI.useraction.AddModuleFromLibraryAction;
import de.lmu.ifi.bio.watchdog.GUI.useraction.MovePropertyAction;
import de.lmu.ifi.bio.watchdog.GUI.useraction.MoveWorkflowModuleAction;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;

public class RasteredGridPane extends GridPane {

	public static final String SEP = "-";
	private static final String SEP_DEP = "x";
	
	private int xSize;
	private int ySize;
	private int xNumber;
	private int yNumber;
	private int maxExtendDist;
	private Bounds lastBounds = null;
	
	private final double OPACITY_FAKTOR = 0.35;
	private final Pane GRID = new Pane();
	private final Pane DEP = new Pane();
	private final HashMap<String, Node> USED_CELLS = new HashMap<> ();
	private final HashMap<Module, Integer> USED_MODULE_COUNT = new HashMap<>();
	private final HashMap<String, Integer> USED_MODULE_VERSION = new HashMap<>();
	private final Rectangle MARK_CELL = new Rectangle();
	private final HashMap<String, Dependency> DEPENDENCIES = new HashMap<>();
	private WorkflowModule clipboard;
	private boolean hideGrid = !PreferencesStore.isGridDisplayedByDefault();
	private boolean isCut = false;
	
	private static final Color GRAY = Color.GRAY.deriveColor(0, 1, 1, 0.5);
			
	public RasteredGridPane() {
		super();
		this.setOnDragDropped(event -> this.onDragDropped(event));
		this.setOnDragOver(event -> this.onDragOver(event));
		this.setOnDragExited(event -> {this.setDisplayGrid(false); this.setDisplayMark(false);});
		this.setOnMouseClicked(event -> this.onMouseClickedOnGrid(event));
	}

	private void onMouseClickedOnGrid(MouseEvent event) {
		if(event.getButton().compareTo(MouseButton.PRIMARY) != 0)
			this.showContextMenu(event);
		
		event.consume();
	}
	
	public void setClipboard(int x, int y, boolean cut) {
		if(!this.isFree(x, y)) {
			Node n = this.USED_CELLS.get(getKey(x, y));
			if(n instanceof WorkflowModule) {
				this.isCut = cut;
				// remove it from the GUI, if it should be cut
				if(cut) {
					n.setOpacity(OPACITY_FAKTOR);
					this.clipboard = (WorkflowModule) n;
				}
				else
					this.clipboard = (WorkflowModule) ((WorkflowModule) n).clone();
			}
		}
	}
	
	/**
	 * returns the parameters that can be used for a module on that position
	 * @return
	 */
	public ArrayList<String> getAvailReturnParams(String key) {
		Pair<Integer, Integer> pos = getPostion(key);
		ArrayList<Dependency> dependencies = this.getDependencies(pos.getKey(), pos.getValue(), true);
		HashSet<String> r = new HashSet<>();
		
		for(Dependency dep : dependencies)  {
			String first = dep.getFirstKey().getKey() + SEP + dep.getFirstKey().getValue();
			WorkflowModule m = this.getModule(first); // origin of dependency
			
			for(String retName : m.getModule().getReturnParams().keySet()) {
				if(r.contains(retName)) {
					// return params with the same name are not allowed. how to handle this ?
					// currently: random with lower id is taken
				}
				else {
					r.add(retName);
				}
			}
		}
		
		// sort and return that stuff
		ArrayList<String> s = new ArrayList<>();
		s.addAll(r);
		Collections.sort(s);
		return s;
	}
	
	public WorkflowModule getModule(String key) {
		if(!this.USED_CELLS.containsKey(key) || !(this.USED_CELLS.get(key) instanceof WorkflowModule))
			return null;
		return (WorkflowModule) this.USED_CELLS.get(key);
	}
	
	/**
	 * Re-inits the size of the grid
	 * @param xSize
	 * @param ySize
	 * @param xNumber
	 * @param yNumber
	 */
	public Pair<Integer, Integer> reinitRaster(int minXNumber, int minYNumber) {
		// delete all
		this.getChildren().clear();
		
		// update the size of the grid
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		Pair<Integer, Integer> p;
		int x, y;
		int eDist = 0;
		for(String pos : this.USED_CELLS.keySet()) {
			p = RasteredGridPane.getPostion(pos);
			x = p.getKey();
			y = p.getValue();
			
			// get max and min size
			if(x < minX)
				minX = x;
			if(x > maxX)
				maxX = x;
			if(y < minY)
				minY = y;
			if(y > maxY)
				maxY = y;
			
			eDist = this.maxExtendDist;
		}
		
		// calculate needed size
		int shiftX = 0;
		int shiftY = 0;
		shiftX = eDist - minX;
		shiftY = eDist - minY;
		
		int xN = Math.max(maxX - minX + 1 + (2 * eDist), minXNumber);
		int yN = Math.max(maxY - minY + 1 + (2 * eDist), minYNumber);
		this.xNumber = xN;
		this.yNumber = yN;
		
		// check, if it must be extended
		if(this.USED_CELLS.size() > 0) {				 
			// shift all modules if needed 
			Pair<Integer, Integer> from, to;
			boolean placed = false;
			ArrayList<String> sortedKeys = new ArrayList<String>(this.USED_CELLS.keySet());
			Collections.sort(sortedKeys, Collections.reverseOrder()); 
			for(String orgin : sortedKeys) {
				from = RasteredGridPane.getPostion(orgin);
				if(shiftX != 0 || shiftY != 0) {
					to = Pair.of(from.getKey() + shiftX, from.getValue() + shiftY);
					placed = this.moveModule(from, to, true);
				}
				// show the stuff that was not shifted yet
				if(!placed) {
					this.moveModule(from, from, true);
				}
			}
			// shift dependencies
			this.moveAllDependencies(shiftX, shiftY);
		}
		this.initGrid();	


		// add the grid
		if(!this.getChildren().contains(this.GRID)) {
			this.getChildren().add(0, this.GRID);
		}
		// add the dependencies
		if(!this.getChildren().contains(DEP))
			this.getChildren().add(1, this.DEP);
		return Pair.of(shiftX, shiftY);
	}
	
	private void showContextMenu(MouseEvent event) {
		// get position
		Pair<Integer, Integer> pos = this.getPosition(event);
		
		// construct contextMenu
		ContextMenu contextMenu = new ContextMenu();
		SeparatorMenuItem sep1 = new SeparatorMenuItem();
		
		// get menu items
		MenuItem paste = new MenuItem("paste task");
		MenuItem show = new MenuItem("show grid");
		MenuItem hide = new MenuItem("hide grid");
		contextMenu.getItems().addAll(paste, sep1, show, hide);
		
		// set coordinates to paste
		paste.setId(getKey(pos));
		
		// add the event handler
		show.onActionProperty().set(e -> { this.setDisplayGrid(true); this.hideGrid = false; });
		hide.onActionProperty().set(e -> { this.hideGrid = true; this.setDisplayGrid(false); });
		paste.onActionProperty().set(e -> this.pasteClipboard(e));
		
		// decide which options should be active
		if(this.hideGrid)
			hide.setDisable(true);
		else
			show.setDisable(true);
		if(this.clipboard == null)
			paste.setDisable(true);
			
		// show the contextmenu
		contextMenu.show(this.getScene().getWindow(), event.getScreenX(), event.getScreenY());
	}
	
	private boolean pasteClipboard(ActionEvent e) {
		Object s = e.getSource();
		if(s instanceof MenuItem) {
			String key = ((MenuItem) s).getId();
			Pair<Integer, Integer> to = getPostion(key);
			int x = to.getKey();
			int y = to.getValue();
			if(this.isFree(x, y)) {
				Pair<Integer, Integer> from = this.clipboard.getPosition();
				boolean ret;
				// simply move it
				if(this.isCut)
					ret = this.moveModule(from, to, false);
				else {
					String name = this.getModule(getKey(from)).getName();
					ret = this.placeContent(this.clipboard, x, y, false);
					// update dependencies
					this.moveDependencies(from, to, true);
					if(ret)
						StatusConsole.addGlobalMessage(MessageType.INFO, "task of module type '"+name+"' was added at " + key +" (copy from " + getKey(from) + ")");
				}
				
				this.clipboard = null;
				return ret;
			}
		}
		return false;
	}

	public void colorCell(int x, int y) {
		this.MARK_CELL.setWidth(this.xSize);
		this.MARK_CELL.setHeight(this.ySize);
		this.MARK_CELL.setX(x*this.xSize);
		this.MARK_CELL.setY(y*this.ySize);
		this.MARK_CELL.setFill(Color.GREENYELLOW);
		this.MARK_CELL.setOpacity(0.2);
		this.setDisplayMark(true);
	}
	
	public static String getDependencyKey(String task, String dependingTask) {
		return task + SEP_DEP + dependingTask;
	}
	

	public void addSeperateDependency(int startX, int startY, int endX, int endY, boolean isFirstOutput, String seperator, Integer prefixLength) {
		// switch direction if required --> switch start and end
		if(!isFirstOutput) { 
			int startXTemp = startX;
			int startYTemp = startY;
			
			startX = endX;
			startY = endY;
			endX = startXTemp;
			endY = startYTemp;
		}

		// create keys
		String task = getKey(startX, startY);
		String dependingTask = getKey(endX, endY);
		String overallKey = getDependencyKey(task, dependingTask);
		
		// create line and show it
		Dependency d = new Dependency(startX, startY, endX, endY, this.xSize, this.ySize, seperator, prefixLength);
		this.DEPENDENCIES.put(overallKey, d);
		this.DEP.getChildren().add(d);
		
		// add on click event
		d.setOnLineClicked(event -> this.removeDependencyAsk(overallKey));
	}
	
	public void addDependency(int startX, int startY, int endX, int endY, boolean isFirstOutput) {
		WorkflowDesignController.configureHasChangedStatic();
		this.addSeperateDependency(startX, startY, endX, endY, isFirstOutput, null, null);
	}
	
	public boolean removeDependencyAsk(String depKey) {
		Pair<String, String> coord = getKeys(depKey);
		String key1 = coord.getKey();
		String key2 = coord.getValue();
		WorkflowModule m1 = (WorkflowModule) this.USED_CELLS.get(key1);
		WorkflowModule m2 = (WorkflowModule) this.USED_CELLS.get(key2);
		
		Optional<ButtonType> result = Inform.confirm("Do you want to delete the dependency between "+m1.getLabel()+" and "+m2.getLabel()+"?");
		if (result.get() == ButtonType.OK) {
		  this.removeDependency(depKey);
		  StatusConsole.addGlobalMessage(MessageType.INFO, "dependency between "+key1+" and "+key2+" was deleted");
			WorkflowDesignController.configureHasChangedStatic();
		  return true;
		}
		return false;
	}
	
	public Dependency getDependency(String depKey) {
		return this.DEPENDENCIES.get(depKey);
	}
	
	public Dependency removeDependency(String depKey) {
		if(this.DEPENDENCIES.containsKey(depKey)) {
			Dependency d = this.DEPENDENCIES.remove(depKey);
			d.finalize();
			this.DEP.getChildren().remove(d);
			WorkflowDesignController.configureHasChangedStatic();
			return d;
		}
		return null;
	}
	
	public void removeDependencies(int x, int y, boolean incomming) {
		for(Dependency d : this.getDependencies(x, y, incomming))
			this.removeDependency(d.getOverallKey());
	}
	
	public ArrayList<Dependency> getDependencies(int x, int y, boolean incoming) {
		ArrayList<Dependency> r = new ArrayList<>();
		String key = getKey(x, y);
		
		for(String k : this.DEPENDENCIES.keySet()) {
			if(incoming) {
				if(k.endsWith(key))
					r.add(this.DEPENDENCIES.get(k));
			}
			else {
				if(k.startsWith(key))
					r.add(this.DEPENDENCIES.get(k));
			}
		}
		return r;
	}
	
	/**
	 * inits the grid
	 */
	protected void initGrid() {	
		// clear the old grid
		ObservableList<Node> childs = this.GRID.getChildren();
		childs.clear();
		// add size constraints
		ObservableList<ColumnConstraints> ccc = this.getColumnConstraints();
		ObservableList<RowConstraints> rcc = this.getRowConstraints();
		ccc.clear();
		rcc.clear();

		// add new one
		for(int i = 0; i <= this.xNumber; i++) {
			Line l = new Line(i*this.xSize, 0, i*this.xSize, this.yNumber*this.ySize);
			l.setStroke(GRAY);
			l.setStrokeLineCap(StrokeLineCap.BUTT);
			l.getStrokeDashArray().setAll(8.0, 2.0);
			l.setMouseTransparent(true);
			childs.add(l);
			
			if(i >= 1) {
				ColumnConstraints cc = new ColumnConstraints(this.xSize);
				cc.setHalignment(HPos.CENTER);
				ccc.add(cc);
			}

		}
		for(int i = 0; i <= this.yNumber; i++) {
			Line l = new Line(0, i*this.ySize, this.xNumber*this.xSize, i*this.ySize);
			l.setStroke(GRAY);
			l.setStrokeLineCap(StrokeLineCap.BUTT);
			l.getStrokeDashArray().setAll(8.0, 2.0);
			l.setMouseTransparent(true);
			childs.add(l);

			if(i >= 1) {
				RowConstraints rc = new RowConstraints(this.ySize);
				rc.setValignment(VPos.CENTER);
				rcc.add(rc);
			}
		}
		// add the cell which should be marked
		this.setDisplayGrid(false);
		this.MARK_CELL.setVisible(false);
		childs.add(this.MARK_CELL);
	}
	
	/**
	 * shows the actual grid when the flag is set to true
	 * @param displayGrid
	 */
	public void setDisplayGrid(boolean displayGrid) {
		if(this.hideGrid)
			this.GRID.setVisible(displayGrid);
	}
	
	/**
	 * shows the marked cell
	 * @param displayMark
	 */
	public void setDisplayMark(boolean displayMark) {
		this.MARK_CELL.setVisible(displayMark);
	}
	
	/**
	 * returns the key for this cell
	 * @param x
	 * @param y
	 * @return
	 */
	public static String getKey(int x, int y) {
		return x + SEP + y;
	}
	
	public static String getKey(Pair<Integer, Integer> p) {
		return getKey(p.getKey(), p.getValue());
	}
	
	public static Pair<Integer, Integer> getPostion(String key) {
		String[] split = key.split(SEP);
		return Pair.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
	}
	
	public static Pair<String, String> getKeys(String depKey) {
		String[] split = depKey.split(SEP_DEP);
		return split.length == 2 ? Pair.of(split[0], split[1]) : null;
	}
	
	/**
	 * replaces the content of a cell
	 * @param n
	 * @param x
	 * @param y
	 */
	public boolean placeContent(Node n, int x, int y, boolean duringReinit) {
		// ensure that version can be used
		if(n instanceof WorkflowModule) {
			WorkflowModule wm = (WorkflowModule) n;
			Module m = wm.getModule();
			int v = m.getVersion();
			String name = m.getName();
			if(this.USED_MODULE_VERSION.containsKey(name)) {
				int usedV = this.USED_MODULE_VERSION.get(name);
				if(usedV != v) {
					StatusConsole.addGlobalMessage(MessageType.ERROR, "task of module type '"+m.getNameForDisplay()+"' was not added because you are already using another version ('" +usedV+ "') of that module"); 
					return false;
				}
			}
		}
		if(x >= 0 && x < this.xNumber && y >= 0 && y < this.yNumber) {
			// reset opacity
			if(this.clipboard != null)
				n.setOpacity(1);
			
			HashMap<Dependency, Boolean> dependencies = null;
			if(n instanceof WorkflowModule) {
				WorkflowModule wm = (WorkflowModule) n;
				dependencies = this.getDependencies(wm.getPosition());
				this.increaseModuleCount(wm);
			}
			
			// add node at this position
			this.add(n, x, y);
			this.USED_CELLS.put(getKey(x, y), n);
			
			// if a node was placed at the end of the grid --> extend it
			if(!duringReinit && ((this.xNumber - x) <= this.maxExtendDist || x < this.maxExtendDist || (this.yNumber - y) <= this.maxExtendDist || y < this.maxExtendDist)) {

				// move the dependency to the new coordinates
				Pair<Integer, Integer> shift = this.adjustGridSize(null);
				int sx = shift.getKey();
				int sy = shift.getValue();
				// update dependencies
				for(Dependency d : dependencies.keySet()) {
					boolean isStart = dependencies.get(d);
					this.removeDependency(d.getOverallKey());
					if(isStart)
						this.addSeperateDependency(x+sx, y+sy, d.getSecondKey().getKey(), d.getSecondKey().getValue(), true, d.getSeparator(), d.getPrefixLength());
					else
						this.addSeperateDependency(d.getFirstKey().getKey(), d.getFirstKey().getValue(), x+sx, y+sy, true, d.getSeparator(), d.getPrefixLength());
				}
			}
			// update the position of the node
			else if(n instanceof WorkflowModule) {
				((WorkflowModule) n).updateCoordinates(x, y);
			}
			WorkflowDesignController.configureHasChangedStatic();
			return true;
		}
		return false;
	}
	
	public Pair<Integer, Integer> adjustGridSize(Bounds bounds) {
		Pair<Integer, Integer> size = this.getScreenFitGridSize(bounds);
		return this.reinitRaster(size.getKey(), size.getValue());
	}
	
	public Pair<Integer, Integer> getScreenFitGridSize(Bounds bounds) {
		if(bounds != null)
			this.lastBounds = bounds;
		else
			bounds = this.lastBounds;
		
		// size of the screen
		double xSize = this.getSizeX();
		double ySize = this.getSizeY();
		double w = this.lastBounds.getWidth();
		double h = this.lastBounds.getHeight();
		int minXNumber = (int) (w / xSize);
		int minYNumber = (int) (h / ySize);
		return Pair.of(minXNumber, minYNumber);
		
	}
	
	/**
	 * returns the grid cell
	 * @param event
	 * @return
	 */
	public Pair<Integer, Integer> getPosition(double xCoordinates, double yCoordinates) {
		Pair<Integer, Integer> p = Pair.of((int) (xCoordinates / (double) this.xSize), (int) (yCoordinates / (double) this.ySize));
		// test if coordinates are ok
		if(p.getKey() >= this.xNumber || p.getKey() < 0)
			throw new IllegalArgumentException("X coordinate is outside the size of the grid: " + xCoordinates);
		if(p.getValue() >= this.yNumber || p.getValue() < 0)
			throw new IllegalArgumentException("Y coordinate is outside the size of the grid: " + yCoordinates);
		return p;
	}

	public Pair<Integer, Integer> getPosition(DragEvent event) {
		return this.getPosition(event.getX(), event.getY());
	}
	
	public Pair<Integer, Integer> getPosition(MouseEvent event) {
		return this.getPosition(event.getX(), event.getY());
	}
	
	/**
	 * tests, if a grid cell is free or already used
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean isFree(int x, int y) {
		return !this.USED_CELLS.containsKey(getKey(x, y));
	}

	public boolean isFree(Pair<Integer, Integer> p) {
		return this.isFree(p.getKey(), p.getValue());
	}
	
	public int getSizeX() {
		return this.xSize;
	}
	
	public int getSizeY() {
		return this.ySize;
	}
	
	public Node deleteModule(int x, int y, boolean deleteDependencies) {
		if(!this.isFree(x, y)) {
			String key = getKey(x, y);
			Node n = this.USED_CELLS.remove(key);
			this.getChildren().remove(n);
			
			if(n instanceof WorkflowModule)
				this.decreaseModuleCount(((WorkflowModule) n));
			
			// remove dependencies
			if(deleteDependencies) {
				this.removeDependencies(x, y, true);
				this.removeDependencies(x, y, false);
				if(n instanceof WorkflowModule) {
					WorkflowModule wm = ((WorkflowModule) n);
					wm.unregisterData();
				}
			}
			return n;
		}
		return null;
	}
	
	/**
	 * counts how often a module is used within the workflow
	 * @param wm
	 * @return
	 */
	public int getModuleCount(Module module) {
		int c = 0;
		if(this.USED_MODULE_COUNT.containsKey(module))
			c = this.USED_MODULE_COUNT.get(module);
		return c;
	}

	/**
	 * counts how often a module is used for versions (decrease module count)
	 * @param wm
	 */
	private void decreaseModuleCount(WorkflowModule wm) {
		Module key = wm.getModule();
		int c = this.getModuleCount(key);
		// delete entry
		if(c <= 1) {
			this.USED_MODULE_COUNT.remove(key);
			this.USED_MODULE_VERSION.remove(key.getName());
		}
		else
			this.USED_MODULE_COUNT.put(key, Math.max(0, c-1));
	}
	
	/**
	 * counts how often a module is used for versions (increase module count)
	 * @param wm
	 */
	private void increaseModuleCount(WorkflowModule wm) {
		Module key = wm.getModule();
		int c = this.getModuleCount(key);
		this.USED_MODULE_COUNT.put(key, c+1);
		this.USED_MODULE_VERSION.put(key.getName(), key.getVersion());
	}

	private void moveAllDependencies(int shiftX, int shiftY) {
		for(String depKey : new ArrayList<>(this.DEPENDENCIES.keySet())) {
			Dependency d = this.getDependency(depKey);
			d.shift(shiftX, shiftY);
			
			// update key
			this.DEPENDENCIES.remove(depKey);
			String overallKey = d.getOverallKey();
			this.DEPENDENCIES.put(overallKey, d);
			d.setOnLineClicked(event -> this.removeDependencyAsk(overallKey));
		}
	}
	
	private HashMap<Dependency, Boolean> getDependencies(Pair<Integer, Integer> pos) {
		HashMap<Dependency, Boolean> res = new HashMap<>();
		String posKey = getKey(pos.getKey(), pos.getValue());
		for(String depKey : new ArrayList<>(this.DEPENDENCIES.keySet())) {
			if(depKey.startsWith(posKey) || depKey.endsWith(posKey)) {
				res.put(this.getDependency(depKey), depKey.startsWith(posKey));
			}
		}
		return res;
	}
	
	private void moveDependencies(Pair<Integer, Integer> from, Pair<Integer, Integer> to, boolean copy) {
		String toKey = getKey(to.getKey(), to.getValue());
		String fromKey = getKey(from.getKey(), from.getValue());
		for(Dependency d : this.getDependencies(from).keySet()) {
			if(!copy) 
				this.removeDependency(d.getOverallKey());

			Pair<String, String> newKeys = getKeys(d.getOverallKey().replaceFirst(fromKey, toKey));
			Pair<Integer, Integer> newFrom = getPostion(newKeys.getKey());
			Pair<Integer, Integer> newTo = getPostion(newKeys.getValue());
			this.addSeperateDependency(newFrom.getKey(), newFrom.getValue(), newTo.getKey(), newTo.getValue(), true, d.getSeparator(), d.getPrefixLength());
		}
	}
	

	public boolean moveModule(Pair<Integer, Integer> from, Pair<Integer, Integer> to, boolean duringReinit) {
		String fromKey = getKey(from.getKey(), from.getValue());
		if(this.USED_CELLS.containsKey(fromKey)) {
			// remove it from its old position	
			Node n = this.deleteModule(from.getKey(), from.getValue(), false);
			
			// node was placed successfully
			if(this.placeContent(n, to.getKey(), to.getValue(), duringReinit)) {
				if(n instanceof WorkflowModule) {
					int newX = ((WorkflowModule) n).getPosition().getKey();
					int newY = ((WorkflowModule) n).getPosition().getValue();
					
					// update dependencies
					if(!duringReinit && newX == to.getKey() && newY == to.getValue()) {
						this.moveDependencies(from, to, false);
					}
				}
				return true;
			}
			// try to place it at the old position
			else {
				this.moveDependencies(to, from, false); // move it back
				this.placeContent(n, from.getKey(), from.getValue(), duringReinit);
			}
		}
		return false;
	}
	
	/*********************************************** EVENT HANDLER **************************************/
	
	/**
	 * called, when something is dragged over the grid
	 * @param event
	 */
	private void onDragOver(DragEvent event) {
		AddModuleFromLibraryAction m = AddModuleFromLibraryAction.getAddModuleFromLibraryAction(event);
		MoveWorkflowModuleAction move = MoveWorkflowModuleAction.getMoveWorkflowModuleAction(event);
		// new module from tool library
		if(m != null || move != null) {
			this.setDisplayGrid(true);
			// test, if the cell does accept an new module
			try {
				if(this.isFree(this.getPosition(event))) {
					if(m != null)
						event.acceptTransferModes(TransferMode.COPY);
					else
						event.acceptTransferModes(TransferMode.MOVE);
			    	
					// mark the cell that is currently selected
					Pair<Integer, Integer> pos = this.getPosition(event);
					int x = pos.getKey();
					int y = pos.getValue();
					this.colorCell(x, y);
				}
			}
			catch(IllegalArgumentException e) { e.printStackTrace();}
		}
		// check, if a property should be deleted
		else {
			MovePropertyAction d = MovePropertyAction.getMovePropertyAction(event);
			if(d != null)
				event.acceptTransferModes(TransferMode.COPY);
		}
	    event.consume();
	}
	
	/**
	 * called, when something is dropped on the grid
	 * @param event
	 */
	private void onDragDropped(DragEvent event) {
		boolean success = false;
		// module is droped that was fetched from library
		AddModuleFromLibraryAction a = AddModuleFromLibraryAction.getAddModuleFromLibraryAction(event);
		if(a != null) {
			// hide visual stuff
			this.setDisplayGrid(false);
			this.setDisplayMark(false);
			
			// process it
			Module m = a.getModule();
			Pair<Integer, Integer> pos = this.getPosition(event);
			int x = pos.getKey();
			int y = pos.getValue();
			WorkflowModule wm = WorkflowModule.getModule(m, x, y, this);
			success = this.placeContent(wm, x, y, false);
			if(success)
				StatusConsole.addGlobalMessage(MessageType.INFO, "task of module type '"+m.getNameForDisplay()+"' was added at " + wm.getKey()); 
		}
		else {
			// an old module might be moved
			MoveWorkflowModuleAction move = MoveWorkflowModuleAction.getMoveWorkflowModuleAction(event);
			if(move != null)
				success = this.moveModule(move.getCoordinates(), this.getPosition(event), false);
			else {
				// an property might be dropped that should be deleted
				MovePropertyAction d = MovePropertyAction.getMovePropertyAction(event);
				if(d != null) {
					success = true;
				}
			}
		}
		event.setDropCompleted(success);
		event.consume();
	}

	/**
	 * true, if a dependency between both exist, no matter which oriantation is given
	 * @param originKey
	 * @param targetKey
	 * @return
	 */
	public boolean isDependencySet(String originKey, String targetKey) {
		return this.DEPENDENCIES.containsKey(RasteredGridPane.getDependencyKey(originKey, targetKey)) || this.DEPENDENCIES.containsKey(RasteredGridPane.getDependencyKey(targetKey, originKey));
	}
	
	/**
	 * returns all modules that are currently active in the GRID
	 * @return
	 */
	public HashMap<String, WorkflowModule> getActiveModules() {
		HashMap<String, WorkflowModule> modules = new HashMap<>();
		
		for(Node n : this.USED_CELLS.values()) {
			if(n instanceof WorkflowModule)
				modules.put(((WorkflowModule) n).getKey(), (WorkflowModule) n);
		}
		return modules;
	}

	@SuppressWarnings("unchecked")
	public void clear() {
		// reset variables
		this.clipboard = null;
		this.hideGrid = !PreferencesStore.isGridDisplayedByDefault();
		this.isCut = false;
				
		// init grid
		this.adjustGridSize(null);
		
		// delete all tasks
		for(String key : ((HashMap<String, Node>) this.USED_CELLS.clone()).keySet()) {
			Pair<Integer, Integer> pos = getPostion(key);
			this.deleteModule(pos.getKey(), pos.getValue(), true);
		}
		// ensure that all dependencies are really gone
		this.DEP.getChildren().clear();
		this.DEPENDENCIES.clear();
	}

	public int getMaxExtendDist() {
		return this.maxExtendDist;
	}

	/**
	 * inits the grid with some parameters
	 * @param xSize
	 * @param ySize
	 * @param maxExtendDist
	 */
	public void initGrid(int xSize, int ySize, int maxExtendDist) {
		this.xSize = xSize;
		this.ySize = ySize;
		this.maxExtendDist = maxExtendDist;
	}
}
