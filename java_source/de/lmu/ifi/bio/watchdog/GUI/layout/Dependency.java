package de.lmu.ifi.bio.watchdog.GUI.layout;

import org.apache.commons.lang3.tuple.Pair;

import de.lmu.ifi.bio.watchdog.GUI.WorkflowDesignController;
import de.lmu.ifi.bio.watchdog.GUI.css.CSSRessourceLoader;
import de.lmu.ifi.bio.watchdog.GUI.helper.ScreenCenteredStage;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Class, that represents a dependency between two tasks 
 * @author kluge
 *
 */
public class Dependency extends AnchorPane {
	
	private Line invisible;
	private int sX;
	private int sY;
	private int eX;
	private int eY;
	private final int X_SIZE;
	private final int Y_SIZE;
	private boolean isDelete = false;
	
	private EventHandler<? super MouseEvent> deleteCall;
	
	// seperate dependency stuff
	private String seperator;
	private Integer prefixLength;
	
	public Dependency(int startX, int startY, int endX, int endY, int xSize, int ySize, String sep, Integer length) {
		this.setPickOnBounds(false); // allow mouse events to interact with visible elements
		// seperate vars
		this.seperator = sep;
		this.prefixLength = length;

		this.sX = startX;
		this.sY = startY;
		this.eX = endX;
		this.eY = endY;
		
		this.X_SIZE = xSize;
		this.Y_SIZE = ySize;

		// create the line
		this.shift(0, 0);
	}
	
	// can be called form outside to update the stuff
	public void setSeparateVariables(String sep, Integer prefixLength) {
		this.prefixLength = prefixLength;
		this.seperator = sep;
		
		// update appearance
		this.shift(0, 0);
	}
	
	public boolean isSeparateDependency() {
		return this.seperator != null && this.prefixLength != null;
	}
	
	@Override
	public void finalize() {
		this.isDelete = true;
		try {
			super.finalize();
		} catch (Throwable e) {}
	}
	
	public boolean isDeleted() {
		return this.isDelete;
	}
	
	public void setOnLineClicked(EventHandler<? super MouseEvent> value) {
		this.deleteCall = value;
	}
	
	public void deleteAsk(MouseEvent me) {
		if(this.deleteCall != null)
			this.deleteCall.handle(me);
	}

	public String getOverallKey() {
		return RasteredGridPane.getDependencyKey(RasteredGridPane.getKey(this.sX, this.sY), RasteredGridPane.getKey(this.eX, this.eY));
	}
	
	public Pair<Integer, Integer> getFirstKey() {
		return RasteredGridPane.getPostion(RasteredGridPane.getKeys(this.getOverallKey()).getKey());
	}
	
	public Pair<Integer, Integer> getSecondKey() {
		return RasteredGridPane.getPostion(RasteredGridPane.getKeys(this.getOverallKey()).getValue());
	}
	
	private Color getDisplayColor() {
		if(this.isSeparateDependency())
			return Color.RED.deriveColor(0, 1, 1, 0.5);
		else
			return Color.GRAY.deriveColor(0, 1, 1, 0.5);
	}

	public void shift(int shiftX, int shiftY) {
		this.getChildren().clear();
		
		// update coordinates
		this.sX += shiftX;
		this.eX += shiftX;
		this.sY += shiftY;
		this.eY += shiftY;
		
		// calculate coordinates
		int csy = this.sY * this.Y_SIZE + 45;
		int cey = this.eY * this.Y_SIZE + 45;
		int csx = this.sX * this.X_SIZE + 115;
		int cex = this.eX * this.X_SIZE + 5;
		
		// line to display
		Line l = new Line(csx, csy, cex, cey);
		l.setStrokeWidth(2);
		l.setStroke(this.getDisplayColor());
		l.setStrokeLineCap(StrokeLineCap.BUTT);
		l.getStrokeDashArray().setAll(10.0, 5.0);
		l.setMouseTransparent(true);

		// line to click at
		this.invisible = new Line(csx, csy, cex, cey);
		this.invisible.setOnMouseClicked(me -> this.onClick(me));
		this.invisible.setOpacity(0);
		this.invisible.setStrokeWidth(15);		
		
		// add the lines
		this.getChildren().add(l);
		this.getChildren().add(this.invisible); 
	}

	private void onClick(MouseEvent me) {
		// no changes in read-only mode
		if(WorkflowDesignController.isInExecutionMode())
			return;

		if(!me.getButton().equals(MouseButton.PRIMARY))
			this.deleteCall.handle(me);
		else {
			try {
				// create the config
				DependencyProperty p = DependencyProperty.getModule(this);
				// show it
				Stage stage = new ScreenCenteredStage();
				stage.setTitle("Dependency properties");
				stage.setResizable(false);
				stage.initModality(Modality.APPLICATION_MODAL);
				Scene scene = new Scene(p);
				scene.getStylesheets().add(CSSRessourceLoader.getCSS("control.css"));
				stage.setScene(scene);	
				stage.showAndWait();
			}
			catch(Exception e) { e.printStackTrace(); }
		}
	}

	public String getSeparator() {
		return this.seperator;
	}

	public Integer getPrefixLength() {
		return this.prefixLength;
	}
}
