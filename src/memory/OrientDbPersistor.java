package memory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import memory.ObjectDefinition;

import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

/**
 * Persists data of various sorts into orientDB
 * 
 * @author Brandon Kindred
 *
 */
public class OrientDbPersistor<T>{
	public OrientGraph graph = null;
	
	/**
	 * Creates a new connection to the orientDB graph
	 */
	public OrientDbPersistor() {
		this.graph = new OrientGraph("remote:localhost/Thoth", "root", "oicu812");
	}
	
	
	/**
	 * Creates a new connection to the orientDB graph
	 */
	public OrientDbPersistor(String url, String username, String password) {
		this.graph = new OrientGraph(url, username, password);
	}
	
	/**
	 * Creates vertex class using the canonical name of the provided object
	 * 
	 * @param obj
	 * @return
	 */
	public Vertex addVertexType(T obj, String[] properties){
		if (graph.getVertexType(obj.getClass().getSimpleName().toString()) == null){
            OClass vt = graph.createVertexType(obj.getClass().getSimpleName().toString());
            vt.createIndex(obj.getClass().getSimpleName(), OClass.INDEX_TYPE.UNIQUE, properties);
            System.out.println("Created objectDefinition vertex type");
        }
		return this.graph.addVertex("class:"+obj.getClass().getSimpleName().toString());
	}
	
	/**
	 * Creates vertex class using the canonical name of the provided object
	 * 
	 * @param clazz the class name to be used for creating a vertex type
	 * 
	 * @return
	 */
	public Vertex addVertexType(String clazz){
		if (graph.getVertexType(clazz) == null){
            graph.createVertexType(clazz);
        }

		return this.graph.addVertex("class:"+clazz);
	}
	
	/**
	 * 
	 * 
	 * @throws OConcurrentModificationException
	 */
	public synchronized void save(){
		try{
			this.graph.commit();
		}
		catch(OConcurrentModificationException e){
			graph.rollback();
			//System.err.println("Concurrent Modification EXCEPTION Error thrown");
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates a directional edge from one {@link Vertex} to another {@link Vertex}
	 * 
	 * @param v1 the vertex that edge is from
	 * @param v2 the vertex that the edge points to
	 * @param clazz the class type of the edge
	 * @param label the label to be assigned to the edge
	 * 
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
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public synchronized Iterable<Vertex> findVertices(T obj){
		Field[] fieldArray = obj.getClass().getFields();
		//System.err.println("Retrieving object of type = ( " + obj.getType() + " ) from orientdb with value :: " + obj.getValue());
		
		Object fieldValue = 0;
		for(Field field : fieldArray){
			if( field.getName().equals("hash_code") ){
				try {
					fieldValue = field.get(obj);
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		Iterable<Vertex> objVertices = this.graph.getVertices("hash_code", fieldValue.toString());
		return objVertices;
		//Iterable<Vertex> objVertices = this.graph.getVertices(obj.getClass().getCanonicalName().toString(), names, values);
		//return objVertices;
	}
	
	/**
	 * Finds a given object Definition in graph
	 * 
	 * @param obj
	 * @return
	 */
	public synchronized Iterable<Vertex> findVertices(String fieldName, String value) {
		Iterable<Vertex> vertices = this.graph.getVertices(fieldName, value);
		
		return vertices;
	}
	
	/**
	 * Finds a given object Definition in graph
	 * 
	 * @param obj
	 * @return
	 */
	public synchronized Iterable<Edge> findEdges(String fieldName, String value) {
		Iterable<Edge> edges = this.graph.getEdges(fieldName, value);
		
		return edges;
	}
	
	/**
	 * Finds and updates the properties or creates a new vertex using the public properties of the Object passed
	 * 
	 * @param persistor
	 * 
	 * @return 
	 */
	public synchronized Vertex findAndUpdateOrCreate(T obj, String[] actions){
		Iterator<com.tinkerpop.blueprints.Vertex> memory_iterator = null;
		try{
			Iterable<com.tinkerpop.blueprints.Vertex> memory_vertex_iter = this.findVertices(obj);
			memory_iterator = memory_vertex_iter.iterator();
		}
		catch(NullPointerException e){
			e.printStackTrace();
		}
		
		com.tinkerpop.blueprints.Vertex v = null;
		if(memory_iterator != null && memory_iterator.hasNext()){
			//find objDef in memory. If it exists then use value for memory, otherwise choose random value

			//System.err.println("Finding and updating OBJECT DEFINITION with probability :: "+this.getProbability());
			v = memory_iterator.next();
			if(actions.length != 0){
				System.err.println("......Actions : "+actions.length);
				v.setProperty("actions", actions);
			}
		}
		else{
			System.out.println("Creating new vertex in OrientDB...");
			v = this.addVertexType(obj, this.getProperties(obj));
			//find objDef in memory. If it exists then use value for memory, otherwise choose random value
			for(Field field : obj.getClass().getFields()){
				String prop_val = "";
				try {
					prop_val =  field.get(obj).toString();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}	
				catch(NullPointerException e){
					e.printStackTrace();
				}
				v.setProperty(field.getName(), prop_val);
			}
		}

		this.save();
		return v;
	}
	
	/**
	 * Retrieves all vertices for given {@link ObjectDefinitions}
	 * 
	 * @param objectDefinitions
	 * 
	 * @pre persistor != null
	 * @pre object_definitions != null
	 * 
	 * @return A list of all vertices found. 
	 */
	public synchronized List<Vertex> findAll(List<T> objects){
		List<Vertex> vertices = new ArrayList<Vertex>();
		for(T objDef : objects){
			//find objDef in memory. If it exists then use value for memory, otherwise choose random value
			Iterable<com.tinkerpop.blueprints.Vertex> memory_vertex_iter = this.findVertices(objDef);
			Iterator<com.tinkerpop.blueprints.Vertex> memory_iterator = memory_vertex_iter.iterator();
			
			if(memory_iterator != null && memory_iterator.hasNext()){
				vertices.add(memory_iterator.next());
			}
		}
		return vertices;
		
	}
	
	/**
	 * Gets all edges for a given memory state
	 * 
	 * @param memState
	 * 
	 * @return
	 */
	public static synchronized Iterable<Edge> getStateEdges(Vertex state){
		assert state != null;
				
		Iterable<Edge> edgeList = null;

		edgeList = state.getEdges(Direction.OUT, "GOES_TO");
		for (Edge e : edgeList) {
			System.out.println("- Bought: " + e);
		}
		
		return edgeList;
	}

	//Retrieves all public properties for an object
	private String[] getProperties(T obj){
		String[] properties = new String[obj.getClass().getFields().length];
		int idx = 0;
		for(Field field : obj.getClass().getFields()){
			properties[idx] = field.getName();
			idx++;
		}
		return properties;
	}
}
