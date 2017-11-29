package de.lmu.ifi.bio.watchdog.GUI.helper;

import java.util.HashMap;

public class ParamCounts {
	private final HashMap<String, Integer> COUNTS = new HashMap<>();
	
	public void decreaseCount(String name) {
		if(this.hasCount(name)) {
			this.COUNTS.put(name, this.getCount(name)-1);
			if(this.getCount(name) <= 0)
				this.COUNTS.remove(name);
		}
	}

	public void increaseCount(String name) {
		if(!this.hasCount(name))
			this.COUNTS.put(name, 0);
		this.COUNTS.put(name, this.getCount(name)+1);
	}
	
	public void clear() {
		this.COUNTS.clear();
	}
	
	public boolean hasCount(String name) {
		return this.COUNTS.containsKey(name);
	}
	
	public int getCount(String name) {
		return this.COUNTS.get(name);
	}
	
	public void set(String name, int count) {
		this.COUNTS.put(name, count);
	}

	public void set(ParamCounts counts) {
		this.COUNTS.clear();
		this.COUNTS.putAll(counts.COUNTS);
	}
}
