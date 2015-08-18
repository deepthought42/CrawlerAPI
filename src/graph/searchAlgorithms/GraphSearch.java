package graph.searchAlgorithms;

import graph.Graph;
import graph.Vertex;

import java.util.HashMap;

import structs.Path;


/**
 * 
 * 
 * @author Brandon Kindred
 *
 */
public abstract class GraphSearch {
	Graph graph = null;
	Path path = null;
	HashMap<Integer, Integer> frontier = null;
	HashMap<Integer, Integer> visited = null;
	
	/**
	 * 
	 * @param graph graph to be crawled
	 * @param path current path
	 */
	public GraphSearch(Graph graph){
		this.graph = graph;
		this.path = new Path();
		this.frontier = new HashMap<Integer, Integer>();
		this.visited = new HashMap<Integer, Integer>();
	}
	
	/**
	 * 
	 * @return
	 */
	public Integer putNodeOnFrontier(int index, int weight){
		return frontier.put(index, weight);
		
	}
	
	/**
	 * 
	 * @return
	 */
	public Integer putNodeInVisited(int index, int weight){
		return visited.put(index, weight);
		
	}
	
	/**
	 * 
	 * @return
	 */
	public int removeNodeFromFrontier(int index){
		return frontier.remove(index);
		
	}
	
	/**
	 * Looks for index in visited list.
	 * 
	 * @return true if present, otherwise false
	 */
	public boolean isNodeInVisited(int index){
		for(Integer idx : this.visited.keySet()){
			if(idx == index){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * retrieves next best node in frontier for expansion. Details for how best
	 *  node is chosen is to be determined by extending classes
	 * 
	 * @return index of graph vertex in graph that is next best node on frontier
	 */
	public abstract int getIndexOfNextBestVertexFromFrontier();
	
	/**
	 * Returns the best {@link Path} to goal vertex index 
	 *
	 * @param start_idx
	 * @param goal_idx
	 * @return
	 */
	public abstract Path findBestPath(int start_idx, int goal_idx);
	
	/**
	 * Searches graph starting at the current vertex and working backward in a breadth first
	 * fashion to find the closes root vertex
	 * 
	 * @param startVertex {@link Vertex} to start with
	 * @return
	 */
	public abstract Path findPathToClosestRoot(int start_idx);

	public abstract Path findPathToClosestRoot(Vertex<?> startVertex);
}
