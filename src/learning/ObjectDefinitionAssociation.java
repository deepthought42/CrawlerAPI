package learning;

/**
 * Associates 2 {@link ObjectDefinition}s with each other and represents their
 * probabilistic association with each other
 * 
 * @author Brandon Kindred
 *
 */
public class ObjectDefinitionAssociation {
	
	public ObjectDefinitionAssociation() {

	}
	
	public long getObjectDefintion_id() {
		return ObjectDefintion_id;
	}
	public void setObjectDefintion_id(long objectDefintion_id) {
		ObjectDefintion_id = objectDefintion_id;
	}

	public long getObjectDefinition2_id() {
		return ObjectDefinition2_id;
	}

	public void setObjectDefinition2_id(long objectDefinition2_id) {
		ObjectDefinition2_id = objectDefinition2_id;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	private long ObjectDefintion_id;
	private long ObjectDefinition2_id;
	private double weight;
	private long count;

}
