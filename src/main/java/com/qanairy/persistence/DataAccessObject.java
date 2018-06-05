package com.qanairy.persistence;

import java.util.Iterator;

import org.mortbay.log.Log;
import com.orientechnologies.common.io.OIOException;

public class DataAccessObject<V> {

	
	/**
	 * Use a key generated and guaranteed to be unique to retrieve all objects which have
	 * a "key" property value equal to the given generated key
	 * 
	 * @param generated_key
	 * @return
	 */
	public static Iterator<?> findByKey(String generated_key, Class<?> clazz) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		Iterator<?> iter = orient_connection.getTransaction().getFramedVertices(clazz.getSimpleName()+".key", generated_key, clazz);
		return iter;
	}

	
	/**
	 * Use a key generated and guaranteed to be unique to retrieve all objects which have
	 * a "key" property value equal to the given generated key for a given class
	 * 
	 * @param generated_key
	 * @return
	 */
	public static Iterator<?> findByKey(String generated_key, OrientConnectionFactory orient_connection, Class<?> clazz) throws OIOException {    	
		return orient_connection.current_tx.getFramedVertices(clazz.getSimpleName()+".key", generated_key, clazz);
	}
	
	public static Iterator<?> findAll(OrientConnectionFactory conn, Class<?> clazz){
		Iterator<?> vertices = null;
		try{
			vertices = conn.getTransaction().getFramedVertices(clazz);
		}catch(IllegalArgumentException e){
			Log.warn(e.getMessage());
		}
		
		return vertices;
	}
}
