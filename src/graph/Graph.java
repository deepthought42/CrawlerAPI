package graph;

import java.util.ArrayList;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class Graph {
	ArrayList<Vertex<?>> vertices = new ArrayList<Vertex<?>>();
	ArrayList<Edge> edges = new ArrayList<Edge>();
	
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
