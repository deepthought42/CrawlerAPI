package memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.tinkerpop.blueprints.Vertex;

/**
 * Defines objects that are available to the system for learning against
 * 
 * @author Brandon Kindred
 *
 */
public class ObjectDefinition {

	private String value;
	private String type;
	private int uid;
	private HashMap<String, Double> actions = new HashMap<String, Double>();
	
	/**
	 * 
	 * 
	 * @param uid
	 * @param value
	 * @param type
	 * @param actions
	 */
	public ObjectDefinition(int uid, String value, String type, HashMap<String, Double> actions) {
		this.value = value;
		this.type = type;
		this.uid = uid;
		this.actions = actions;
	}
	
	/**
	 * 
	 * 
	 * @param uid
	 * @param value
	 * @param type
	 */
	public ObjectDefinition(int uid, String value, String type) {
		this.value = value;
		this.type = type;
		this.uid = uid;
	}

	/**
	 * 
	 * 
	 * @param value
	 * @param type
	 */
	public ObjectDefinition(String value, String type) {
		this.value = value;
		this.type = type;
	}
	
	public ObjectDefinition(){}


	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getValue(){
		return this.value;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString(){
		return this.value;

	}

	/**
	 * Sets the this object's {@link HashMap} of actions to a predefined set.
	 * @param actionMap
	 */
	public void setActions(HashMap<String, Double> actionMap) {
		this.actions = actionMap;		
	}
	
	/**
	 * Gets list of probabilities associated with actions for this object definition
	 * @return
	 */
	public HashMap<String, Double> getActions(){
		return this.actions;
	}
	
	
	/**
	 * Finds {@link ObjectDefinition} and updates its probability if it exists, otherwise creates a new vertex.
	 * @param persistor
	 * @return
	 */
	public synchronized Vertex findAndUpdateOrCreate(Persistor persistor){
		//find objDef in memory. If it exists then use value for memory, otherwise choose random value
		Iterable<com.tinkerpop.blueprints.Vertex> memory_vertex_iter = persistor.find(this);
		Iterator<com.tinkerpop.blueprints.Vertex> memory_iterator = memory_vertex_iter.iterator();
		
		com.tinkerpop.blueprints.Vertex v = null;
		if(memory_iterator.hasNext()){
			//System.err.println("Finding and updating OBJECT DEFINITION with probability :: "+this.getProbability());
			v = memory_iterator.next();
			if(this.getActions().size() != 0){
				System.err.println("......Actions : "+this.getActions().size());
				v.setProperty("actions", this.getActions());
			}
		}
		else{
			System.err.println("Creating new vertex in OrientDB...");
			v = persistor.addVertex(this);
			v.setProperty("value", this.getValue());
			v.setProperty("type", this.getType());
			v.setProperty("identifier", uid);
			v.setProperty("actions", this.getActions());
		}

		persistor.save();
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
	public static synchronized List<Vertex> findAll(List<ObjectDefinition> object_definitions, Persistor persistor){
		List<Vertex> vertices = new ArrayList<Vertex>();
		for(ObjectDefinition objDef : object_definitions){
			//find objDef in memory. If it exists then use value for memory, otherwise choose random value
			Iterable<com.tinkerpop.blueprints.Vertex> memory_vertex_iter = persistor.find(objDef);
			Iterator<com.tinkerpop.blueprints.Vertex> memory_iterator = memory_vertex_iter.iterator();
			
			if(memory_iterator != null && memory_iterator.hasNext()){
				vertices.add(memory_iterator.next());
			}
		}
		return vertices;
		
	}
}
