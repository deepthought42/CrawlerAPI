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
	//In edgeHash, the key is the index in vertices for the from vertex 
	//	and the value for the to is stored in the ArrayList
	private HashMap<Integer, ArrayList<Integer>> edgeHash = new HashMap<Integer, ArrayList<Integer>>();
	private ArrayList<Edge> edges = new ArrayList<Edge>();
	
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
			return edges.add(new Edge(idx1, idx2));
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
		
		return edges.add(new Edge(from, to));
	}
	
	/**
	 * Adds both indices to edgeHash
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	public void addEdgeToHash(int from, int to){
		ArrayList<Integer> toIndices =  edgeHash.get(from);
		toIndices.add(to);
		edgeHash.put(from, toIndices);
	}
	
	/**
	 * 
	 * @param from
	 * @return
	 */
	public ArrayList<Integer> getToIndices(int from){
		return edgeHash.get(from);
	}
	
	/**
	 * 
	 * @param to
	 * @return
	 */
	public ArrayList<Integer> getFromIndices(int to){
		ArrayList<Integer> fromIndices = new ArrayList<Integer>();
		for(Integer key : edgeHash.keySet()){
			for(Integer idx : edgeHash.get(key)){
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
		int fromIdx = -1;
		
		do{
			for(Edge edge: edges){
	
				if(edge.to == vertexIdx){
					fromIdx = edge.to;
				}
			}
		}while(fromIdx != -1 && !vertices.get(fromIdx).isRoot());
			
		
		return vertices.get(fromIdx);
	}
}
