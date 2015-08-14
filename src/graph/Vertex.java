package graph;

import java.util.ArrayList;

/**
 * A Vertex node that contains the data for a given vertex in a {@link Graph}
 * 
 * @author Brandon Kindred
 *
 * @param <T>
 */
public class Vertex<T> {
	private boolean isRoot = false;
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
	 * 
	 * @param vertex
	 * @return
	 */
	public boolean equals(Vertex<?> vertex){
		if(vertex.getData().getClass().equals(this.getData().getClass())){
			return ((T)vertex.getData()).equals(this.getData());
		}
		return false;		
	}
}
