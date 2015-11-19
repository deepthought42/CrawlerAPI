package memory;

import com.tinkerpop.blueprints.Vertex;

/**
 * Describes the object describing the edge between states, which consists of
 * an action, a direction based on from and to and a set of object definitions that
 * the value was performed against for the state transition to occur
 * 
 * @author Brandon Kindred
 */
public class MemorySequence extends Persistor{
	public Vertex[] nodes = null;
	
	/**
	 * 
	 * 
	 * @param action
	 * @param object
	 * @param fromState
	 * @param toState
	 */
	public MemorySequence(Vertex[] nodes) {
		super();
		this.nodes = nodes;
	}
	
	/**
	 * 
	 * 
	 * @return
	 */
	public Vertex[] getNodes() {
		return nodes;
	}
	
	public void save(){
		
	}
	
	
}
