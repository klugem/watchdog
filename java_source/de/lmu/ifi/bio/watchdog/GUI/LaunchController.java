package de.lmu.ifi.bio.watchdog.GUI;

import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ProgressBar;

public class LaunchController implements Initializable, Runnable {

	@FXML private ProgressBar loading;

	@Override
	public void initialize(URL location, ResourceBundle resources) { 
	}

	@Override
	public void run() {
		Random rand = new Random();
		double i = 0;
		double step = 0.05;
		int wait = 150;
		while(i < 1) {
			i = i + step;
			this.loading.setProgress(i);
			try { Thread.sleep(wait + (rand.nextInt(300) - 150)); } catch(Exception e) {}
		}
		this.loading.setProgress(1);
	}
}
