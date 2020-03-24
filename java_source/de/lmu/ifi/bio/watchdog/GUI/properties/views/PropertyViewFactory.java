package de.lmu.ifi.bio.watchdog.GUI.properties.views;

import de.lmu.ifi.bio.watchdog.GUI.properties.PropertyData;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.executor.ExecutorPropertyView;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.processblocks.ProcessblockPropertyView;
import de.lmu.ifi.bio.watchdog.GUI.properties.views.wrapper.ExecutionWrapperPropertyView;
import javafx.scene.paint.Color;

public class PropertyViewFactory {
	
	private final PropertyViewType TYPE;
	
	public PropertyViewFactory(PropertyViewType type) {
		this.TYPE = type;
	}
	
	public PropertyView getView(PropertyData data) throws Exception {
		return getPropertyView(this.TYPE);
	}
	
	public PropertyViewType getType() {
		return this.TYPE;
	}

	public static PropertyView getPropertyView(PropertyViewType type) throws Exception {
		PropertyView view = null;
		switch (type) {
		case ENVIRONMENT:
			view = EnvironmentPropertyView.getEnvironmentPropertyView();
			break;
		case PROCESS_BLOCK:
			view = ProcessblockPropertyView.getProcessblockPropertyView();
			break;
		case EXECUTOR:
			view = ExecutorPropertyView.getExecutorPropertyView();
			break;
		case CONSTANTS:
			view = ConstantsPropertyView.getConstantsPropertyView();
			break;
		case WRAPPERS:
			view = ExecutionWrapperPropertyView.getExecutionWrapperPropertyView();
			break;
		default:
			throw new Exception("PropertyViewFactory does not accept type " + type + " yet!");
		}
		return view;
	}

	public PropertyData getPropertyData(Color color, Integer number) {
		PropertyData data = null;
		switch (this.TYPE) {
		case CONSTANTS:
		case WRAPPERS:
			data = new PropertyData(number);
			break;
		default:
			data = new PropertyData(color, number);
		}
		return data;
	}
}