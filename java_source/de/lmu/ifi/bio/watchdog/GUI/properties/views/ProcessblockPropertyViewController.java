package de.lmu.ifi.bio.watchdog.GUI.properties.views;


import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.MessageType;
import de.lmu.ifi.bio.watchdog.GUI.helper.SuggestPopup;
import de.lmu.ifi.bio.watchdog.GUI.helper.TextFilter;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import de.lmu.ifi.bio.watchdog.helper.ProcessBlock.ProcessBlock;
import de.lmu.ifi.bio.watchdog.helper.ProcessBlock.ProcessFolder;
import de.lmu.ifi.bio.watchdog.helper.ProcessBlock.ProcessInput;
import de.lmu.ifi.bio.watchdog.helper.ProcessBlock.ProcessSequence;
import de.lmu.ifi.bio.watchdog.helper.ProcessBlock.ProcessTable;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;

public class ProcessblockPropertyViewController extends PropertyViewController {

	private enum Types {
		SEQUENCE, FOLDER, TABLE, INPUT;
		
		public boolean isSequence() { return this.equals(SEQUENCE); }
		public boolean isFolder() { return this.equals(FOLDER); }
		public boolean isTable() { return this.equals(TABLE); }
		public boolean isInpute() { return this.equals(INPUT); }
	}
	
	@FXML private TextField name;
	@FXML private TabPane selectType;
	@FXML private Tab tabSequence;
	@FXML private Tab tabFolder;
	@FXML private Tab tabTable;
	@FXML private Tab tabInput;
	
	// sequence process block
	@FXML private TextField s_start;
	@FXML private TextField s_end;
	@FXML private TextField s_step;
	@FXML private CheckBox s_append;
	// folder process block
	@FXML private TextField f_folder;
	@FXML private TextField f_pattern;
	@FXML private TextField f_ignore;
	@FXML private CheckBox f_append;
	@FXML private CheckBox f_enforce;
	@FXML private TextField f_maxDepth;
	// table process block
	@FXML private TextField t_path;
	@FXML private TextField t_compare;
	@FXML private CheckBox t_enforce;
	// input process block
	@FXML private TextField i_seperator;
	@FXML private TextField i_compare;
	
