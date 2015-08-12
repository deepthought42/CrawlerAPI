package graph;

import java.util.ArrayList;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class Graph {
	ArrayList<Vertex> vertices = new ArrayList<Vertex>();
	ArrayList<int[]> edges = new ArrayList<int[]>();
	
	public Graph(Vertex<?> vertex){
		vertex.setRoot(true);
		vertices.add(vertex);
	}
	
	/**
	 * 
	 * @param vertex
	 * @return
	 */
	public boolean addVertex(Vertex vertex){
		return this.vertices.add(vertex);
	}
	
	/**
	 * 
	 * @param vertex1
	 * @param vertex2
	 * @return
	 */
	public boolean addEdge(Vertex vertex1, Vertex vertex2){
		int idx1 = -1;
		int idx2 = -1;
		int curr_idx = 0;
		for(Vertex v : vertices){
			if(v.equals(vertex1)){
				idx1 = curr_idx;
			}
			else if(v.equals(vertex2)){
				idx2 = curr_idx;
			}
		}
		
		if(idx1 > -1 && idx2 > -1){
			int[] edge = {idx1, idx2};
			return edges.add(edge);
		}
		return false;
	}
}
