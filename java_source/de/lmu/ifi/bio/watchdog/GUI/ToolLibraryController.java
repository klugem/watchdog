package de.lmu.ifi.bio.watchdog.GUI;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.lang3.tuple.Pair;
import org.xml.sax.SAXException;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.MessageType;
import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.StatusConsole;
import de.lmu.ifi.bio.watchdog.GUI.datastructure.Category;
import de.lmu.ifi.bio.watchdog.GUI.datastructure.ExtendedClipboardContent;
import de.lmu.ifi.bio.watchdog.GUI.datastructure.Module;
import de.lmu.ifi.bio.watchdog.GUI.helper.PreferencesStore;
import de.lmu.ifi.bio.watchdog.GUI.interfaces.ListLibraryView;
import de.lmu.ifi.bio.watchdog.GUI.layout.DragableTableCellFactory;
import de.lmu.ifi.bio.watchdog.GUI.layout.DragableTreeTableCell;
import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import de.lmu.ifi.bio.watchdog.GUI.useraction.AddModuleFromLibraryAction;
import de.lmu.ifi.bio.watchdog.helper.Functions;
import de.lmu.ifi.bio.watchdog.helper.Parameter;
import de.lmu.ifi.bio.watchdog.helper.returnType.ReturnType;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import javafx.beans.value.ObservableValueBase;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;

/**
 * controller for the tool library
 * @author kluge 
 *
 */
public class ToolLibraryController implements Initializable {

	@FXML private VBox toolLibVbox;
	@FXML private TreeTableView<ListLibraryView> modules;
	@FXML private TreeTableColumn<ListLibraryView, ListLibraryView> names;
	@FXML private TextField search;
	@FXML private Label filterLabel;
	
	private final LinkedHashMap<String, Module> LOADED_MODULES =  new LinkedHashMap<>();
	private final HashMap<String, String> INCLUDE_DIRS =  new HashMap<>();

	@Override 
	public void initialize(URL location, ResourceBundle resources) {		
		// add event handler
		this.search.textProperty().addListener((observable, oldValue, newValue) -> { this.updateLibrary(newValue); });
		this.names.setCellValueFactory((CellDataFeatures<ListLibraryView, ListLibraryView> p) -> new ReadOnlyLibraryListViewProperty(p.getValue().getValue()));
		this.names.setCellFactory(new DragableTableCellFactory<ListLibraryView, ListLibraryView>(event -> this.onDragStart(event), null));
		
		// set css property
		this.modules.getStyleClass().add("noHorScrollBar");
		this.modules.setPrefHeight(100000); // take all space you can take ;)
		this.filterLabel.setGraphic(ImageLoader.getImage(ImageLoader.FILTER_SMALL));
	}
	
