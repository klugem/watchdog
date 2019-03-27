package de.lmu.ifi.bio.watchdog.processblocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import de.lmu.ifi.bio.watchdog.helper.ReplaceSpecialConstructs;
import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;
import de.lmu.ifi.bio.watchdog.xmlParser.XMLParser;

/**
 * Can process a block of numbers
 * @author Michael Kluge
 *
 */
public class ProcessSequence extends ProcessBlock {

	private static final long serialVersionUID = 2628902907361215662L;
	private final LinkedList<String> VALUES = new LinkedList<>();
	private static final LinkedHashMap<String, String> OFFER_VAR = new LinkedHashMap<>();
	
	// only used as data store for GUI
	public double gui_start;
	public double gui_end;
	public double gui_step;
	
	static {
		OFFER_VAR.put("[]", "numeric sequence element");
	}
	
	/**
	 * constructor for GUI storage only!
	 * @param name
	 * @param start
	 * @param end
	 * @param step
	 * @param append
	 */
	public ProcessSequence(String name, double start, double end, double step, boolean append) {
		super(name, null, null);
		this.gui_start = start;
		this.gui_end = end;
		this.gui_step = step;
		this.gui_append = append;
	}
	
	/**
	 * Constructor
	 * @param start
	 * @param end
	 * @param step
	 */
	public ProcessSequence(String name, double start, double end, double step) {
		super(name, null, null);
		this.addValues(start, end, step);
	}
	
	/**
	 * Constructor
	 * @param start
	 * @param end
	 * @param step
	 */
	public ProcessSequence(String name, ArrayList<Double> start, ArrayList<Double> end, ArrayList<Double> step) {
		super(name, null, null);
		if(!(start.size() == end.size() && start.size() == step.size() && start.size() > 0)) {
			throw new IllegalArgumentException("Size of the three ArrayLists must be equal!");
		}
		// add all the values
		for(int i = 0; i < start.size(); i++) {
			this.addValues(start.get(i), end.get(i), step.get(i));
		}
	}
	
	/**
	 * adds a range to the already existing onces
	 * @param start
	 * @param end
	 * @param step
	 */
	public void append(double start, double end, double step) {
		this.addValues(start, end, step);
	}
	
	/**
	 * adds the values of that range
	 * @param start
	 * @param end
	 * @param step
	 */
	private void addValues(double start, double end, double step) {
		// add the values
		for(double i = start; i <= end; i = i + step) {
			// let an integer be an real integer
			if(i == ((double) (int) i))
				this.VALUES.add(Integer.toString((int) i));
			else
				this.VALUES.add(ReplaceSpecialConstructs.DF.format(i));			
		}
	}
	
	@Override
	public LinkedHashMap<String, String> getValues() {
		LinkedHashMap<String, String> ret = new LinkedHashMap<>();
		for(String v : this.VALUES) {
			ret.put(v, v);
		}
		return ret;
	}
	
	@Override
	public int size() {
		return this.VALUES.size();
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof ProcessSequence))
			return false;
		
		ProcessSequence s = (ProcessSequence) o;
		return s.gui_append == this.gui_append && s.gui_start == this.gui_start && s.gui_end == this.gui_end && s.gui_step == this.gui_step ? true : false;
		
	}

	@Override
	public String toXML() {
		XMLBuilder x = new XMLBuilder();
		// start with basic tag
		x.startTag(XMLParser.PROCESS_SEQUENCE, false);
		x.addQuotedAttribute(XMLParser.NAME, this.getName());
		x.addQuotedAttribute(XMLParser.START, this.gui_start);
		x.addQuotedAttribute(XMLParser.END, this.gui_end);
		
		// add optional attributes
		if(this.gui_append)
			x.addQuotedAttribute(XMLParser.APPEND, this.gui_append);
		if(this.gui_step != 1)
			x.addQuotedAttribute(XMLParser.STEP, this.gui_step);
		if(this.hasColor())
			x.addQuotedAttribute(XMLParser.COLOR, this.getColor());
		
		// end the tag
		x.endCurrentTag();
		return x.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public LinkedHashMap<String, String> getOfferedVariables(ArrayList<String> replace) {
		return (LinkedHashMap<String, String>) OFFER_VAR.clone();
	}
	
	@Override
	public boolean isAppendAble() { 
		return true;
	}

	@Override
	public boolean mightContainFilenames() {
		return false;
	}
	
	@Override
	public boolean addsReturnInfoToTasks() {
		return false;
	}
	
	@Override
	public Object[] getDataToLoadOnGUI() { 
		return new Object[] { this.gui_start, this.gui_end, this.gui_step };
	}
}
