package graph.searchAlgorithms;

import java.util.ArrayList;

import structs.Path;
import graph.Graph;

public abstract class GraphCrawler {
	Graph graph = null;
	Path path = null;
	ArrayList<Integer> frontier = null;
	ArrayList<Integer> visited = null;
	
	/**
	 * 
	 * @param graph graph to be crawled
	 * @param path current path
	 */
	public GraphCrawler(Graph graph, Path path){
		this.graph = graph;
		this.path = path;
		this.frontier = new ArrayList<Integer>();
		this.visited = new ArrayList<Integer>();
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean putNodeOnFrontier(int index){
		return frontier.add(index);
		
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean putNodeInVisited(int index){
		return visited.add(index);
		
	}
	
	/**
	 * 
	 * @return
	 */
	public int removeNodeFromFrontier(int index){
		return frontier.remove(index);
		
	}
	
	/**
	 * 
	 * @return
	 */
	public abstract int findNodeInVisited();
	
	
	public abstract int getNextBestNode();
	public abstract void run();
}