	private ProcessBlock processblockStore;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);
		
		// add event handler on change type
		this.selectType.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) -> this.validate(this.getActiveType(this.selectType.getTabs().get(newIndex.intValue())).toString()));
		
		// add double or integer enforcer
		this.s_start.setTextFormatter(TextFilter.getDoubleFormater());
		this.s_end.setTextFormatter(TextFilter.getDoubleFormater());
		this.s_step.setTextFormatter(TextFilter.getDoubleFormater());
		this.f_maxDepth.setTextFormatter(TextFilter.getPositiveIntFormater());
		
		// add checker
		this.addValidateToControl(this.name, "name", f -> this.checkName((TextField) f));
		// for process sequence
		this.addValidateToControl(this.s_start, "s_start", f -> this.isDouble((TextField) f, "Start of process sequence must be a valid double value. (f.e. 5.0)"), Types.SEQUENCE.toString());
		this.addValidateToControl(this.s_end,  "s_end",f -> this.isDouble((TextField) f, "End of process sequence must be a valid double value. (f.e. 9.0)"), Types.SEQUENCE.toString());
		// for process folder
		this.addValidateToControl(this.f_pattern, "f_pattern", f -> !this.isEmpty((TextField) f, "A file pattern must be given. (f.e *.txt)"), Types.FOLDER.toString());
		this.addValidateToControl(this.f_folder, "f_folder", f -> this.isAbsoluteFolder((TextField) f, "A folder to search for files matching the pattern must be given. (f.e. /tmp/)"), Types.FOLDER.toString());
		// for process table
		this.addValidateToControl(this.t_path, "t_path", f -> this.isAbsoluteFile((TextField) f, "A path to a file in csv-format must be given. (f.e *.txt)"), Types.TABLE.toString());

		// add event handler for GUI validation
		this.name.textProperty().addListener(event -> this.validate());
		this.s_start.textProperty().addListener(event -> this.validate());
		this.s_end.textProperty().addListener(event -> this.validate());
		this.f_folder.textProperty().addListener(event -> this.validate());
		this.f_pattern.textProperty().addListener(event -> this.validate());
		this.t_path.textProperty().addListener(event -> this.validate());
		this.i_seperator.textProperty().addListener(event -> this.validate());
		this.f_append.selectedProperty().addListener(event -> this.validate());
		this.s_append.selectedProperty().addListener(event -> this.validate());
		
		// add suggest constants support
		@SuppressWarnings("unused") SuggestPopup p1 = new SuggestPopup(this.t_path);
		@SuppressWarnings("unused") SuggestPopup p2 = new SuggestPopup(this.f_folder);
		
		// call it once to get initial coloring
		this.validate();
	}
	
	@Override
	protected boolean validate() {
		return this.validate(this.getActiveType().toString());
	}
	
	private boolean checkName(TextField f) {
		if(this.isEmpty((TextField) f, "Name for process block is missing."))
			return false;
		if(!this.hasUniqueName(((TextField) f).getText()))
			return false;
		// all was ok!
		return true;
	}

	public ProcessBlock getStoredData() {
		return this.processblockStore;
	}
	
	private Types getActiveType() {
		return this.getActiveType(this.selectType.getSelectionModel().getSelectedItem());
	}
	
	private Types getActiveType(Tab selected) {		
		if(this.tabFolder.equals(selected))
			return Types.FOLDER;
		else if(this.tabSequence.equals(selected))
			return Types.SEQUENCE;
		else if(this.tabInput.equals(selected))
			return Types.INPUT;
		else if(this.tabTable.equals(selected))
			return Types.TABLE;
		else
			return null;
	}
	

	@Override
	protected void saveData() {
		String name = this.name.getText();
		ProcessBlock b = null;
		Types active = this.getActiveType();
		if(active.isSequence()) {
			Double step = 1.0;
			double start = Double.parseDouble(this.s_start.getText());
			double end = Double.parseDouble(this.s_end.getText());
			try { step = Double.parseDouble(this.s_step.getText()); } catch(Exception e) {}
			boolean append = this.s_append.isSelected();			
			b = new ProcessSequence(name, start, end, step, append);
		}
		else if(active.isFolder()) {
			String rootPath = this.f_folder.getText();
			String pattern = this.f_pattern.getText();
			String ignore = this.f_ignore.getText();
			int maxDepth = Integer.parseInt(this.f_maxDepth.getText());
			boolean disableExistanceCheck = !this.f_enforce.isSelected();
			boolean append = this.f_append.isSelected();
			b = new ProcessFolder(name, rootPath, null, pattern, ignore, maxDepth, append, disableExistanceCheck);
		}
		else if(active.isTable()) {
			String table = this.t_path.getText();
			String compareColum = this.t_compare.getText();
			boolean disableExistanceCheck = !this.t_enforce.isSelected();
			b = new ProcessTable(name, table, null, compareColum, disableExistanceCheck);
		}
		else if (active.isInpute()) {
			String seperator = this.i_seperator.getText();
			String compareName = this.i_compare.getText();
			
			b = new ProcessInput(name, seperator, compareName);
		}
		
		// save the process block
		this.storeXMLData(b);
		super.saveData();
	}
	
	protected PropertyViewType getPropertyTypeName() {
		return PropertyViewType.PROCESS_BLOCK;
	}
	
	private void storeXMLData(ProcessBlock data) {
		this.processblockStore = data;
		XMLDataStore.registerData(data);
	}

	public void loadData(ProcessBlock data) {
		if(data != null) {
			// unregister that data or otherwise name will be blocked!
			XMLDataStore.unregisterData(data);
			this.isDataLoaded = true;
						 
			// switch types
			if(data instanceof ProcessSequence) {
				ProcessSequence s = (ProcessSequence) data;
				this.s_append.setSelected(s.gui_append);
				this.s_end.setText(Double.toString(s.gui_end));
				this.s_start.setText(Double.toString(s.gui_start));
				this.s_step.setText(Double.toString(s.gui_step));
				this.selectType.getSelectionModel().select(this.tabSequence);
			}
			else if(data instanceof ProcessFolder) {
				ProcessFolder f = (ProcessFolder) data;
				this.f_append.setSelected(f.gui_append);
				this.f_enforce.setSelected(!f.gui_disableExistanceCheck);
				this.f_folder.setText(f.gui_rootPath);
				this.f_ignore.setText(f.gui_ignorePattern);
				this.f_maxDepth.setText(Integer.toString(f.gui_maxDepth));
				this.f_pattern.setText(f.gui_pattern);
				this.selectType.getSelectionModel().select(this.tabFolder);
			}
			else if(data instanceof ProcessTable) {
				ProcessTable t = (ProcessTable) data;
				this.t_enforce.setSelected(!t.gui_disableExistanceCheck);
				this.t_compare.setText(t.gui_compareColum);
				this.t_path.setText(t.gui_table);
				this.selectType.getSelectionModel().select(this.tabTable);
			}
			else if(data instanceof ProcessInput) {
				ProcessInput i = (ProcessInput) data;
				this.i_seperator.setText(i.getGlobalSep());
				this.i_compare.setText(i.getReplaceDefaultGroup());
				this.selectType.getSelectionModel().select(this.tabInput);
			}
			
			// set basic settings
			this.name.setText(data.getName());
			
			// load the data
			this.processblockStore = data;
			this.isDataLoaded = false;
		}
	}

	@Override
	protected boolean hasUniqueName(String name) {
		Types active = this.getActiveType();
		// look for append
		if(active.isFolder() || active.isSequence()) {
			if(active.isFolder() && this.f_append.isSelected())
				return true;
			if(active.isSequence() && this.s_append.isSelected())
				return true;
		}
		if(!XMLDataStore.hasRegistedData(name, ProcessBlock.class))
			return true;
		else {
			this.addMessageToPrivateLog(MessageType.ERROR, "An process block with name '"+name+"' exists already.");
			return false;
		}
	}
}
