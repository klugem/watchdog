package de.lmu.ifi.bio.watchdog.GUI.AdditionalBar;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.event.TabEvent;
import de.lmu.ifi.bio.watchdog.GUI.interfaces.TabableNode;
import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import de.lmu.ifi.bio.watchdog.logger.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class AdditionalBarController implements Initializable {

	private static final String NEWLINE = System.lineSeparator();
	private final StatusConsole GLOBAL = StatusConsole.getStatusConsole(null);
	private final HashSet<StatusConsole> STATUS_CONSOLES = new HashSet<>();
	 
	@FXML protected StackPane root;
	@FXML protected TabPane tabpane;
	@FXML protected HBox buttons;
	@FXML protected Button clear;
	@FXML protected Button copy;
	@FXML protected ToggleButton lock; 

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// configure the buttons
		this.lock.setFocusTraversable(false);
		this.lock.setGraphic(ImageLoader.getImage(ImageLoader.LOCK));
		this.clear.setGraphic(ImageLoader.getImage(ImageLoader.CLEAR));
		this.copy.setGraphic(ImageLoader.getImage(ImageLoader.CLIPBOARD_SMALL));
		this.lock.onActionProperty().set(e -> this.onClickLock(e));
		this.clear.onActionProperty().set(e -> this.onClickClear(e));
		this.copy.onActionProperty().set(e -> this.onClickCopy(e));
		
		this.copy.tooltipProperty().set(new Tooltip("copy messages to clipboard"));
		this.lock.tooltipProperty().set(new Tooltip("lock autoscroll"));
		this.clear.tooltipProperty().set(new Tooltip("clear all log messages"));
		
		// grow only if required
		VBox.setVgrow(this.buttons, Priority.SOMETIMES);
		HBox.setHgrow(this.buttons, Priority.SOMETIMES);
		this.buttons.setMaxWidth(1);
		this.buttons.setMaxHeight(1);
		
		// add message console tab
		this.GLOBAL.clear();
		this.addNewTab(this.GLOBAL);
	}
	
	private void onClickCopy(ActionEvent e) {
		final Clipboard clipboard = Clipboard.getSystemClipboard();
		final ClipboardContent content = new ClipboardContent();
		content.putString(this.getText());
		clipboard.setContent(content);
	}

	private String getText() {
		StatusConsole c = (StatusConsole) this.getActiveTab().getContent();
		StringBuffer b = new StringBuffer();
		for(String l : c.getMessages()) {
			b.append(l);
			b.append(NEWLINE);
		}
		return b.toString();
	}

	private void onClickClear(ActionEvent e) {
		if(this.getActiveTab().getContent() instanceof StatusConsole) {
			StatusConsole c = (StatusConsole) this.getActiveTab().getContent();
			c.clear();
		}
		e.consume();
	}

	private void onClickLock(ActionEvent e) {
		// toogle status
		this.lock.setSelected(this.lock.isSelected());
		boolean newStatus = this.lock.isSelected();
		
		// notify all status consules
		for(StatusConsole c : this.STATUS_CONSOLES) {
			c.setAutoScrollLock(newStatus);
		}
		e.consume();
	}

	public StatusConsole getGlobalConsole() {
		return this.GLOBAL;
	}
	
	/**
	 * adds a new tab
	 * @param content
	 */
	public void addNewTab(TabableNode content) {
		// already there
		if(this.selectTab(content.getName()) != null)
			return;
		
		// auto-adapt height...
		this.tabpane.widthProperty().addListener(x -> content.setWidth(this.tabpane.getWidth()));
		this.tabpane.heightProperty().addListener(x -> content.setHeight(this.tabpane.getHeight()-35));
		// set width
		content.setWidth(this.tabpane.getWidth());
		content.setHeight(this.tabpane.getHeight()-35);
		
		// add the tab
		Tab t = new Tab();
		t.setContent(content);
		t.setText(content.getName());
		t.setGraphic(content.getImage());
		this.tabpane.getTabs().add(t);
		this.tabpane.getSelectionModel().select(t);
		
		// set current scroll status
		if(content instanceof StatusConsole) {
			((StatusConsole) content).setAutoScrollLock(this.lock.isSelected());
			this.STATUS_CONSOLES.add(((StatusConsole) content));
		}
	}

	/**
	 * sends the event to the root of the tab
	 * @param e
	 * @return
	 */
	public boolean sendEventToTab(TabEvent e) {
		String targetTab = e.getTargetTabName();
		Tab t = this.selectTab(targetTab);
		if(t != null && t.getContent() instanceof TabableNode) {
			TabableNode tn = (TabableNode) t.getContent();
			tn.fireTabEvent(e);
			return true;
		}
		return false;
	}
	
	private Tab getActiveTab() {
		return this.tabpane.getSelectionModel().getSelectedItem();
	}
	
	/**
	 * returns the tab with that name or null if tab was not found
	 * @param tabName
	 * @return
	 */
	private Tab selectTab(String tabName) {
		for(Tab t : this.tabpane.getTabs()) {
			if(tabName.equals(t.getText()))
				return t;
		}
		return null;
	}
	
	/**
	 * remove a tab with that name
	 * @param tabName
	 */
	public boolean removeTab(String tabName) {
		return this.removeTab(tabName, false);
	}

	private boolean removeTab(String tabName, boolean doNotRemove) {
		Tab t = this.selectTab(tabName);
		if(t != null) {
			// free memory of log messages
			if(t.getContent() instanceof StatusConsole) {
				((StatusConsole) t.getContent()).clear();
				// remove then handlers to let the GC eat all stuff
				ArrayList<LogMessageEventHandler> handlers = ((StatusConsole) t.getContent()).getMessageHandlers();
				for(LogMessageEventHandler h : handlers)
					Logger.unregisterListener(h);
				handlers.clear();
			}
			if(!doNotRemove)
				return this.tabpane.getTabs().remove(t);
			else
				return true;
		}
		return false;
	}

	/**
	 * resets the bars
	 */
	public void clear() {
		for(Iterator<Tab> it = this.tabpane.getTabs().iterator(); it.hasNext(); ) {
			Tab t = it.next();
			this.removeTab(t.getText(), true);
			it.remove();
		}
		// add default status tab
		this.initialize(null, null);
	}

	public void hideGlobalConsole(boolean hide) {
		if(hide)
			this.removeTab(this.GLOBAL.getName());
		else
			this.addNewTab(this.GLOBAL);
	}
}
