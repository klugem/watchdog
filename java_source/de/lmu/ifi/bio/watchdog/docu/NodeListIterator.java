package de.lmu.ifi.bio.watchdog.docu;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Element iterator for Nodelists
 * @author kluge
 *
 */
public class NodeListIterator implements Iterator<Element> {

	protected final NodeList L;
	private int i = 0;
	private Element next = null;
	
	public NodeListIterator(NodeList l) {
		this.L = l;
	}

	@Override
	public boolean hasNext() {
		if(this.next != null) 
			return true;
		else {
			while(this.L.getLength() > this.i) {
				Node n = this.L.item(this.i++);
				if(n instanceof Element) {
					this.next = (Element) n;
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public Element next() {
		Element e = this.next;
		this.next = null;
		return e;
	}
	
	/**
	 * get it as stream
	 * @return
	 */
	public Stream<Element> stream() {
		Iterable<Element> iterable = () -> this;
		return StreamSupport.stream(iterable.spliterator(), false);
	}
}
