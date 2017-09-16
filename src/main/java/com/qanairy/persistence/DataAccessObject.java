package com.qanairy.persistence;

import org.mortbay.log.Log;

import com.tinkerpop.blueprints.Vertex;

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
		Iterable<?> iter = orient_connection.getTransaction().getVertices("key", generated_key, clazz);
		return iter;
	}

	
	/**
	 * Use a key generated and guaranteed to be unique to retrieve all objects which have
	 * a "key" property value equal to the given generated key for a given class
	 * 
	 * @param generated_key
	 * @return
	 */
	public static Iterable<?> findByKey(String generated_key, OrientConnectionFactory orient_connection, Class<?> clazz) {    	
		return orient_connection.current_tx.getVertices("key", generated_key, clazz);
	}
	
	public static Iterable<?> findAll(OrientConnectionFactory conn, Class<?> clazz){
		Iterable<?> vertices = null;
		try{
			vertices = conn.getTransaction().getVertices("@class", clazz.getSimpleName());//getVerticesOfClass(clazz.getSimpleName());
		}catch(IllegalArgumentException e){
			Log.warn(e.getMessage());
		}
		
		return vertices;
	}
}
