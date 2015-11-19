package memory;

import java.util.Iterator;
import java.util.List;

import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;


/**
 * A state consists of an identifier and an image formatted to base64
 * 
 * @author Brandon Kindred
 */
public class MemoryState {
	/**
	 * Identifier is meant to identify the state
	 */
	public int identifier = 0;
	
	/**
	 * 
	 * @param objects
	 */
	public MemoryState(int identifier) {
		this.setIdentifier(identifier);
	}

	/**
	 * 
	 * @return
	 */
	public int getIdentifier() {
		return this.identifier;
	}

	/**
	 * 
	 * @param identifier
	 */
	public void setIdentifier(int identifier) {
		this.identifier = identifier;
	}
	
	/**
	 * 
	 * 
	 * @param memState
	 * @return
	 */
	public static synchronized Iterable<Vertex> findState(int identifier, Persistor persistor){
		Iterable<Vertex> objVertices = persistor.graph.getVertices("identifier", identifier);
		return objVertices;
	}

	/**
	 * Gets all edges for a given memory state
	 * @param memState
	 * @return
	 */
	public static synchronized Iterable<Edge> getStateEdges(Vertex state, Persistor persistor){
		assert state != null;
				
		Iterable<Edge> edgeList = null;

		edgeList = state.getEdges(Direction.OUT, "GOES_TO");
		for (Edge e : edgeList) {
			System.out.println("- Bought: " + e);
		}
		
		return edgeList;
	}
	
	/**
	 * If a state for the given page exists then it is loaded, otherwise a new state is created and returned
	 * 
	 * @param page
	 * @return
	 * @throws NullPointerException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public synchronized Vertex createAndLoadState(Object obj, Vertex last_state_vertex, Persistor persistor) throws IllegalArgumentException, IllegalAccessException, NullPointerException{
		
		DataDecomposer dataDef = new DataDecomposer(obj);
		List<Object> objList = dataDef.decomposeObject();
		int state_id = 0;
		
		for(Object decomposedObject : objList){
			Vertex v = null;
			ObjectDefinition objDef = null;
			if(decomposedObject.getClass().equals(ObjectDefinition.class)){
				objDef = (ObjectDefinition)decomposedObject;
				v = objDef.findAndUpdateOrCreate(persistor);
				
				if(last_state_vertex != null){
					//NEED TO CHECK IF EDGE ALREADY EXISTS
					persistor.addEdge(last_state_vertex, v, (objDef).getType(), "CONSISTS_OF");
				}
				boolean isSaved = false;
				while(!isSaved){
					try{
						persistor.save();
						isSaved = true;
					}
					catch(OConcurrentModificationException e1){
						System.err.println("Concurrent Modification Exception thrown. ITERATION : 2");
						//e1.printStackTrace();
					}
				}
			}
			else{
				//calculate hash of object and use it as an identifier
				//System.out.println("OBJECT CLASS :: "+decomposedObject.getClass().getCanonicalName());

				Vertex state_vertex = null;
				if(last_state_vertex != null){
					state_id = last_state_vertex.getProperty("identifier");
				}
				else{
					last_state_vertex = createOrLoadState(obj, persistor);

				}
				state_vertex = createOrLoadState(decomposedObject, persistor);
				
				if(state_id != decomposedObject.hashCode()){
					persistor.addEdge(last_state_vertex, state_vertex, decomposedObject.getClass().getCanonicalName().replace(".", "").replace("[","").replace("]",""), "CONSISTS_OF");
				}
				
				try{
					createAndLoadState(decomposedObject, state_vertex, persistor);
				}catch(IllegalArgumentException e){
					System.err.println("EXCEPTION OCCURED WHILE CREATING AND LOADING STATE");
				}
			}
		}
		return last_state_vertex;
	}
	
	/**
	 * Creates a state in the database
	 * @param page
	 * @return
	 */
	public static synchronized Vertex createState(Object obj, Persistor persistor){
		Vertex state_vertex = persistor.addVertex(obj.getClass().getCanonicalName().replace(".", "").replace("[","").replace("]",""));
		state_vertex.setProperty("identifier", obj.hashCode());
		//state_vertex.setProperty("screenshot", page.screenshot);
		try{
			persistor.save();
		}
		catch(OConcurrentModificationException e){
			System.err.println("Concurrent Modification EXCEPTION Error thrown");
			//e.printStackTrace()
		}
		return state_vertex;
	}
	
	/**
	 * Creates a state in the database, or loads a matching existing state from memory
	 * @param page
	 * @return
	 */
	public static synchronized Vertex createOrLoadState(Object obj, Persistor persistor){
		Iterator<com.tinkerpop.blueprints.Vertex> state_iter = MemoryState.findState(obj.hashCode(), persistor).iterator();
		Vertex state_vertex = null;
		
		if(!state_iter.hasNext()){
			state_vertex = MemoryState.createState(obj, persistor);			
		}
		else{
			state_vertex = state_iter.next();
		}
		
		return state_vertex;
	}
}
