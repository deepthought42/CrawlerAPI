package com.minion.persistence;

/**
 * 
 * @author brand
 *
 * @param <V>
 */
public interface IPersistable<V> {
	/**
	 * @return string of hashCodes identifying unique fingerprint of object by the contents of the object
	 */
	String generateKey();

	/**
	 * 
	 * @param framedGraph
	 */
	V convertToRecord(OrientConnectionFactory connection);
	
	//PathObject<?> convertFromRecord(V obj);
	
	/**
	 * 
	 * @return
	 */
	V create();
	
	/**
	 * Updates the given object by finding existing instances in the databases, making
	 * the appropriate updates, then saving the data to the database
	 * 
	 * @param existing_obj
	 * @return
	 */
	V update();
}
