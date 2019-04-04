package de.lmu.ifi.bio.watchdog.GUI.png;

import java.net.URL;

import javafx.scene.image.ImageView;

/**
 * Loads png files stored in this packge
 * @author kluge
 *
 */
public class ImageLoader {

	/** icons 24 x 24 or small 16 x 16 */
	public static final String NEW = "new.png";
	public static final String NEW_SMALL = "new_small.png";
	public static final String ADD = "add.png";
	public static final String ADD_SMALL = "add_small.png";
	public static final String CONFIG = "config.png";
	public static final String CONFIG_SMALL = "config_small.png";
	public static final String DELETE = "delete.png";
	public static final String DELETE_SMALL = "delete_small.png";
	public static final String SAVE_AS = "saveAs.png";
	public static final String SAVE_AS_SMALL = "saveAs_small.png";
	public static final String SAVE = "save.png";
	public static final String SAVE_SMALL = "save_small.png";
	public static final String OPEN = "open.png";
	public static final String OPEN_SMALL = "open_small.png";
	public static final String RECENT = "recent.png";
	public static final String RECENT_SMALL = "recent_small.png";
	public static final String ABOUT_SMALL = "about_small.png";
	public static final String VERIFY_SMALL = "verify_small.png";
	public static final String RUN_SMALL = "run_small.png";
	public static final String RESIZE_SMALL = "resize_small.png";
	public static final String FILTER_SMALL = "filter_small.png";
	public static final String WATCHDOG_ICON_SMALL = "watchdog_icon_small.png";
	public static final String ZOOM_SMALL = "zoom_small.png";
	public static final String ZOOM = "zoom.png";
	public static final String BLOCK = "block.png";
	public static final String BLOCK_SMALL = "block_small.png";
	public static final String CLIPBOARD_SMALL = "clipboard_small.png";
	
	/** from eclipse --- http://eclipse-icons.i24.cc/eclipse-icons-10.html */
	public static final String PAUSE_SMALL = "pause_small.gif";
	public static final String RESUME_SMALL = "resum_small.gif";
	public static final String STOP_SMALL = "stop_small.gif";
	public static final String DETACH_SMALL = "term_restart_small.gif";
	public static final String LOCK = "lock.gif";
	public static final String CLEAR = "clear.gif";
	
	public static final String GREEN = "lights_green.png";
	public static final String YELLOW = "lights_yellow.png";
	public static final String RED = "lights_red.png";
	
	public static final String WATCHDOG = "watchdog.png";
	public static final String TITLE = "title.png";
	
	// status updates for execute
	public static final String FAILED = "failed.png";
	public static final String QUEUE = "queue.png";
	public static final String RUNNING = "running.png";
	public static final String FINISHED = "finished.png";

	public static URL getURL(String name) {
		return ImageLoader.class.getResource(name);
	}

	public static ImageView getImage(String name) {
		if(name == null)
			return null;
		
		URL url = getURL(name);
		return (url == null ? null : new ImageView(url.toString()));
	}
}
