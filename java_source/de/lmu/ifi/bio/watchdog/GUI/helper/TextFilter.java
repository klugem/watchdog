package de.lmu.ifi.bio.watchdog.GUI.helper;

import java.util.function.UnaryOperator;

import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;

public class TextFilter implements UnaryOperator<Change> {
	
	private final String PATTERN;
	
	public static TextFormatter<String> getIntFormater() { return new TextFormatter<>(new TextFilter("-?[0-9]*")); }
	public static TextFormatter<String> getPositiveIntFormater() { return new TextFormatter<>(new TextFilter("[0-9]+")); }
	public static TextFormatter<String> getDoubleFormater() { return new TextFormatter<>(new TextFilter("-?[0-9]*(\\.[0-9]*)?")); }
	public static TextFormatter<String> getMemoryFormater() { return new TextFormatter<>(new TextFilter("[0-9]+[MG]{0,1}")); }
	public static TextFormatter<String> getAlpha() { return new TextFormatter<>(new TextFilter("[A-Za-z_]+")); }
	public static TextFormatter<String> getTaskNameFormater() { return new TextFormatter<>(new TextFilter("[a-zA-z0-9a-zA-Z_\\- ]+")); }
	public static TextFormatter<String> getAlphaFollowedByAlphaNumber() { return new TextFormatter<>(new TextFilter("[A-Za-z_][A-Za-z_0-9]*")); }
	
	private TextFilter(String pattern) {
		this.PATTERN = pattern;
	}

	@Override
	public Change apply(Change change) {
	    String t = change.getControlNewText();
	    if (t.length() == 0 || t.matches(this.PATTERN)) {
	        return change;
	    }
	    return null;
	}
}
