package de.lmu.ifi.bio.watchdog.GUI.helper;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import de.lmu.ifi.bio.multithreading.StopableLoopRunnable;
import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.MessageType;
import de.lmu.ifi.bio.watchdog.GUI.AdditionalBar.StatusConsole;
import de.lmu.ifi.bio.watchdog.executor.WatchdogThread;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Local executor has no additional settings --> nothing to do here
 * @author kluge
 *
 */
public class LogViewController implements Initializable {

	@FXML private TextField filename;
	@FXML private ChoiceBox<String> frequency;
	@FXML private CheckBox tail;
	@FXML private ScrollPane scroll;
	@FXML private BorderPane root;
	@FXML private TextFlow content;
	@FXML private Button refresh;
	
	private boolean showTail = true;
	private int reloadTime = 15;
	private File file2Display;
	private UpdateFile updateThread = null;
	
	private static LinkedHashMap<String, Integer> WAIT_TIMES = new LinkedHashMap<>();
	static {
		// add some pre-defined values
		for(int i = 1; i <5; i++) {
			WAIT_TIMES.put(i + "s", i);
		}
		for(int i = 1; i <=6; i++) {
			WAIT_TIMES.put(i*5 + "s", i*5);
		}
		for(int i = 3; i <9; i++) {
			WAIT_TIMES.put(i*15 + "s", i*15);
		}
	}
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.filename.setDisable(true);
		
		Platform.runLater(() -> this.root.prefHeightProperty().bind(this.root.getScene().heightProperty()));
		Platform.runLater(() -> this.root.prefWidthProperty().bind(this.root.getScene().widthProperty()));
		
		// init the combo box
		this.frequency.getItems().addAll(WAIT_TIMES.keySet());
		this.frequency.getSelectionModel().select(6); // select 15s
		this.tail.setSelected(true);
		
		// add change events
		this.frequency.getSelectionModel().selectedItemProperty().addListener(e -> this.onChangeUpdateIntervall());
		this.tail.selectedProperty().addListener(e -> this.onChangeShowTail());
		this.refresh.setOnAction(e -> this.updateThread.update());
	}
	
	private void onChangeShowTail() {
		boolean old = this.showTail;
		this.showTail = this.tail.isSelected();
		if(old != showTail) {
			this.startNewUpdateThread();
		}
	}
	
	private void onChangeUpdateIntervall() {
		String key = this.frequency.getSelectionModel().getSelectedItem();
		if(key != null && WAIT_TIMES.containsKey(key)) {
			this.reloadTime = WAIT_TIMES.get(key);
			this.startNewUpdateThread();
		}
	}

	public void setFile(File file) {
		this.file2Display = file;
		this.filename.setText(file.getAbsolutePath());
		// start thread that will update the content
		this.startNewUpdateThread();		
	}
	
	private void startNewUpdateThread() {
		// stop old thread if one is running
		if(this.updateThread != null) {
			this.updateThread.requestStop(500, TimeUnit.MILLISECONDS);
			this.updateThread = null;
		}
		// start a new one
		this.updateThread = new UpdateFile(this.file2Display, this.reloadTime, this.showTail, this.content, this.scroll);
		WatchdogThread.addUpdateThreadtoQue(this.updateThread, false);
	}
	
	
	// file update thread 
	private class UpdateFile extends StopableLoopRunnable {
		
		private final File FILE;
		private final int RELOAD_TIME;
		private final boolean SHOW_TAIL;
		private final TextFlow CONTENT;
		private final ScrollPane SCROLL;
		private final String NEWLINE = System.lineSeparator();
		
		public UpdateFile(File f, int reloadTime, boolean showTail, TextFlow text, ScrollPane scrollPane) {
			super("UpdateFile");
			this.FILE = f;
			this.RELOAD_TIME = Math.max(1, reloadTime) * 1000;
			this.SHOW_TAIL = showTail;
			this.CONTENT = content;
			this.SCROLL = scrollPane;
		}
				
		public void update() {
			try {
				if(this.FILE.exists() && this.FILE.canRead() && this.FILE.isFile()) {
					List<String> lines = Files.readAllLines(this.FILE.toPath());
					Platform.runLater(() -> this.setText(lines));
				}
			}
			catch(Exception e) { Platform.runLater(() -> StatusConsole.addGlobalMessage(MessageType.ERROR, "Failed to load content of log file '"+this.FILE.getAbsolutePath()+"'.")); e.printStackTrace();}
		}
		
		private void setText(List<String> lines) {
			// calculate old line position
			long numberOfLines = this.CONTENT.getChildren().size();
			double pos = this.SCROLL.getVvalue();
			long oldScrollPosLine = (long) (numberOfLines * pos);
			
			// add new content
			this.CONTENT.getChildren().clear();
			for(String l : lines) {
				this.CONTENT.getChildren().add(new Text(l + NEWLINE));
			}
			
			// scroll to end
			if(this.SHOW_TAIL) {
				this.SCROLL.setVvalue(1.0);
			}
			// try to scroll to the old "line"
			else {
				numberOfLines = lines.size();
				double newPos = ((double) oldScrollPosLine / (double) numberOfLines);
				this.SCROLL.setVvalue(newPos);
			}
		}

		@Override
		public int executeLoop() throws InterruptedException {
			if(this.FILE == null) {
				this.requestStop(1, TimeUnit.SECONDS);
				return 0;
			}
			// read the content of the file
			this.update();
			return 0;
		}

		@Override
		public void afterLoop() {}

		@Override
		public long getDefaultWaitTime() {
			return this.RELOAD_TIME;
		}

		@Override
		public void beforeLoop() {
			
		}

		@Override
		public boolean canBeStoppedForRestart() {
			return false;
		}
	}
}
