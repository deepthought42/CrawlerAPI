package memory;

import memory.ObjectDefinition;

/**
 * Describes the object describing the edge between states, which consists of
 * an action, a direction based on from and to and a set of object definitions that
 * the value was performed against for the state transition to occur
 * 
 * @author Brandon Kindred
 */
public class MemoryStateEdge {
	public String action = null;
	public ObjectDefinition object = null;

	public MemoryState fromState = null;
	public MemoryState toState = null;
	
	/**
	 * 
	 * 
	 * @param action
	 * @param object
	 * @param fromState
	 * @param toState
	 */
	public MemoryStateEdge(String action, ObjectDefinition object, MemoryState fromState, MemoryState toState) {
		this.fromState = fromState;
		this.toState = toState;
		this.action = action;
		this.object = object;
	}
	
	/**
	 * 
	 * 
	 * @return
	 */
	public String getAction() {
		return action;
	}

	/**
	 * 
	 * 
	 * @param action
	 */
	public void setAction(String action) {
		this.action = action;
	}
}