	protected void loadModuleLibrary() {
		try {
			if(PreferencesStore.hasWatchdogBaseDir() && PreferencesStore.getMouleFolders().size() > 0) {
				Functions.filterErrorStream();
				// get information about the stored modules
				File defaultSchemaPath = new File(PreferencesStore.getWatchdogBaseDir() + File.separator + XMLParser.FILE_CHECK);
				File defaultTmpPath = new File(PreferencesStore.getWatchdogBaseDir() + File.separator + XMLParser.TMP_FOLDER);
				HashMap<String, String> moduleFolders = PreferencesStore.getMouleFolders();
				ArrayList<String> paths = new ArrayList<String>(moduleFolders.values());
				HashMap<String, String> modules = getNamesOfModules(paths);
				HashMap<String, Pair<Pair<File, File>, HashMap<String, Parameter>>> modulesAndParameters = getParameterOfModules(modules, null, defaultSchemaPath.getParent(), defaultTmpPath);
				HashMap<String, Pair<HashMap<String, ReturnType>, String>> retInfo = getReturnInformation(modules, null, PreferencesStore.getWatchdogBaseDir(), defaultTmpPath);
				this.setModules(moduleFolders, modulesAndParameters, retInfo);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns parameters of all modules
	 * @params modules
	 * @return
	 * @throws SAXException 
	 */
	public static HashMap<String, Pair<Pair<File, File>, HashMap<String, Parameter>>> getParameterOfModules(HashMap<String, String> modules, File schemaFile, String xsdRootDir, File tmpBase) throws SAXException {
		Schema schema = null;
		// don't do that in GUI made where no actual XML document is parsed
		if(schemaFile != null) {
			// load the schema in XSD 1.1 format
			SchemaFactory schemaFac = SchemaFactory.newInstance(XMLParser.XML_1_1);
			try { schema = schemaFac.newSchema(schemaFile); }
			catch(Exception e) {
				e.printStackTrace();
				StatusConsole.addGlobalMessage(MessageType.ERROR, "Problem during the loading of the modules: " + e.getMessage());
				return null;
			}
		}
		
		// create a new document factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringElementContentWhitespace(true);
		dbf.setNamespaceAware(true);
		if(schema != null) dbf.setSchema(schema);
		// find the parameters
		return XMLParser.getParameters(dbf, new HashSet<>(modules.values()), xsdRootDir, tmpBase);
	}
	
	public static HashMap<String, Pair<HashMap<String, ReturnType>, String>> getReturnInformation(HashMap<String, String> modules, File schemaFile, String watchDogBase, File tmpBaseDir) throws SAXException {
		Schema schema = null;
		// don't do that in GUI made where no actual XML document is parsed
		if(schemaFile != null) {
			// load the schema in XSD 1.1 format
			SchemaFactory schemaFac = SchemaFactory.newInstance(XMLParser.XML_1_1);
			schema = schemaFac.newSchema(schemaFile);
		}
		
		// create a new document factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringElementContentWhitespace(true);
		dbf.setNamespaceAware(true);
		if(schema != null) dbf.setSchema(schema);
		// find the parameters
		return XMLParser.getReturnInformation(dbf, new HashSet<>(modules.values()), watchDogBase, tmpBaseDir);
	}

	/**
	 * Retuns all modules stored in that folders
	 * @param moduleFolders
	 * @return
	 */
	public static HashMap<String, String> getNamesOfModules(ArrayList<String> moduleFolders) {
		// create a new document factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringElementContentWhitespace(true);
		dbf.setNamespaceAware(true);
		// find the modules
		return XMLParser.findModules(dbf, moduleFolders);
	}

	/** is called when the drag of a module is started */
	private void onDragStart(MouseEvent mouseEvent) {
		if(mouseEvent.getSource() != null && mouseEvent.getSource() instanceof DragableTreeTableCell) {
			String moduleName = ((DragableTreeTableCell<?,?>) mouseEvent.getSource()).getText();
			
			// draged item is a module and not a category
			if(this.LOADED_MODULES.containsKey(moduleName)) {
				Module m = this.LOADED_MODULES.get(moduleName);
				Dragboard db = ((DragableTreeTableCell<?,?>) mouseEvent.getSource()).startDragAndDrop(TransferMode.COPY);
				ExtendedClipboardContent content = new ExtendedClipboardContent();
                content.putUserAction(new AddModuleFromLibraryAction(m));
                db.setContent(content);
			}
            mouseEvent.consume();
		}
	}

	/**
	 * is used to update the displayed modules
	 * @pattern current pattern to search for
	 */
	private void updateLibrary(String pattern) {
		TreeItem<ListLibraryView> root = new TreeItem<>(Module.getRootModule());
		LinkedHashMap<String, ArrayList<TreeItem<ListLibraryView>>> modules2add = new LinkedHashMap<>();
		ArrayList<TreeItem<ListLibraryView>> sublist;
		String cat;
		TreeItem<ListLibraryView> i;
		HashMap<String, Integer> total = new HashMap<>();
		
		// determine which modules to show
		for(Module m : this.LOADED_MODULES.values()) {		
			cat = m.getCategory();
			
			// count modules per category
			if(!total.containsKey(cat))
				total.put(cat, 0);
			total.put(cat, total.get(cat)+1);
			
			// module should be displayed
			if(m.getNameForDisplay().contains(pattern)) {
				i = new TreeItem<ListLibraryView>(m);
				if(!modules2add.containsKey(cat))
					modules2add.put(cat, new ArrayList<TreeItem<ListLibraryView>>());

				// add the module
				sublist = modules2add.get(cat);
				sublist.add(i);
			}
		}
		
		// clean the old stuff
		if(this.modules.getRoot() != null)
			this.modules.getRoot().getChildren().clear();
		
		// show them
		for(String c : modules2add.keySet()) {
			// add a new category
			ArrayList<TreeItem<ListLibraryView>> modules = modules2add.get(c);
			String catName = c + " (" + modules.size() + "/" + total.get(c) + ")";
			TreeItem<ListLibraryView> category = new TreeItem<>(new Category(catName));
			category.getChildren().addAll(modules);
			if(pattern.length() > 0)
				category.setExpanded(true);
			root.getChildren().add(category);
		}
		this.modules.setRoot(root);
	}

	public void setModules(HashMap<String, String> moduleFolders, HashMap<String, Pair<Pair<File, File>, HashMap<String, Parameter>>> modulesAndParameters, HashMap<String, Pair<HashMap<String, ReturnType>, String>> retInfo) {
		this.INCLUDE_DIRS.clear();
		this.LOADED_MODULES.clear();
		HashMap<String, Module> modUnsorted = new HashMap<>();
		
		for(String name : modulesAndParameters.keySet()) {
			Pair<Pair<File, File>, HashMap<String, Parameter>> data = modulesAndParameters.get(name);
			@SuppressWarnings("unused")
			String pathCached = data.getLeft().getRight().getAbsolutePath();
			String pathOrg = data.getLeft().getLeft().getAbsolutePath();
			HashMap<String, Parameter> params = data.getRight();
			HashMap<String, ReturnType> ret = retInfo.containsKey(name) ? retInfo.get(name).getLeft() : null;
			String tmp[] = name.split(XMLParser.VERSION_SEP);
			String nameFull = name;
			name = tmp[0];
			int version = Integer.parseInt(tmp[1]);
			
			int maxLength = 0;
			String lastCat = "";
			
			// find matching base dir
			for(String catName : moduleFolders.keySet()) {
				String searchBase = moduleFolders.get(catName);
				if(pathOrg.startsWith(searchBase)) {
					if(searchBase.length() > maxLength) {
						maxLength = searchBase.length();
						lastCat = catName;
					}
				}
			}
			
			modUnsorted.put(nameFull, new Module(name, new File(pathOrg).getParentFile().getAbsolutePath(), "", params, ret, lastCat, version));
			this.INCLUDE_DIRS.put(name, moduleFolders.get(lastCat));
		}
		
		// sort it
		modUnsorted.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> this.LOADED_MODULES.put(entry.getValue().getNameForDisplay(), entry.getValue()));
		// update the list
		this.updateLibrary("");
	}
	
	public String getIncludeDir4Module(String name) {
		return this.INCLUDE_DIRS.get(name);
	}
	
	/**
	 * must be name + sep + version
	 * @param name
	 * @return
	 */
	public Module getModuleData(String name) {
		return this.LOADED_MODULES.get(name);
	}
	
	/************************************* EVENT HANDLER *******************************************/
	
	private class ReadOnlyLibraryListViewProperty extends ObservableValueBase<ListLibraryView> {
		
		private final ListLibraryView OBJECT;
		private ReadOnlyLibraryListViewProperty(ListLibraryView object) {
			this.OBJECT = object;
		}

		@Override
		public ListLibraryView getValue() {
			return this.OBJECT;
		}
	}

	public void setSearchFocus() {
		this.search.requestFocus();
	}
}