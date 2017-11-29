package de.lmu.ifi.bio.watchdog.GUI.AdditionalBar;


import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.event.StatusConsoleMessageEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class StatusConsoleController extends AdditionalBarTabController {

	@FXML private ScrollPane root;
	@FXML private VBox messages;

	private final static DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
	private String name;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.root.addEventFilter(StatusConsoleMessageEvent.NEW_MESSAGE_EVENT_TYPE, e -> this.addMessage(e.getMessageType(), e.getMessage(), e.isAutoScrollDisabled()));
		this.root.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		this.root.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
		VBox.setVgrow(this.messages, Priority.ALWAYS);
		HBox.setHgrow(this.messages, Priority.ALWAYS);	
		this.messages.getChildren().add(new Label());
	}	
	
	public void addMessage(MessageType info, String text, boolean disableAutoScroll) {
		HBox b = new HBox();
		b.setSpacing(5);
		Label t = new Label(getTime());
		Label head = new Label(info.toString());
		Label message = new Label(text);
		head.setTextFill(info.getColor());
		message.setTextFill(info.getColor());
		b.getChildren().add(t);
		b.getChildren().add(head);
		b.getChildren().add(message);
		this.messages.getChildren().add(this.messages.getChildren().size()-1, b);
		b.onMousePressedProperty().set(x -> offerCopy(x, b));
		
		if(!disableAutoScroll)
			this.root.setVvalue(1.0); // scroll to end of list
	}
	
	private void offerCopy(MouseEvent event, HBox h) {
		ContextMenu contextMenu = new ContextMenu();
		MenuItem copy = new MenuItem("copy to clipboard");
		contextMenu.getItems().add(copy);
		copy.onActionProperty().set(x -> copy2Clip(getText(h)));
		contextMenu.show(this.messages.getScene().getWindow(), event.getScreenX(), event.getScreenY());
	}

	private static void copy2Clip(String text) {
		final Clipboard clipboard = Clipboard.getSystemClipboard();
		final ClipboardContent content = new ClipboardContent();
		content.putString(text);
		clipboard.setContent(content);
	}

	private static String getText(HBox h) {
		StringBuilder bb = new StringBuilder();
		for(Node l : h.getChildrenUnmodifiable()) {
			if(l instanceof Label) {
				bb.append(((Label) l).getText());
				bb.append(" ");
			}
		}
		return bb.toString();
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getImageName() {
		return null;
	}

	@Override
	protected Node getRoot() {
		return this.root;
	}
	
	public static String getTime() {
		return "[" + TIME_FORMAT.format(Calendar.getInstance().getTime()) + "]";
	}

	public void setName(String name) {
		if(name == null)
			this.name = StatusConsole.NAME;
		else
			this.name = name;
	}
	
	public void clear() {
		this.messages.getChildren().clear();
		this.messages.getChildren().add(new Label());
	}

	public ArrayList<String> getMessages() {
		ArrayList<String> m = new ArrayList<>();
		for(Node n : this.messages.getChildren()) {
			if(n instanceof HBox) {
				HBox b = (HBox) n;
				m.add(getText(b));
			}
		}
		return m;
	}
}
