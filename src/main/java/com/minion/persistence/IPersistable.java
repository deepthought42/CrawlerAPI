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
	
	IPersistable<V> create();
	
	/**
	 * Updates the given object by finding existing instances in the databases, making
	 * the appropriate updates, then saving the data to the database
	 * 
	 * @param existing_obj
	 * @return
	 */
	IPersistable<V> update(V existing_obj);
	
	/**
	 * Use a key generated and guaranteed to be unique to retrieve all objects which have
	 * a "key" property value equal to the given generated key
	 * 
	 * @param generated_key
	 * @return
	 */
	Iterable<V> findByKey(String generated_key);
}
