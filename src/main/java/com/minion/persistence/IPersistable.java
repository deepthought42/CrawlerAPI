package com.minion.persistence;

/**
 * 
 * @author brand
 *
 * @param <V>
 */
public interface IPersistable<V> {
	String generateKey();

	/**
	 * 
	 * @param framedGraph
	 */
	V convertToRecord(OrientConnectionFactory connection);
	
	//PathObject<?> convertFromRecord(V obj);
	
	IPersistable<V> create();
	
	/**
	 * Updates the given object by finding existing instances in the databases, making
	 * the appropriate updates, then saving the data to the database
	 * 
	 * @param existing_obj
	 * @return
	 */
	IPersistable<V> update();
}
