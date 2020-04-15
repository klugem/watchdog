package de.lmu.ifi.bio.watchdog.helper.graphFolderPath;

import java.util.ArrayList;

public class GraphFolderpath {
	private final GraphNode ROOT = new GraphNode(null, null);
	private final String SPLIT = "[\\/]";
	
	/**
	 * adds a complete path to the graph
	 * @param path
	 */
	public void addPath(String path) {
		String[] p = path.split(SPLIT);
		GraphNode parent = this.ROOT;
		for(String name : p) {
			parent = parent.addChild(name);
		}
	}
	
	/***
	 * 
	 * @param minDepthPath minimal number of path seps in the returned path
	 * @return
	 */
	public ArrayList<String> getLCPs(int minDepthPath) {
		int minNameParts = minDepthPath - 1;
		// get all nodes that full fill the min depth criteria
		ArrayList<GraphNode> minD = new ArrayList<>();
		this.findMinDepth(this.ROOT, minNameParts, minD);
		minD = this.makeMoreSpecific(minD);
		
		// reconstruct the path
		ArrayList<String> results = new ArrayList<>();
		for(GraphNode n : minD) {
			results.add(n.getPath());
		}
		return results;
	}
	
	/**
	 * makes the path more specific if possible
	 * @param minD
	 */
	private ArrayList<GraphNode> makeMoreSpecific(ArrayList<GraphNode> minD) {
		ArrayList<GraphNode> ret = new ArrayList<>(minD.size());
		for(GraphNode n : minD) {
			while(n.getChildNumber() == 1) {
				n = n.getChilds().get(0);
			}
			ret.add(n);
		}
		return ret;
	}

	/**
	 * Finds all nodes on a certain level
	 * @param cur
	 * @param minNameParts
	 * @param ret
	 */
	private void findMinDepth(GraphNode cur, int minNameParts, ArrayList<GraphNode> ret) {
		if(cur.getLevel() <= minNameParts) {
			for(GraphNode c : cur.getChilds()) {
				this.findMinDepth(c, minNameParts, ret);
			}
		}
		else {
			ret.add(cur);
		}
	}
}
