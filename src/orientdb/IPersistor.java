package orientdb;

import java.util.HashMap;

/**
 * 
 * @author Brandon Kindred
 *
 */
public interface IPersistor {
	public HashMap<String, ?> attributes = null;
	
	/**
	 * gets Attributes for an Object
	 * 
	 * @return 
	 */
	public HashMap<String, ?> getAttributes();
}
