package com.qanairy.persistence;

import java.util.List;

/**
 * 
 * @author brand
 *
 * @param <V>
 */
public interface IPersistable<V, Z> {
	/**
	 * @return string of hashCodes identifying unique fingerprint of object by the contents of the object
	 */
	String generateKey(V obj);

	/**
	 * 
	 * @param connection
	 * @param obj
	 * @return
	 */
	Z save(OrientConnectionFactory connection, V obj);
	
	/**
	 * 
	 * @param connection
	 * @param obj
	 * @return
	 */
	V load(Z obj);

	/**
	 * 
	 * @param connection
	 * @param key
	 * @return
	 */
	V find(OrientConnectionFactory connection, String key);
	
	/**
	 * 
	 * @param connection
	 * @return
	 */
	List<V> findAll(OrientConnectionFactory connection);
}
