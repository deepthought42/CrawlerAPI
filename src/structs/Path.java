package structs;

import graph.Graph;

import java.util.ArrayList;
import java.util.Iterator;

import com.tinkerpop.blueprints.Edge;

import browsing.Page;
import browsing.PageElement;
import memory.MemoryState;
import memory.Persistor;

/**
 * A set of vertex objects that form a sequential movement through a graph
 * 
 * @author Brandon Kindred
 *
 */
public class Path {
	public Integer reward = 0;
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
	
	public Integer getReward(){
		return this.reward;
	}
	
	/**
	 * Calculates the cost of traversing the path based on the cost of individual vertices within this path
	 * 
	 * @param graph
	 * @return
	 */
	public int calculateCost(Graph graph){
		this.cost=0;
		for(Integer vertex_idx : this.getPath()){
			this.cost += graph.getVertices().get(vertex_idx).getCost();
		}
		return this.cost;
	}
	
	/**
	 * Gets the estimated reward value for this path 
	 * @param graph
	 * @return
	 */
	public int calculateReward(Graph graph){
		this.reward = 0;
		for(Integer vertex_idx : this.getPath()){
			this.reward += graph.getVertices().get(vertex_idx).getReward();
		}
		
		return reward;
	}
	
	/**
	 * Gets the actual reward value for this path 
	 * @param graph
	 * @return
	 */
	public int getActualReward(Graph graph){
		int reward = 0;
		com.tinkerpop.blueprints.Vertex current_state = null;
		MemoryState mem_state = null;
		Iterable<Edge> edgeList = null;
		Persistor persistor = new Persistor();
		System.out.println("CALCULATING ACTUAL REWARD");

		for(Integer vertex_idx : this.getPath()){
			Object vertex_object = graph.getVertices().get(vertex_idx).getData();
			if(vertex_object instanceof Page){
				Iterator<com.tinkerpop.blueprints.Vertex> matching_states = 
						MemoryState.findState(vertex_object.hashCode(), persistor).iterator();
				if(matching_states.hasNext()){
					current_state = matching_states.next();
					edgeList = MemoryState.getStateEdges(current_state, persistor);
					reward += 1;
				}
			}
			else if(vertex_object instanceof PageElement){
				//get all GOES_TO edges originating from current state that match the given pageElement xpath value
				
				
				//get reward of matching vertex;
				
				//persistor.findState(memState)
				//reward = reward * pageElementReward();
			}
			
			this.reward += graph.getVertices().get(vertex_idx).getReward();
		}
		
		return reward;
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
