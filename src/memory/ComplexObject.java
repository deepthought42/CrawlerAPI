package memory;

import java.util.HashMap;
import java.util.TreeMap;

/**
 * A complex object is a composition of {@link ObjectDefinition}s
 * 
 * @author Brandon Kindred
 *
 */
public class ComplexObject {

	private TreeMap<ObjectDefinition, Double> simpleObjects;
	
	public ComplexObject(TreeMap<ObjectDefinition, Double> simpleObjects) {
		this.setSimpleObjects(simpleObjects);
	}

	public TreeMap<ObjectDefinition, Double> getSimpleObjects() {
		return simpleObjects;
	}

	public void setSimpleObjects(TreeMap<ObjectDefinition, Double> simpleObjects) {
		this.simpleObjects = simpleObjects;
	}
}
