package de.lmu.ifi.bio.watchdog.GUI;

import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import de.lmu.ifi.bio.multithreading.TimedExecution;
import de.lmu.ifi.bio.watchdog.GUI.layout.RasteredGridPane;
import de.lmu.ifi.bio.watchdog.GUI.module.WorkflowModule;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;

public class WorkflowController implements Initializable {
		
	@FXML private ScrollPane scrollPane;
	@FXML private RasteredGridPane workflowPane;
	private boolean ignoreSizeChange = false;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		WorkflowDesignerRunner.addListenerOnLoadReady((object, oldV, newV) -> this.initAdaptCalls());
	}
	
	private void initAdaptCalls() {
		this.scrollPane.layoutBoundsProperty().addListener(e -> this.adaptGridRequest());
		this.adaptGridRequest();
	}

	private  void adaptGridRequest() {
		if(!this.ignoreSizeChange) {
			// create a new resize request
			TimedExecution.addRunableNamed(() -> this.adaptGrid(), 1, TimeUnit.SECONDS, "adaptGridRequest");
		}
	}
	
	protected void adaptGrid() {
		this.workflowPane.adjustGridSize(this.scrollPane.getLayoutBounds());
	}

	/**
	 * inits the grid
	 * @param xSize
	 * @param ySize
	 * @param xNumber 
	 * @param yNumber
	 */
	public void initGrid(int xSize, int ySize, int maxExtendDist) {
		this.workflowPane.initGrid(xSize, ySize, maxExtendDist);
	}
	
	/**
	 * returns all modules that are currently active in the GRID
	 * @return
	 */
	public HashMap<String, WorkflowModule> getActiveModules() {
		return this.workflowPane.getActiveModules();
	}

	/**
	 * deletes all stuff
	 */
	public void clear() {
		this.workflowPane.clear();
	}
	
	public void setWidth(double w) {
		this.workflowPane.setPrefWidth(w);
	}
	public void setHeight(double w) {
		this.workflowPane.setPrefHeight(w);
	}

	public RasteredGridPane getGrid() {
		return this.workflowPane;
	}

	public void validateAllModules() {
		for(WorkflowModule m : this.getActiveModules().values())
			m.setLoadedParameters(null);
	}

	public void ignoreSizeChange(boolean ignore) {
		this.ignoreSizeChange = ignore;
	}
}