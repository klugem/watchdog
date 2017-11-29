package de.lmu.ifi.bio.watchdog.GUI.properties.views;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.MessageType;
import de.lmu.ifi.bio.watchdog.executor.ExecutorInfo;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

public abstract class PluginPropertyViewController<T extends XMLDataStore> extends PropertyViewController {

	@FXML protected TextField name;
	@FXML private ChoiceBox<PluginView<T>> type;
	@FXML private Pane specific;

	/* stores infos about all plugins to load */
	private static final LinkedHashMap<Class<? extends XMLDataStore>, Class<? extends PluginView<? extends XMLDataStore>>> LOADED_VIEWS = new LinkedHashMap<>();
	private final HashMap<Class<? extends XMLDataStore>, Integer> DATA_INDEX = new HashMap<>();
	
	private T dataStore;
	protected PluginView<T> activeGUIView;
	private final HashMap<String, PluginView<T>> REAL_VIEWS = new HashMap<>();
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);
		// add all registered xml plugins
		boolean first = true;
		try {
			HashSet<Class<? extends XMLDataStore>> classesToLoad = getRegisteredClassesOfGenericType();
			// ensure that we go some classes
			if(classesToLoad.size() == 0) {
				throw new IllegalArgumentException("No plugins that implement the type '" + this.getGenericName() + "' were registered!");
			}
			// iterate over all classes
			for(Class<? extends XMLDataStore> c : classesToLoad) {
				PluginView<?> viewDummy = LOADED_VIEWS.get(c).newInstance();
				this.type.getItems().add((PluginView<T>) viewDummy);
				this.DATA_INDEX.put(c, this.DATA_INDEX.size());
				if(first)
					this.type.getSelectionModel().select((PluginView<T>) viewDummy);
				first = false;
			}
		} catch(Exception e) { e.printStackTrace(); System.exit(1);} // must work 

		// add checker
		this.name.textProperty().addListener(event -> this.validate());
		this.addValidateToControl(this.name, "name", f -> this.checkName((TextField) f));
		this.type.setOnAction(c -> this.onChangeType());
		
		// load event handler, validators, ...
		this.initGUIElements();
		this.onChangeType();
	}
	
	@SuppressWarnings("unchecked")
	private HashSet<Class<? extends XMLDataStore>> getRegisteredClassesOfGenericType() {
		HashSet<Class<? extends XMLDataStore>> results = new HashSet<>();
		try {
			Class<T> genericClass = (Class<T>) Class.forName(this.getGenericName());
			for(Class<? extends XMLDataStore> c : LOADED_VIEWS.keySet()) {
				// ensure that we load only classes of the correct type
				if(genericClass.isAssignableFrom(c)) {
					results.add(c);
				}
			}
		} catch(Exception e) { e.printStackTrace(); } 
		return results;
	}
	
	@SuppressWarnings("unchecked")
	private String getGenericName() {
        return ((Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]).getTypeName();
    }

	protected abstract void initGUIElements();

	@Override
	public boolean validate() {
		return this.validate(this.activeGUIView.getName());
	}
	

	/**
	 * is called when ever the type is changed
	 */
	private void onChangeType() {
		try {
			PluginView<T> viewDummy = this.type.getSelectionModel().getSelectedItem();
			// create instance
			if(!this.REAL_VIEWS.containsKey(viewDummy.getName()))
				this.REAL_VIEWS.put(viewDummy.getName(), this.getInstance(viewDummy));
			
			// set the instance
			this.activeGUIView = this.REAL_VIEWS.get(viewDummy.getName());

			// add it on the GUI
			this.specific.getChildren().clear();
			if(this.specific.getChildren().size() > 0)
				this.specific.getChildren().set(0, this.activeGUIView);
			else
				this.specific.getChildren().add(this.activeGUIView);
			
			// call validate to get initial coloring
			this.validate();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	private PluginView<T> getInstance(PluginView<T> viewDummy) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		PluginView<T> pane = null;
		try {
			pane = viewDummy.getClass().newInstance();
		} catch(Exception e) { e.printStackTrace(); System.exit(1);} // must work
		
		PluginView<T> view = PluginView.getExecutorPropertyView(viewDummy.getFXMLResourceFilename(), pane);
		view.addValidateToControl(this);
		return view;
	}
	
	public boolean checkName(TextField f) {
		if(this.isEmpty((TextField) f, "Name for executor property is missing."))
				return false;
		if(!this.hasUniqueName(((TextField) f).getText()))
			return false;
		
		// all was ok!
		return true;
	}

	public T getStoredData() {
		return this.dataStore;
	}
		
	protected void storeXMLData(T data) {
		this.dataStore = data;
		XMLDataStore.registerData(data);
	}

	public void loadData(T data) {
		if(data != null) {
			// unregister that data or otherwise name will be blocked!
			XMLDataStore.unregisterData(data);
			this.isDataLoaded = true;
			this.type.getSelectionModel().select(this.DATA_INDEX.get(data.getClass()));
			
			// set basic settings
			this.name.setText(data.getName());
			
			this.loadAdditionalUnspecificBaseData(data);
			
			// load type specific settings
			this.activeGUIView.loadData(data.getDataToLoadOnGUI());
			this.type.getSelectionModel().select(this.activeGUIView);
			
			// load the data
			this.dataStore = data;
			this.isDataLoaded = false;
		}
	}

	/**
	 * loads additional data on the GUI that is common to all plugins of the same type 
	 */
	protected abstract void loadAdditionalUnspecificBaseData(T data);
	
	protected abstract PropertyViewType getPropertyTypeName();

	@Override
	protected boolean hasUniqueName(String name) {
		if(!XMLDataStore.hasRegistedData(name, ExecutorInfo.class))
			return true;
		else {
			this.addMessageToPrivateLog(MessageType.ERROR, "An executor property with name '"+name+"' exists already.");
			return false;
		}
	}
	
	/**
	 * adds a new factory for that type 
	 * @param f
	 */
	@SuppressWarnings("unchecked")
	public static void registerWatchdogPluginOnGUI(Class<?> c, Class<?> view) {
		// save the class for further use
		LOADED_VIEWS.put((Class<? extends XMLDataStore>) c, (Class<? extends PluginView<? extends XMLDataStore>>) view);
	}
}
