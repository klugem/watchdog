package de.lmu.ifi.bio.watchdog.GUI.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.bio.watchdog.GUI.WorkflowDesignController;
import de.lmu.ifi.bio.watchdog.GUI.layout.RasteredGridPane;
import de.lmu.ifi.bio.watchdog.helper.Constants;
import de.lmu.ifi.bio.watchdog.helper.ReplaceSpecialConstructs;
import de.lmu.ifi.bio.watchdog.helper.XMLDataStore;
import de.lmu.ifi.bio.watchdog.processblocks.ProcessBlock;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.stage.Popup;

public class SuggestPopup {
	
	private static final String REG_REPLACE_ENDING = "<<%-~-~-%>>";
	private boolean duringChangeSuggest = false;
	private final TextField T;
	private final ProcessBlock B;
	private final String KEY;
	private final RasteredGridPane GRID;
	
	public SuggestPopup(TextField t) {
		this.T = t;
		this.KEY = null;
		this.B = null;
		this.GRID = null;
		this.T.focusedProperty().addListener(ev -> this.textLoseFocus());
		this.T.textProperty().addListener(ev -> this.suggestConstants());
	}
	
	public SuggestPopup(TextField t, RasteredGridPane grid, ProcessBlock b, String key, boolean showTmpVarForTasks) {
		this.T = t;
		this.B = b;
		this.KEY = key;
		this.GRID = grid;
		this.T.focusedProperty().addListener(ev -> this.textLoseFocus());
		this.T.textProperty().addListener(ev -> this.suggestVars(this.GRID, this.B, this.KEY, showTmpVarForTasks));
	}
	
	/**
	 * is called when the popup is closed
	 * @param t
	 * @param p
	 */
	public void closePopup(Popup p) {
		p.hide();
		p.setUserData(null);
		this.T.setUserData(null);
		this.duringChangeSuggest = false;
	}
	
	/**
	 * close popup
	 */
	private void textLoseFocus() {
		if(this.T.getUserData() != null && this.T.getUserData() instanceof Object[]) 
			this.closePopup((Popup) ((Object[]) this.T.getUserData())[0]);
	}
	
	/**
	 * suggests some constants
	 * @param t
	 */
	private void suggestConstants() {
		this.suggestVars(null, null, null, false);
	}
	
