package memory;

/**
 * Describes the object describing the edge between states, which consists of
 * an action, a direction based on from and to and a set of object definitions that
 * the value was performed against for the state transition to occur
 * 
 * @author Brandon Kindred
 */
public class SequenceVertex {
	public final String value;
	public final String class_name;
	public final int hash_code;
	public final String img_src;
	
	/**
	 * Initializes a new sequence vertex
	 * 
	 * @param action
	 * @param object
	 * @param fromState
	 * @param toState
	 */
	public SequenceVertex(int hash_code, String value, String class_name, String img_src) {
		this.hash_code = hash_code;
		this.value = value;
		this.class_name = class_name;
		this.img_src = img_src;
	}
}
