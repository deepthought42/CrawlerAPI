package com.qanairy.persistence;

public class DataAccessObject<V> {

	
	/**
	 * Use a key generated and guaranteed to be unique to retrieve all objects which have
	 * a "key" property value equal to the given generated key
	 * 
	 * @param generated_key
	 * @return
	 */
	public static Iterable<?> findByKey(String generated_key, Class<?> clazz) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		return orient_connection.getTransaction().getVertices("key", generated_key, clazz);
	}

	
	/**
	 * Use a key generated and guaranteed to be unique to retrieve all objects which have
	 * a "key" property value equal to the given generated key for a given class
	 * 
	 * @param generated_key
	 * @return
	 */
	public static Iterable<?> findByKey(String generated_key, OrientConnectionFactory orient_connection, Class<?> clazz) {
		return orient_connection.getTransaction().getVertices("key", generated_key, clazz);
	}
}