	/**
	 * open the variable suggest menu
	 * @param t
	 * @param w
	 */
	private void suggestVars(RasteredGridPane grid, ProcessBlock b, String key, boolean showTmpVariableForTasks) {
		if(this.duringChangeSuggest)
			return;
		
		String text = this.T.getText();
		if(text == null)
			return;
		int selEnd = Math.min(text.length(), this.T.getSelection().getEnd()+1);
		
		// test if last entered char is [/{/( -->  get return variables or process block info
		String filter = "";
		if(selEnd >= 1)
			filter = text.substring(selEnd-1, selEnd);

		if(filter.matches("[\\[\\{\\(]") && (selEnd-2 < 0 || text.charAt(selEnd-2) != '$')) {
			// offer some variables if a process block is set
			if(b != null) {
				ArrayList<String> availParams = new ArrayList<>();
				if(grid != null && key != null)
					availParams = grid.getAvailReturnParams(key);
				else
					this.getChoisePopup(b.getOfferedVariables(availParams), false);
			}
		}
		// get constants
		else {
			/*******************************************************/
			Pattern p = Pattern.compile("[^\\(\\[\\{]?\\$(\\{([^\\}]*))?");
			Matcher m = p.matcher(text.substring(0, selEnd));
			boolean foundConst = false;
			filter = null;
			while(m.find()) {
				foundConst = false;
				String match = m.group();
				int i = text.indexOf(match);
				i += match.length();
				
				// ensure that it is no closed tag
				if(text.length() > i && text.charAt(i) == '}')
					continue;
				
				foundConst = true;
				filter = m.group(2);
			}
			if(foundConst) {
				HashMap<String, String> dd = new HashMap<>();
				// filter the data
				ArrayList<XMLDataStore> consts = WorkflowDesignController.getConstManager().getXMLData();
				if(showTmpVariableForTasks) {
					//String workingDir = XMLTask.hasXMLTask(task.getTaskID()) ? XMLTask.getXMLTask(task.getTaskID()).getExecutor().getWorkingDir() : "";
					consts.add(new Constants(XMLParser.TMP_BLOCKED_CONST, ""));
					consts.add(new Constants(XMLParser.WF_PARENT_BLOCKED_CONST, ""));
				}
				for(XMLDataStore d : consts) {
					if(filter == null)
						dd.put(d.getName(), ((Constants) d).getValue());
					else if(d.getName().startsWith(filter))
						dd.put(d.getName(), ((Constants) d).getValue());
				}
				if(dd.size() > 0)
					this.getChoisePopup(dd, true);
				// close open one
				else {
					if(this.T.getUserData() != null && this.T.getUserData() instanceof Object[])
						this.closePopup((Popup) ((Object[]) this.T.getUserData())[0]);
				}
			}
		}
		/*******************************************************/
	}
	

	
	@SuppressWarnings("unchecked")
	private void getChoisePopup(HashMap<String, String> data, boolean constant) {
		Bounds b = this.T.localToScreen(this.T.getBoundsInLocal());
		// show it only when stuff is loaded
		if(b == null)
			return;
		ObservableList<Label> o = FXCollections.observableList(new ArrayList<>());
		boolean newOne = true;
		 
		// test if there is already an popup
		if(this.T.getUserData() != null && this.T.getUserData() instanceof Object[]) {
			Popup popup = (Popup) ((Object[]) this.T.getUserData())[0];
			// create new one
			if(((boolean) ((Object[]) this.T.getUserData())[1]) != constant) {
				this.closePopup(popup);	
			}
			else {
				o = (ObservableList<Label>) popup.getUserData();
				o.clear();
				newOne = false;
			}
		}
		if(newOne) {
			// show it only if some values are there
			if(data.size() > 0) {
				// create popup
				Popup pop = new Popup();
				Pane p = new Pane();
				pop.getContent().add(p);
			
				// set position and size
				pop.setX(b.getMinX());
				pop.setY(b.getMaxY()+ 5);		
		
				ListView<Label> l = new ListView<>(o);
				p.getChildren().add(l);
				// set size of popup
				l.setMinWidth(300);
				l.setMaxWidth(300);
				l.setMinHeight(150);
				l.setMaxHeight(150);
				
				// set event handler
				l.setOnMouseClicked(m -> this.selectProperty(l, pop, constant));
				l.setOnKeyPressed(ev -> { if(KeyCode.ENTER.equals(ev.getCode())) this.selectProperty(l, pop, constant); });
				
				// show the popup
				this.T.setUserData(new Object[] { pop, constant });
				pop.show(this.T.getScene().getWindow());
				pop.setUserData(o);
			}
		}
		// set the values
		for(String name : data.keySet()) {
			Label ll = new Label(name + " (" + data.get(name) +")");
			ll.setMinWidth(275);
			ll.setTooltip(new Tooltip(data.get(name)));
			o.add(ll);
		}
	}

	/**
	 * is called when the user selects an entry on the list
	 * @param t
	 * @param l
	 * @param pop
	 */
	private void selectProperty(ListView<Label> l, Popup pop, boolean constant) {	
		this.duringChangeSuggest = true;
		if(l.getSelectionModel().getSelectedItem() != null) {
			// set the text
			String text = this.T.getText();
			Label label = ((Label) l.getSelectionModel().getSelectedItem());
			String v = label.getText().replace(" (" + label.getTooltip().getText() + ")", "");
			// replace constant
			if(constant) {
				int cursorEnd = this.T.getSelection().getEnd();
				String first = text.substring(0, cursorEnd) + REG_REPLACE_ENDING;
				String textNew = first.replaceAll("\\$\\{?(\\{[^\\}]*)?(\\{[^\\}]+\\})?" + REG_REPLACE_ENDING + "$", "\\${" + v + "}");
				int pos = textNew.length();
				if(text.length() > cursorEnd)
					textNew = textNew + text.substring(cursorEnd);
				
				if(!text.equals(textNew)) {
					this.T.setText(textNew);
					this.T.positionCaret(pos);
				}
			}
			// replace process block
			else {
				int cursorEnd = this.T.getSelection().getEnd();
				if(cursorEnd < 1 || cursorEnd > this.T.getText().length()) {
					if(this.T.getText().length() < 1) {
						this.T.setText(v);
					}
				}
				else {
					String newT = text.substring(0, cursorEnd-1) + v;
					this.T.setText(newT + text.substring(cursorEnd));
					this.T.positionCaret(newT.length());
				}
			}	
		}
		this.closePopup(pop);
	}
}
