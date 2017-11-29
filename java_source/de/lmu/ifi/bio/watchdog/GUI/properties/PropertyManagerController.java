package de.lmu.ifi.bio.watchdog.GUI.properties;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.event.DeletePropertyEvent;
import de.lmu.ifi.bio.watchdog.GUI.helper.AddButtonToTitledPane;
import de.lmu.ifi.bio.watchdog.GUI.module.WorkflowModuleController;
import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PropertyViewFactory;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.PropertyViewType;
import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.Constants;
import de.lmu.ifi.bio.watchdog.helper.Environment;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import de.lmu.ifi.bio.watchdog.helper.ProcessBlock.ProcessBlock;
import de.lmu.ifi.bio.watchdog.helper.ProcessBlock.ProcessFolder;
import de.lmu.ifi.bio.watchdog.helper.ProcessBlock.ProcessSequence;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Controller for property manager
 * @author kluge
 *
 * @param <A>
 */
public class PropertyManagerController implements Initializable {
 
	@FXML private VBox root;
	@FXML private VBox properties;
	@FXML private TitledPane pane;
	
	public static String SUFFIX_SEP = "'+'*'";
	private Button add = new Button();
	private PropertyViewType type;
	private final HashMap<Integer, PropertyLine> PROPERTIES = new HashMap<>();
	private final LinkedHashSet<Integer> USED_NUMBERS = new LinkedHashSet<>();
	private static final HashMap<PropertyLine, Integer> USED_COLORS = new HashMap<>();
	private final HashMap<String, Integer> USED_NAMES = new HashMap<>();
	
	private static final ArrayList<Color> COLOR_STORE = new ArrayList<>();

	static {
		COLOR_STORE.add(Color.GREENYELLOW);
		COLOR_STORE.add(Color.ORANGE);
		COLOR_STORE.add(Color.CORNFLOWERBLUE);
		COLOR_STORE.add(Color.CADETBLUE);
		COLOR_STORE.add(Color.YELLOW);
		COLOR_STORE.add(Color.DARKKHAKI);
		COLOR_STORE.add(Color.PALEVIOLETRED);
		COLOR_STORE.add(Color.PALEGREEN);
		COLOR_STORE.add(Color.AZURE);
	}
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.properties.setMinHeight(0);
		this.updateSize();
		
