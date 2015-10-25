package structs;

import graph.Graph;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class Path {
	private Integer cost = null;
	private ArrayList<Integer> vertexPath = null;
	
	/**
	 * 
	 */
	public Path(){
		this.vertexPath = new ArrayList<Integer>();
	}
	
	/**
	 * 
	 */
	public Path(Integer vertex_idx){
		this.vertexPath = new ArrayList<Integer>();
	}

	/**
	 * 
	 * @param current_path
	 */
	public Path(Path current_path){
		this.vertexPath = new ArrayList<Integer>();
		this.append(current_path);
	}
	
	/**
	 * 
	 * @return
	 */
	public void append(Path appendablePath){
		Iterator<Integer> iter = appendablePath.getPath().iterator();
		while(iter.hasNext()){
			this.vertexPath.add(iter.next());
		}				
	}
		
	public boolean add(Integer node_idx){
		return this.vertexPath.add(node_idx);
	}
	/**
	 * 
	 * @return
	 */
	public ArrayList<Integer> getPath(){
		return this.vertexPath;
	}
	
	public boolean equals(Path path){
		int thisPathLength = this.vertexPath.size();
		int comparatorPathLength = path.getPath().size();
				
		if(thisPathLength != comparatorPathLength){
			System.out.println("PATHS ARE NOT EQUAL");
			return false;
		}
		for(int i = 0; i < thisPathLength; i++){
			Integer thisPathNode = this.vertexPath.get(i);
			Integer comparatorPathNode = this.vertexPath.get(i);
			
			if(!thisPathNode.getClass().getCanonicalName().equals(comparatorPathNode.getClass().getCanonicalName())){
				System.out.println("NODE CLASS NAMES ARE NOT EQUAL");
				return false;
			}
			if(thisPathNode != comparatorPathNode){
				System.out.println("NODE DATA NOT EQUAL.");
				return false;
			}
		}
		
		System.out.println("NODE PATHS ARE EQUAL");
		return true;		
	}

	public Integer getCost(){
		return this.cost;
	}
	
	public int getCost(Graph graph){
		this.cost=0;
		for(Integer vertex_idx : this.getPath()){
			this.cost += graph.getVertices().get(vertex_idx).getCost();
		}
		return this.cost;
	}
	/**
	 * 
	 * @param path
	 * @return
	 */
	public static Path clone(Path path){
		Path clonePath = new Path();
		
		Iterator<Integer> pathIterator = path.getPath().iterator();
		while(pathIterator.hasNext()){
			clonePath.add(pathIterator.next());
		}
		
		return clonePath;
	}
	
	
}
