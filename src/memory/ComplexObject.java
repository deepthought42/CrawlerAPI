package memory;

import java.util.HashMap;

/**
 * A complex object is a composition of {@link ObjectDefinition}s
 * This class can technically handle any object type. For now
 * this object is assumed to only hold @link ObjectDefinition}s and
 * {@link ComplexObject}
 * 
 * @author Brandon Kindred
 *
 */
public class ComplexObject {

	HashMap<Object, Double> objects;
	public ComplexObject(HashMap<Object, Double> objects) {
		this.objects = objects;
	}

}
