package memory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.tinkerpop.blueprints.Vertex;

/**
 * Defines objects that are available to the system for learning against
 * 
 * @author Brandon Kindred
 *
 */
public class ObjectDefinition {

	private String value;
	private int count;
	private String type;
	private double probability;
	
	public ObjectDefinition(int count, String value, String type) {
		this.count = count;
		this.value = value;
		this.type = type;
	}

	public ObjectDefinition(String name, String type) {
		this.count = 1;
		this.type = type;
	}
	
	public ObjectDefinition(){}

	public int getCount() {
		return count;
	}
	
	private void setCount(int count) {
		this.count = count;
	}

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
	
	@Override
	public String toString(){
		return this.value;

	}

	public void incrementCount() {
		this.count += 1;
	}

	public void setProbability(double i) {
		this.probability = i;		
	}
	
	public double getProbability(){
		return this.probability;
	}
	
	public synchronized Vertex findAndUpdateOrCreate(){
		Persistor persistor = new Persistor();
		//find objDef in memory. If it exists then use value for memory, otherwise choose random value
		Iterable<com.tinkerpop.blueprints.Vertex> memory_vertex_iter = persistor.find(this);
		Iterator<com.tinkerpop.blueprints.Vertex> memory_iterator = memory_vertex_iter.iterator();

		com.tinkerpop.blueprints.Vertex v = null;
		if(memory_iterator.hasNext()){
			v = memory_iterator.next();
			v.setProperty("probability", this.getProbability());
		}
		else{
			v = persistor.addVertex(this);
			v.setProperty("value", this.getValue());
			v.setProperty("type", this.getType());
		}
		
		try{
			persistor.save();
		}catch(OConcurrentModificationException e){
			System.out.println("CONCURRENT MODIFICATION WHILE reinforcing known objectDef");
		}
		
		return v;
	}
	
	/**
	 * Retrieves all vertices for given object Definitions
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
