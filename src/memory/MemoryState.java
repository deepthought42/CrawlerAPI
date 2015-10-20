package memory;

import java.util.List;
import memory.ObjectDefinition;

/**
 * A state consists of a list of {@linkplain ObjectDefinition object definitions}
 * that define the state in it's entirety as disparate information from memory
 * 
 * @author Brandon Kindred
 */
public class MemoryState {
	public List<ObjectDefinition> elements = null;
	
	/**
	 * 
	 * @param objects
	 */
	public MemoryState(List<ObjectDefinition> objects) {
		this.setElements(objects);
	}

	public List<ObjectDefinition> getElements() {
		return this.elements;
	}

	public void setElements(List<ObjectDefinition> objects) {
		this.elements = objects;
	}
}
