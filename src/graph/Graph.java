package graph;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines a Graph utilizing vertices and a hash map where every key is the 
 * 	index for a from vertex and every value in the corresponding ArrayList
 * 	is the to vertex
 * 
 * @author Brandon Kindred
 *
 */
public class Graph {
	private ArrayList<Vertex<?>> vertices = null;
	//In edges, the key is the index in vertices for the from vertex 
	//	and the value for the to is stored in the ArrayList
	private ConcurrentHashMap<Integer, ArrayList<Integer>> edges = new ConcurrentHashMap<Integer, ArrayList<Integer>>();
	
	public Graph(){
		vertices = new ArrayList<Vertex<?>>();
	}
	
	public Graph(Vertex<?> vertex){
		vertex.setRoot(true);
		vertices = new ArrayList<Vertex<?>>();
		vertices.add(vertex);
	}
	
	/**
	 * 
	 * @param vertex
	 * @return
	 */
	public synchronized boolean addVertex(Vertex<?> vertex){
		return this.vertices.add(vertex);
	}
	
	public ArrayList<Vertex<?>> getVertices(){
		return this.vertices;
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
			addEdge(idx1, idx2);
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
	
	public ConcurrentHashMap<Integer, ArrayList<Integer>> getEdges(){
		return this.edges;
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
	 * Gets all indices for vertices that could have come from in {@link Graph)
	 * 
	 * @param to index of node 
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
	
	/**
	 * Finds the given vertex in the graph
	 * @param vertex
	 * @return >-1 if exists else -1
	 */
	public int findVertexIndex(Vertex<?> vertex){
		int i = 0;
		for(Vertex<?> curr_vertex : this.vertices){
			if(curr_vertex.equals(vertex)){
				System.out.println("Vertex classes match...");
				System.out.println("CURRENT VERTEX CLASS = " + curr_vertex.getData().getClass());
				System.out.println("PASSED VERTEX CLASS = " + vertex.getData().getClass());
				return i;
			}
			i++;
		}
		return -1;
	}
}
