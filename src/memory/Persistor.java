package memory;
import java.util.Iterator;
import java.util.List;

import memory.ObjectDefinition;
import browsing.Page;

import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.tinkerpop.blueprints.Direction;
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
	 * @return 
	 * @throws OConcurrentModificationException
	 */
	public synchronized Vertex save() throws OConcurrentModificationException{
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
	 * 
	 * 
	 * @param obj
	 * @return
	 */
	public synchronized Iterable<Vertex> find(ObjectDefinition obj) {
		//System.err.println("Retrieving object of type = ( " + obj.getType() + " ) from orientdb with value :: " + obj.getValue());
		Iterable<Vertex> objVertices = graph.getVertices("value", obj.getValue());
		
		return objVertices;
	}
	
	/**
	 * 
	 * 
	 * @param memState
	 * @return
	 */
	public synchronized Iterable<Vertex> findState(MemoryState memState){
		Iterable<Vertex> objVertices = graph.getVertices("identifier", memState.getIdentifier());
		return objVertices;
	}

	/**
	 * Gets all edges for a given memory state
	 * @param memState
	 * @return
	 */
	public synchronized Iterable<Edge> getStateEdges(MemoryState memState){
		Iterable<Edge> edgeList = null;
		Iterator<Vertex> states = findState(memState).iterator();
		
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
	 * Associates all elements in list of {@link ObjectDefinition}s to a state_vertex via an edge
	 *  with a label of CONSISTS_OF
	 * @param vertices
	 * @param state_vertex
	 */
	public synchronized void saveState(List<ObjectDefinition> vertices, Vertex state_vertex ){
		for(ObjectDefinition obj : vertices){
			//find objDef in memory. If it exists then use value for memory, otherwise choose random value
			Iterator<Vertex> memory_iterator = this.find(obj).iterator();
			MemoryState memState = new MemoryState((Integer)state_vertex.getProperty("identifier"));
			state_vertex = this.findState(memState).iterator().next();
			Vertex v = null;
			if(memory_iterator.hasNext()){
				//while(memory_iterator.hasNext()){
					//System.err.println(this.getName() + " -> Getting memory vertex");
					v = memory_iterator.next();
					boolean saveFailed=false;
					int iterCount = 0;
					do{
						if(state_vertex!=null){
							Edge e = this.addEdge(state_vertex, v, obj.getType(), "CONSISTS_OF");
							try{
								this.save();
								saveFailed=false;
							}
							catch(OConcurrentModificationException e1){
								System.err.println("Concurrent Modification Exception thrown. ITERATION : "+iterCount);
								
								saveFailed=true;
								//e1.printStackTrace();
							}
							
							memState = new MemoryState((Integer)state_vertex.getProperty("identifier"));
							state_vertex = this.findState(memState).iterator().next();
						}
						iterCount++;
					}while(saveFailed && iterCount < 10);
				//}
			}
			else{
				//If vertex is not in memory yet, then create new vertex and save it.
				v = this.addVertex(obj);
				v.setProperty("value", obj.getValue());
				v.setProperty("type", obj.getType());
				
				if(state_vertex!=null){
					
					this.addEdge(state_vertex, v, obj.getType(), "CONSISTS_OF");
					try{
						this.save();
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
	 * If a state for the given page exists then it is loaded, otherwise a new state is created and returned
	 * @param page
	 * @return
	 * @throws NullPointerException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public synchronized Vertex createAndLoadState(Page page) throws IllegalArgumentException, IllegalAccessException, NullPointerException{
		Vertex state_vertex = null;
		MemoryState memState = new MemoryState(page.hashCode());
		System.out.println("FINDING STATE WITH IDENTIFIER :: "+memState.getIdentifier());
		Iterator<com.tinkerpop.blueprints.Vertex> state_iter = this.findState(memState).iterator();
		if(!state_iter.hasNext()){
			state_vertex = createState(page);
			DataDefinition dataDef = new DataDefinition(page);
			List<ObjectDefinition> objDefList = dataDef.decompose();
			saveState(objDefList, state_vertex);
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
		Vertex state_vertex = this.addVertex(Page.class.getCanonicalName().replace(".", "").replace("[","").replace("]",""));
		state_vertex.setProperty("identifier", page.hashCode());
		//state_vertex.setProperty("screenshot", page.screenshot);
		try{
			System.out.println("CREATING STATE...");
			this.save();
		}
		catch(OConcurrentModificationException e){
			System.err.println("Concurrent Modification EXCEPTION Error thrown");
			//e.printStackTrace()
		}
		return state_vertex;
	}
}