		// add event handler for delete operation
		this.properties.addEventHandler(DeletePropertyEvent.DELETE_EVENT_TYPE, event -> this.delete(event.getNumber()));
		// bit of a hack because calculating of distances needs components to be added to a scene...
		AddButtonToTitledPane.registerAddImageCall((e) -> AddButtonToTitledPane.addImage(this.pane, this.add, ImageLoader.getImage(ImageLoader.ADD_SMALL), event -> this.addProperty(), true));
	}

	/**
	 * deletes all set properties
	 */
	public void clear() {
		this.USED_NAMES.clear();
		this.USED_NUMBERS.clear();
		this.PROPERTIES.clear();
		this.properties.getChildren().clear();
		this.updateSize();
	}
	
	/**
	 * used to set to with property type this controller belongs
	 * @param t
	 */
	protected void setPropertyViewType(PropertyViewType t) {
		this.type = t;
		this.pane.setText(this.type.getLabel());
	}
	
	private int getNumber() {
		int i = 1;
		while(this.USED_NUMBERS.contains(i))
			i++;
		return i;
	}
	
	private static int getColorNumber() {
		int i = 1;
		while(USED_COLORS.values().contains(i))
			i++;
		return i;
	}
	
	public Color getColor(Integer n) {		
		if(n <= COLOR_STORE.size())
			return COLOR_STORE.get(n-1);
		else {
			Color c = Color.rgb(new Random().nextInt(256), new Random().nextInt(256), new Random().nextInt(256));
			COLOR_STORE.add(c);
			return c;
		}
	}
	
	protected void sortByName() {
		LinkedHashMap<String, PropertyLine> names = new LinkedHashMap<>();
		int suffix = 0;
		for(PropertyLine pl : this.PROPERTIES.values()) {
			String n = pl.getStoredData().getName() + SUFFIX_SEP + suffix;
			names.put(n, pl);
			suffix++;			
		}
		// add the entries in a sorted order
		this.properties.getChildren().clear();
		names.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> this.properties.getChildren().add(entry.getValue()));
	}
		
	public void addProperty() { 
		int number = this.getNumber();
		int colorNumber = getColorNumber();
		Color c = this.getColor(colorNumber);
		PropertyLine p = this.loadProperty(c, number, null);
		if(p != null) {
			USED_COLORS.put(p, colorNumber);
			this.sortByName();
		}
	}
	
	public PropertyLine loadProperty(Color c, int number, XMLDataStore data2load) {
		// check, if data is already loaded
		for(PropertyLine i : this.PROPERTIES.values()) {
			if(i.hasDataStored() && i.getStoredData().equals(data2load))
				return null;
		}
		// create the element
		PropertyLine p = PropertyLine.getPropertyLine(this, new PropertyViewFactory(this.type), number, null, c);

		// let the user enter the data via the GUI if no data is set
		if(data2load == null)
			p.config();
		else {
			p.setStoredData(data2load);
			p.setLabel(data2load.getName());
		}
				
		// show it on the GUI if some data is there
		if(p.hasDataStored()) {
			// check, if name is already in use --> update the label, if required
			int usedNumber = this.updateLabel(p, null);
			
			// store the data internally
			this.PROPERTIES.put(number, p);
			this.properties.getChildren().add(p);
			this.updateSize();
			this.USED_NUMBERS.add(number);
			this.USED_NAMES.put(p.getPropertyData().getXMLData().getName(), (usedNumber != -1 ? usedNumber : number));
			// also mark the color as used if is a default one
			int index = COLOR_STORE.indexOf(c);
			if(index >= 0)
				USED_COLORS.put(p, index+1);	
		}
		else
			p = null; // let it eat the gc!
		
		return p;
	}
	
	protected int updateLabel(PropertyLine p, PropertyLine target) {
		if(!p.hasDataStored())
			return -1;
		
		if(p.getPropertyData().getXMLData() instanceof ProcessBlock && this.USED_NAMES.containsKey(p.getPropertyData().getXMLData().getName())) {
			int udateNumber = this.USED_NAMES.get(p.getPropertyData().getXMLData().getName());
			Color c = this.getColor(udateNumber);
			p.setDisplayNumber(udateNumber, c);
			return udateNumber;
		}
		else if(p.hasDataStored() && p.getPropertyData().hasXMLData()) {
			// reset the label and color
			p.resetCustomLabelAndColor();
			
			if(target != null && p.getPropertyData().getID() == target.getPropertyData().getID()) {
				this.USED_NAMES.put(p.getPropertyData().getXMLData().getName(), p.getPropertyData().getNumber());
			}
			
		}
		return p.getPropertyData().getNumber();
	}

	// Vbox is hidden, if no elements are added yet
	private void updateSize() {
		if(this.PROPERTIES.size() == 0) {
			this.properties.setMaxHeight(0);
			this.pane.setExpanded(false);
		}
		else {
			this.properties.setMaxHeight(Region.USE_COMPUTED_SIZE);
			this.pane.setExpanded(true);
		}
	}
	
	public ArrayList<XMLDataStore> getXMLData() {
		ArrayList<XMLDataStore> r = new ArrayList<>();
		for(PropertyLine p : this.PROPERTIES.values())
			r.add(p.getStoredData());
		return r;
	}
	
	public void delete(int number) {
		PropertyLine p = this.PROPERTIES.remove(number);
		if(p != null) {
			String name = p.getStoredData().getName();
			this.USED_NAMES.remove(name);
			USED_COLORS.remove(p);
			this.properties.getChildren().remove(p);
			this.updateSize();
			ArrayList<PropertyLine> sameName = this.getDataWithSameName4Append(name);
			// test if there is no other data with that name --> append
			if(sameName.size() == 0) {
				WorkflowModuleController.deleteProperty(p.getStoredData(), number);
				this.USED_NUMBERS.remove(number);
			}
			
			// unregister the stuff
			XMLDataStore data = p.getStoredData();			
			if(data != null) {
				data.onDeleteProperty();
				if(data instanceof Environment)
					XMLDataStore.unregisterData(data);
				else if(data instanceof ProcessBlock) {
					XMLDataStore.unregisterData(data);
					// special case: process block append
					if(sameName.size() > 0 && (data instanceof ProcessFolder || data instanceof ProcessSequence)) {
						boolean isAppend = false;
						if(data instanceof ProcessFolder)
							isAppend = ((ProcessFolder) data).gui_append;
						else if(data instanceof ProcessSequence)
							isAppend = ((ProcessSequence) data).gui_append;
						
						// if it was the first one --> make another the first one
						PropertyLine changeTarget = sameName.get(0);
						XMLDataStore change = null;
						if(!isAppend) {
							change = changeTarget.getStoredData();
							if(change instanceof ProcessFolder) {
								((ProcessFolder) change).gui_append = false;
							}
							else if(change instanceof ProcessSequence) {
								((ProcessSequence) change).gui_append = false;
							}
						}
						// GUI update might be needed in order to change color and number
						for(PropertyLine a : this.PROPERTIES.values()) {
							this.updateLabel(a, changeTarget);
						}
						// change color and number on main window 
						WorkflowModuleController.updateAppendProperties(change, p.getPropertyData().getNumber(), changeTarget.getPropertyData().getNumber(), changeTarget.getPropertyData().getColor());
					}
				}
				else if(data instanceof ExecutorInfo)
					XMLDataStore.unregisterData(data);
				else if(data instanceof Constants)
					XMLDataStore.unregisterData(data);
				else {
					System.err.println("delete of PropertyManager is not implemented for object of type '"+ data.getClass().getSimpleName() +"'");
					System.exit(1);
				}
			}
		}
	}
	
	protected ArrayList<PropertyLine> getDataWithSameName4Append(String name) {
		ArrayList<PropertyLine> r = new ArrayList<>();
		for(PropertyLine l : this.PROPERTIES.values()) {
			if(l.hasDataStored() && l.getPropertyData().hasXMLData()) {
				if(name.equals(l.getPropertyData().getXMLData().getName())) {
					r.add(l);
				}
			}
		}
		return r;
	}

	public void updateAppendColor(String name, Color p) {
		for(PropertyLine l : this.getDataWithSameName4Append(name)) {
			l.getPropertyData().setDisplayNumber(null, p);
		}
	}

	public void updateUsedColor(PropertyLine p, int number, Color c) {
		COLOR_STORE.set(number-1, c);	
		USED_NAMES.put(p.getPropertyData().getXMLData().getName(), number);
	}
}