package memory;

import java.util.Iterator;
import java.util.List;

import browsing.Page;

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
	Persistor persistor = null;
	
	/**
	 * 
	 * @param objects
	 */
	public MemoryState(int identifier) {
		this.setIdentifier(identifier);
		persistor = new Persistor();
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
	 * Associates all elements in list of {@link ObjectDefinition}s to a state_vertex via an edge
	 *  with a label of CONSISTS_OF
	 * @param vertices
	 * @param state_vertex
	 */
	public synchronized void saveState(List<ObjectDefinition> vertices, Vertex state_vertex ){

		for(ObjectDefinition obj : vertices){
			//find objDef in memory. If it exists then use value for memory, otherwise choose random value
			Iterator<Vertex> memory_iterator = persistor.find(obj).iterator();
			MemoryState memState = new MemoryState((Integer)state_vertex.getProperty("identifier"));
			state_vertex = this.findState().iterator().next();
			Vertex v = null;
			if(memory_iterator.hasNext()){
					//System.err.println(this.getName() + " -> Getting memory vertex");
					v = memory_iterator.next();
					boolean saveFailed=false;
					int iterCount = 0;
					do{
						if(state_vertex!=null){
							Edge e = persistor.addEdge(state_vertex, v, obj.getType(), "CONSISTS_OF");
							try{
								persistor.save();
								saveFailed=false;
							}
							catch(OConcurrentModificationException e1){
								System.err.println("Concurrent Modification Exception thrown. ITERATION : "+iterCount);
								
								saveFailed=true;
								//e1.printStackTrace();
							}
							
							memState = new MemoryState((Integer)state_vertex.getProperty("identifier"));
							state_vertex = this.findState().iterator().next();
						}
						iterCount++;
					}while(saveFailed && iterCount < 10);
			}
			else{
				//If vertex is not in memory yet, then create new vertex and save it.
				v = persistor.addVertex(obj);
				v.setProperty("value", obj.getValue());
				v.setProperty("type", obj.getType());
				
				if(state_vertex!=null){
					
					persistor.addEdge(state_vertex, v, obj.getType(), "CONSISTS_OF");
					try{
						persistor.save();
					}
					catch(OConcurrentModificationException e2){
						System.err.println("Concurrent Modification Error thrown");
						//e2.printStackTrace();
					}					
				}
			}
		}
	}
	
	/**
	 * 
	 * 
	 * @param memState
	 * @return
	 */
	public synchronized Iterable<Vertex> findState(){
		Iterable<Vertex> objVertices = persistor.graph.getVertices("identifier", this.getIdentifier());
		return objVertices;
	}

	/**
	 * Gets all edges for a given memory state
	 * @param memState
	 * @return
	 */
	public synchronized Iterable<Edge> getStateEdges(MemoryState memState){
		Iterable<Edge> edgeList = null;
		Iterator<Vertex> states = findState().iterator();
		
		if(states.hasNext()){
			Vertex vertex = states.next();
			edgeList = vertex.getEdges(Direction.OUT, "GOES_TO");
			for (Edge e : edgeList) {
				System.out.println("- Bought: " + e);
			}
		}
		return edgeList;
	}
	
	/**
	 * If a state for the given page exists then it is loaded, otherwise a new state is created and returned
	 * @param page
	 * @return
	 * @throws NullPointerException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public synchronized Vertex createAndLoadState(Page page) throws IllegalArgumentException, IllegalAccessException, NullPointerException{
		Vertex state_vertex = null;

		System.out.println("FINDING STATE WITH IDENTIFIER :: "+this.getIdentifier());
		Iterator<com.tinkerpop.blueprints.Vertex> state_iter = this.findState().iterator();
		if(!state_iter.hasNext()){
			state_vertex = this.createState(page);
			DataDecomposer dataDef = new DataDecomposer(page);
			List<ObjectDefinition> objDefList = dataDef.decompose();
			this.saveState(objDefList, state_vertex);
		}
		else{
			state_vertex = state_iter.next();
		}
		return state_vertex;
	}
	
	/**
	 * Creates a state in the database
	 * @param page
	 * @return
	 */
	public synchronized Vertex createState(Page page){
		Vertex state_vertex = persistor.addVertex(Page.class.getCanonicalName().replace(".", "").replace("[","").replace("]",""));
		state_vertex.setProperty("identifier", page.hashCode());
		//state_vertex.setProperty("screenshot", page.screenshot);
		try{
			System.out.println("CREATING STATE...");
			persistor.save();
		}
		catch(OConcurrentModificationException e){
			System.err.println("Concurrent Modification EXCEPTION Error thrown");
			//e.printStackTrace()
		}
		return state_vertex;
	}
}
