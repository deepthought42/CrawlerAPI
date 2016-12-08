package com.qanairy.models;

import java.util.HashMap;


/**
 * Defines objects that are available to the system for learning against
 * 
 * @author Brandon Kindred
 *
 */
public class ObjectDefinition {

	public final String value;
	public final String type;
	public final int hash_code;
	public final HashMap<String, Double> actions;
	
	/**
	 * Instantiates a new object definition
	 * 
	 * @param uid
	 * @param value
	 * @param type
	 * @param actions
	 * 
	 * @pre actions != null
	 */
	public ObjectDefinition(String value, String type, HashMap<String, Double> actions) {
		assert actions != null;
		
		this.value = value;
		this.type = type;
		this.hash_code = value.hashCode();
		this.actions = actions;
	}
	
	/**
	 * 
	 * 
	 * @param uid
	 * @param value
	 * @param type
	 */
	public ObjectDefinition(String value, String type) {
		this.value = value;
		this.type = type;
		this.hash_code = value.hashCode();
		this.actions = new HashMap<String, Double>();
	}


	public String getType() {
		return type;
	}
	
	public String getValue(){
		return this.value;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString(){
		return this.value;
	}
	
	/**
	 * Gets list of probabilities associated with actions for this object definition
	 * @return
	 */
	public HashMap<String, Double> getActions(){
		return this.actions;
	}
}
