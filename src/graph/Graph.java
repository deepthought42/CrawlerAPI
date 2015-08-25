package graph;

import java.util.ArrayList;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Defines a Graph utilizing vertices and a hash map where every key is the 
 * 	index for a from vertex and every value in the corresponding ArrayList
 * 	is the to vertex
 * 
 * @author Brandon Kindred
 *
 */
public class Graph extends Observable{
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
	 * 
	 * @param vertex
	 * @return
	 */
	public synchronized boolean addVertex(Vertex<?> vertex){
		boolean wasAdded = false;
		if(findVertexIndex(vertex) == -1){
			setChanged();
			wasAdded = this.vertices.add(vertex);
			if(wasAdded){
				notifyObservers();
			}
		}

		return wasAdded;
	}
	
	public synchronized ArrayList<Vertex<?>> getVertices(){
		return this.vertices;
	}
	
	/**
	 * Adds an edge by finding both vertices indices and creating a from-to edge
	 * @param vertex1
	 * @param vertex2
	 * 
	 * @return true if successfully created, false otherwise
	 */
	public synchronized void addEdge(Vertex<?> fromVertex, Vertex<?> toVertex){
		setChanged();
		
		int from_idx = -1;
		int to_idx = -1;
		int curr_idx = 0;
		for(Vertex<?> v : vertices){
			if(v.equals(fromVertex)){
				from_idx = curr_idx;
			}
			else if(v.equals(toVertex)){
				to_idx = curr_idx;
			}
			curr_idx++;
		}
		
		if(from_idx > -1 && to_idx > -1){
			addEdge(from_idx, to_idx);
		}
		
		notifyObservers();
	}
	
	/**
	 * Adds both indices to edges
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	public void addEdge(int from, int to){
		setChanged();
		ArrayList<Integer> toIndices =  edges.get(from);
		if(toIndices == null){
			toIndices = new ArrayList<Integer>();
		}
		toIndices.add(to);
		edges.put(from, toIndices);
		notifyObservers();
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
	public synchronized ArrayList<Integer> getFromIndices(int to){
		ArrayList<Integer> fromIndices = new ArrayList<Integer>();
		for(Integer key : edges.keySet()){
			//System.out.print(Thread.currentThread().getName() + " -> Key set FOR " + key + " :: ");
			for(Integer idx : edges.get(key)){
				//System.out.print(idx + ", ");
				if(idx == to){
					fromIndices.add(key);
				}
			}
		}
		//System.out.println(Thread.currentThread().getName() + " -> Total from indices for index "+to+" = "+fromIndices.size());
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
	public synchronized int findVertexIndex(Vertex<?> vertex){
		int i = 0;
		for(Vertex<?> curr_vertex : this.vertices){
			if(curr_vertex.equals(vertex)){
				return i;
			}
			i++;
		}
		return -1;
	}
}
