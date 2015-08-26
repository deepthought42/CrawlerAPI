package graph;

import browsing.ElementAction;
import browsing.Page;
import browsing.PageElement;
import browsing.PageState;

/**
 * A Vertex node that contains the data for a given vertex in a {@link Graph}
 * 
 * @author Brandon Kindred
 *
 * @param <T>
 */
public class Vertex<T> {
	private boolean isRoot = false;
	private int cost = 0;
	private T data = null;
	
	public Vertex(T obj){
		this.data = obj;
	}
	
	public T getData(){
		return this.data;
	}
	
	public void setRoot(boolean isRoot){
		this.isRoot = isRoot;
	}
	
	public boolean isRoot(){
		return this.isRoot;
	}
	
	/**
	 * Returns the cost of visiting a vertex based
	 *  on a constant define per object type
	 * 
	 * @return 
	 */
	public int getCost(){
		if(data instanceof Page){
			return 0;
		}
		else if(data instanceof PageElement){
			return 1;
		}
		else if(data instanceof PageState){
			return 10;
		}
		else if(data instanceof ElementAction){
			return 5;
		}
		return 1000;
	}
	
	/**
	 * 
	 * @param vertex
	 * @return
	 */
	public boolean equals(Vertex<?> vertex){
		boolean isEqual = false;
		if(vertex.getData().getClass().equals(this.getData().getClass())){
			isEqual = vertex.getData().equals(this.getData());
		}
		return isEqual;		
	}
}
