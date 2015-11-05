package memory;

import memory.ObjectDefinition;

import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class Persistor {
	public OrientGraph graph = null;
	
	/**
	 * 
	 */
	public Persistor() {
		this.graph = new OrientGraph("remote:localhost/Thoth", "deepthought", "oicu812");
	}
	
	/***
	 * 
	 * @param obj
	 * @return
	 */
	public Vertex addVertex(ObjectDefinition obj){
		if (graph.getVertexType(obj.getType()) == null){
            graph.createVertexType(obj.getType());
            System.out.println("Created objectDefinition vertex type");
        }

		return this.graph.addVertex("class:"+obj.getType());
	}
	
	/**
	 * 
	 * @param clazz
	 * @return
	 */
	public Vertex addVertex(String clazz){
		if (graph.getVertexType(clazz) == null){
            graph.createVertexType(clazz);
        }

		return this.graph.addVertex("class:"+clazz);
	}
	
	/**
	 * 
	 * @throws OConcurrentModificationException
	 */
	public synchronized void save() throws OConcurrentModificationException{
		this.graph.commit();
	}
	
	/**
	 * 
	 * @param v1
	 * @param v2
	 * @param clazz
	 * @param label
	 * @return
	 */
	public synchronized Edge addEdge(Vertex v1, Vertex v2, String clazz, String label){
		return graph.addEdge(clazz, v1, v2, label);
	}
	
	
	/**
	 * Finds a given object Definition in graph
	 * 
	 * @param obj
	 * @return
	 */
	public synchronized Iterable<Vertex> find(ObjectDefinition obj) {
		Persistor persistor = new Persistor();
		//System.err.println("Retrieving object of type = ( " + obj.getType() + " ) from orientdb with value :: " + obj.getValue());
		Iterable<Vertex> objVertices = persistor.graph.getVertices("value", obj.getValue());
		
		return objVertices;
	}
}
