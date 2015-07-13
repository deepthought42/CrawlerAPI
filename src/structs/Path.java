package structs;

import java.util.Iterator;
import java.util.LinkedList;

import browsing.ConcurrentNode;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class Path {
	private LinkedList<ConcurrentNode<?>> path = null;
	
	/**
	 * 
	 */
	public Path(){
		this.path = new LinkedList<ConcurrentNode<?>>();
	}
	
	/**
	 * 
	 * @param current_path
	 */
	public Path(ConcurrentNode<?> current_node){
		this.path = new LinkedList<ConcurrentNode<?>>();
		this.path.offer(current_node);
	}
	
	/**
	 * 
	 * @param current_path
	 */
	public Path(Path current_path){
		this.path = new LinkedList<ConcurrentNode<?>>();
		this.append(current_path);
	}
	
	/**
	 * 
	 * @return
	 */
	public void append(Path appendablePath){
		Iterator<?> iter = appendablePath.getPath().iterator();
		while(iter.hasNext()){
			this.path.add((ConcurrentNode<?>) iter.next());
		}				
	}
	
	
	public boolean add(ConcurrentNode<?> node){
		return this.path.offer(node);
	}
	
	/**
	 * 
	 * @return
	 */
	public LinkedList<?> getPath(){
		return this.path;
	}
}
