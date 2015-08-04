package structs;

import java.util.Iterator;
import java.util.LinkedList;

import structs.ConcurrentNode;

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
	
	public boolean equals(Path path){
		int thisPathLength = this.path.size();
		int comparatorPathLength = path.getPath().size();
				
		if(thisPathLength != comparatorPathLength){
			System.out.println("PATHS ARE NOT EQUAL");
			return false;
		}
		for(int i = 0; i < thisPathLength; i++){
			ConcurrentNode<?> thisPathNode = this.path.get(i);
			ConcurrentNode<?> comparatorPathNode = (ConcurrentNode<?>)path.getPath().get(i);
			
			if(!thisPathNode.getClass().getCanonicalName().equals(comparatorPathNode.getClass().getCanonicalName())){
				System.out.println("NODE CLASS NAMES ARE NOT EQUAL");
				return false;
			}
			if(!thisPathNode.getData().equals(comparatorPathNode.getData())){
				System.out.println("NODE DATA NOT EQUAL.");
				return false;
			}
		}
		
		System.out.println("NODE PATHS ARE EQUAL");
		return true;
		
	}
	
	/**
	 * 
	 * @param path
	 * @return
	 */
	public static Path clone(Path path){
		Path clonePath = new Path();
		
		Iterator<?> pathIterator = path.getPath().iterator();
		while(pathIterator.hasNext()){
			clonePath.add((ConcurrentNode<?>) pathIterator.next());
		}
		
		return clonePath;
	}
}
