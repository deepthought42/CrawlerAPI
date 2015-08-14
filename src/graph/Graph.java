package graph;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class Graph {
	private ArrayList<Vertex<?>> vertices = new ArrayList<Vertex<?>>();
	//In edges, the key is the index in vertices for the from vertex 
	//	and the value for the to is stored in the ArrayList
	private HashMap<Integer, ArrayList<Integer>> edges = new HashMap<Integer, ArrayList<Integer>>();
	
	public Graph(Vertex<?> vertex){
		vertex.setRoot(true);
		vertices.add(vertex);
	}
	
	/**
	 * 
	 * @param vertex
	 * @return
	 */
	public boolean addVertex(Vertex<?> vertex){
		return this.vertices.add(vertex);
	}
	
	/**
	 * Adds an edge by finding both vertices indices and creating a from-to edge
	 * @param vertex1
	 * @param vertex2
	 * 
	 * @return true if successfully created, false otherwise
	 */
	public void addEdge(Vertex<?> fromVertex, Vertex<?> toVertex){
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
			ArrayList<Integer> toList = edges.get(idx1);
			toList.add(idx2);
			edges.put(idx1, toList);
		}
	}
	
	/**
	 * Adds both indices to edges
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	public void addEdge(int from, int to){
		ArrayList<Integer> toIndices =  edges.get(from);
		toIndices.add(to);
		edges.put(from, toIndices);
	}
	
	/**
	 * 
	 * @param from
	 * @return
	 */
	public ArrayList<Integer> getToIndices(int from){
		return edges.get(from);
	}
	
	/**
	 * 
	 * @param to
	 * @return
	 */
	public ArrayList<Integer> getFromIndices(int to){
		ArrayList<Integer> fromIndices = new ArrayList<Integer>();
		for(Integer key : edges.keySet()){
			for(Integer idx : edges.get(key)){
				if(idx == to){
					fromIndices.add(idx);
				}
			}
		}
		return fromIndices;
	}
	
	/**
	 * Finds a root vertex
	 * 
	 * @param vertexIdx
	 * @return
	 */
	public Vertex<?> findRoot(int vertexIdx){
		ArrayList<Integer> fromVertices = getFromIndices(vertexIdx);
		
		return vertices.get(fromVertices.get(0));
	}
}
