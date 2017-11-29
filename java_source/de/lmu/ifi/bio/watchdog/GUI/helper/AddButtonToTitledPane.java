package de.lmu.ifi.bio.watchdog.GUI.helper;

import java.util.HashSet;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class AddButtonToTitledPane {
	
	private static final HashSet<EventHandler<?>> ADD_BUTTONS = new HashSet<>();
	
	/**
	 * must be externally called when the GUI is loaded
	 */
	public synchronized static void initAddButtonsAsGUIisLoaded() {
		for(EventHandler<?> e : ADD_BUTTONS) 
			e.handle(null);
		ADD_BUTTONS.clear();
	}

	public synchronized static void registerAddImageCall(EventHandler<?> e) {
		ADD_BUTTONS.add(e);
	}
	
	public static void addImage(TitledPane pane, Button b, ImageView image, EventHandler<ActionEvent> onButtonClick, boolean positionRight) {
		// process only stuff, that is actually shown on the screen.
		if(pane.getScene() == null)
			return;
		
		// set image and event handler
		b.setGraphic(image);
		if(onButtonClick != null) 
			b.setOnAction(onButtonClick);
		
		// add it
		pane.setGraphic(b);
		pane.setContentDisplay(ContentDisplay.RIGHT);
		
		// calculate distance
        Node titleRegion = pane.lookup(".title");
        Insets padding = ((StackPane) titleRegion).getPadding();
        double graphicWidth = image.getLayoutBounds().getWidth();
        double arrowWidth = titleRegion.lookup(".arrow-button").getLayoutBounds().getWidth();
        double labelWidth = titleRegion.lookup(".text").getLayoutBounds().getWidth();
        double nodesWidth = graphicWidth + padding.getLeft() + padding.getRight() + arrowWidth + labelWidth;  

        // set the distance
        if(positionRight) 
        	pane.graphicTextGapProperty().set(pane.widthProperty().doubleValue() - nodesWidth);
	}
}
