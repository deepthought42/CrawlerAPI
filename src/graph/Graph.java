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
	 * Adds an edge by finding both vertices indices and creating a from-to edge
	 * @param vertex1
	 * @param vertex2
	 * 
	 * @return true if successfully created, false otherwise
	 */
	public boolean addEdge(Vertex<?> fromVertex, Vertex<?> toVertex){
		int idx1 = -1;
		int idx2 = -1;
		int curr_idx = 0;
		for(Vertex<?> v : vertices){
			if(v.equals(fromVertex)){
				idx1 = curr_idx;
			}
			else if(v.equals(toVertex)){
				idx2 = curr_idx;
			}
		}
		
		if(idx1 > -1 && idx2 > -1){
			int[] edge = {idx1, idx2};
			return edges.add(edge);
		}
		return false;
	}
	
	/**
	 * Adds an edge using integer indices for from and to vertices
	 * 
	 * @param from
	 * @param to
	 * 
	 * @pre edges.size() > from
	 * @pre edges.size() > to
	 * 
	 * @return true if edge added successfully, false otherwise
	 */
	public boolean addEdge(int from, int to){
		assert edges.size() > from; 
		assert edges.size() > to;
		
		int[] edge = {from, to};
		return edges.add(edge);
	}
}
