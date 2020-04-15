package de.lmu.ifi.bio.watchdog.helper.graphFolderPath;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class GraphNode {
	private final HashMap<String, GraphNode> CHILDS = new HashMap<>();
	private final GraphNode PARENT;
	private final String NAME;
	private final int LEVEL;
	private int size = 0;
	
	public GraphNode(GraphNode p, String name) {
		this.PARENT = p;
		this.NAME = name;
		this.LEVEL = (p == null ? 0 : p.getLevel() + 1);
	}
	
	public int getLevel() {
		return this.LEVEL;
	}
	
	public int getSize() {
		return this.size;
	}
	
	public int getChildNumber() {
		return this.CHILDS.size();
	}
	
	public GraphNode addChild(String name) {
		this.size++;
		if(!this.CHILDS.containsKey(name)) {
			GraphNode c = new GraphNode(this, name);
			this.CHILDS.put(name, c);
			return c;
		}
		else
			return this.CHILDS.get(name);		
	}
	
	public GraphNode getParent() {
		return this.PARENT;
	}
	
	public ArrayList<GraphNode> getChilds() {
		ArrayList<GraphNode> ret = new ArrayList<>(this.getChildNumber());
		ret.addAll(this.CHILDS.values());
		return ret;
	}
	
	public String getName() {
		return this.NAME;
	}

	public String getPath() {
		StringBuilder buf = new StringBuilder();
		GraphNode par = this;
		while(par != null && par.getParent() != null) {
			buf.insert(0, File.separator);
			buf.insert(0, par.getName());
			par = par.getParent();
		}
		return buf.toString();
	}
}
