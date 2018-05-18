package com.qanairy.persistence;

/**
 * Interface for persistable objects which allows objects to generate a key before saving 
 *
 * @param <V>
 */
public interface IPersistable<V> {
	/**
	 * @return string of hashCodes identifying unique fingerprint of object by the contents of the object
	 */
	String generateKey(V obj);
}
