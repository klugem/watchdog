package de.lmu.ifi.bio.watchdog.GUI.helper;

import java.net.URL;
import java.util.ResourceBundle;

import de.lmu.ifi.bio.watchdog.GUI.WorkflowDesignController;
import de.lmu.ifi.bio.watchdog.GUI.png.ImageLoader;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;

/**
 * Local executor has no additional settings --> nothing to do here
 * @author kluge
 *
 */
public class ExecuteToolbarController implements Initializable {

	@FXML private Button run;
	@FXML private Button pause;
	@FXML private Button detach;
	@FXML private Button stop;
	@FXML private ToolBar toolbar;
	
	private WorkflowDesignController mainController;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.run.setGraphic(ImageLoader.getImage(ImageLoader.RUN_SMALL));
		this.pause.setGraphic(ImageLoader.getImage(ImageLoader.PAUSE_SMALL));
		this.stop.setGraphic(ImageLoader.getImage(ImageLoader.STOP_SMALL));
		this.detach.setGraphic(ImageLoader.getImage(ImageLoader.DETACH_SMALL));
		
		this.run.setOnMouseClicked(x -> this.runWorkflow());
		this.stop.setOnMouseClicked(x -> this.stopWorkflow());
		this.pause.setOnMouseClicked(x -> this.pauseWorkflow());
		this.detach.setOnMouseClicked(x -> this.requestDetach());
	}
	
	protected void stopWorkflow() {
		if(this.mainController.stopWorkflow()) {
			this.setIsFinished();
		}
	}
	
	private void pauseWorkflow() {
		boolean paused = this.mainController.isProcessingPaused();
		this.pauseWorkflow(paused);
	}
	
	private void requestDetach() {
		this.mainController.requestDetach();
		this.detach.setDisable(true);
	}

	public void pauseWorkflow(boolean paused) {
		if(paused) {
			if(this.mainController.setPauseWorkflow(false))
				this.pause.setGraphic(ImageLoader.getImage(ImageLoader.PAUSE_SMALL));
				this.pause.setText("pause scheduling");
		}
		else {
			if(this.mainController.setPauseWorkflow(true))
				this.pause.setGraphic(ImageLoader.getImage(ImageLoader.RESUME_SMALL));
				this.pause.setText("resume scheduling");
		}
	}

	private void runWorkflow() {
		if(this.mainController.runWorkflow()) {
			this.run.setDisable(true);
			this.stop.setDisable(false);
			this.pause.setDisable(false);
			this.detach.setDisable(false);
		}
	}

	public void setDesignControlller(WorkflowDesignController controller) {
		this.mainController = controller;
	}

	public void setIsFinished() {
		this.run.setDisable(false);
		this.stop.setDisable(true);
		this.pause.setDisable(true);
		this.detach.setDisable(true);
		
		// update value
		Platform.runLater(() -> this.pauseWorkflow(true));
	}
}